package org.metadatacenter.submission.resources;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.metadatacenter.config.*;
import org.metadatacenter.config.environment.*;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.submission.*;
import org.metadatacenter.submission.immport.ImmPortUtil;
import org.metadatacenter.util.http.HttpTimeouts;
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.test.TestAuthUtil;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** Real authenticated submission routes; only the external validator/workspace service is stubbed. */
class SubmissionAnswerMatrixTest {
  static HttpServer upstream;
  static final AtomicInteger answer = new AtomicInteger(200);
  static final AtomicInteger calls = new AtomicInteger();
  static final Queue<HttpExchange> held = new ConcurrentLinkedQueue<>();
  static volatile boolean tokenAvailable = true;
  static DropwizardTestSupport<SubmissionServerConfiguration> server;
  static String auth;

  public static class MatrixApplication extends SubmissionServerApplication {
    @Override public void initializeApp() { }
    @Override public void runApp(SubmissionServerConfiguration configuration, Environment environment) {
      String base = "http://127.0.0.1:" + upstream.getAddress().getPort();
      HttpTimeouts timeouts = HttpTimeouts.EXTERNAL.with(new OutboundTimeoutOverride(200,200));
      environment.jersey().register(new LincsSubmissionServerResource(cedarConfig,base+"/lincs",timeouts));
      ImmPortUtil immport = new ImmPortUtil(cedarConfig) {
        @Override public Optional<String> getImmPortBearerToken() {
          return tokenAvailable ? Optional.of("stub-token") : Optional.empty();
        }
        @Override public String getWorkspaceUrl() { return base+"/immport"; }
      };
      environment.jersey().register(new ImmPortSubmissionServerResource(cedarConfig,immport,timeouts));
    }
  }

  @BeforeAll static void start() throws Exception {
    upstream = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
    upstream.createContext("/", exchange -> {
      calls.incrementAndGet(); exchange.getRequestBody().readAllBytes();
      int status = answer.get();
      if(status == -1) { exchange.close(); return; }
      if(status == -2) { held.add(exchange); return; } // Keep the listener bound; client must time out.
      String body = status == 200 ? (exchange.getRequestURI().getPath().equals("/lincs")
          ? "{\"valid\":true}" : "{\"1\":\"Workspace\"}") : "{\"error\":\"upstream refusal\"}";
      if(status == -3) { status=200; body="<html>maintenance</html>"; }
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type","application/json");
      exchange.getResponseHeaders().set("Retry-After","17");
      exchange.sendResponseHeaders(status,bytes.length);
      exchange.getResponseBody().write(bytes); exchange.close();
    });
    upstream.start();
    Map<String,String> env = new HashMap<>(CedarEnvironmentSource.getAll());
    env.put("CEDAR_SUBMISSION_HTTP_PORT","0"); env.put("CEDAR_SUBMISSION_ADMIN_PORT","0");
    env.put("CEDAR_SUBMISSION_STOP_PORT","0"); CedarEnvironmentSource.setOverride(env);
    server = new DropwizardTestSupport<>(MatrixApplication.class,ResourceHelpers.resourceFilePath("test-config.yml"));
    server.before();
    CedarConfig config = CedarConfig.getInstance(CedarEnvironmentVariableProvider.getFor(SystemComponent.SERVER_SUBMISSION));
    TestAuthUtil.installInMemoryUserService(config);
    auth=TestAuthUtil.getTestUser1AuthHeader(config);
  }
  @AfterEach void reset() { tokenAvailable=true; calls.set(0); held.forEach(HttpExchange::close); held.clear(); }
  @AfterAll static void stop() {
    if(server != null) server.after(); if(upstream != null) upstream.stop(0);
  }
  static Stream<Arguments> answers() {
    List<Arguments> cases=new ArrayList<>();
    for(String service : List.of("lincs","immport"))
      for(int upstream : new int[]{200,400,401,403,404,409,412,422,429,500,502,503,504,-1,-2,-3}) {
        int expected;
        if(upstream == -1 || upstream == -2) expected=503;
        else if(upstream == -3) expected=502;
        else if(service.equals("lincs")) expected=Set.of(400,401,403,500).contains(upstream) ? 502 : upstream;
        else expected=upstream == 401 || upstream == 403 || upstream >= 500 ? 502 : upstream;
        cases.add(Arguments.of(service,upstream,expected));
      }
    return cases.stream();
  }
  @ParameterizedTest(name="{0}: upstream {1} -> {2}") @MethodSource("answers")
  void upstreamOutcomesHaveTheDeclaredWireContract(String service,int status,int expected) throws Exception {
    answer.set(status); calls.set(0);
    var response=request(service);
    assertTrue(calls.get()>0,"The authenticated route must actually reach the stub");
    assertEquals(expected,response.statusCode(),response.body());
    assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
    var body=JsonMapper.STRICT_MAPPER.readTree(response.body());
    if(expected>=400) {
      assertEquals(expected,body.path("statusCode").asInt(),response.body());
      assertTrue(body.has("errorKey")); assertTrue(body.path("parameters").isObject());
      assertFalse(body.has("originalException")); assertFalse(body.has("stackTrace"));
      if(status>=400) {
        assertEquals(status,body.path("parameters").path("upstreamStatusCode").asInt());
        assertEquals("17",response.headers().firstValue("Retry-After").orElseThrow());
      }
    } else assertFalse(body.has("errorKey"));
  }
  @Test void unavailableImmPortCredentialsAreNotACallerAuthenticationFailure() throws Exception {
    tokenAvailable=false; calls.set(0);
    var response=request("immport");
    assertEquals(503,response.statusCode()); assertEquals(0,calls.get());
    assertEquals(503,JsonMapper.STRICT_MAPPER.readTree(response.body()).path("statusCode").asInt());
  }
  private static HttpResponse<String> request(String service) throws Exception {
    String route=service.equals("lincs") ? "/command/validate-lincs" : "/command/immport-workspaces";
    var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+server.getLocalPort()+route))
        .timeout(java.time.Duration.ofSeconds(5)).header("Authorization",auth);
    if(service.equals("lincs")) request.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{}"));
    else request.header("Content-Type","multipart/form-data").GET();
    return HttpClient.newHttpClient().send(request.build(),HttpResponse.BodyHandlers.ofString());
  }
}
