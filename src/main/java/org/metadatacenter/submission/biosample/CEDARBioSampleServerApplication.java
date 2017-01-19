package org.metadatacenter.submission.biosample;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.metadatacenter.submission.biosample.health.CEDARBioSampleServerHealthCheck;
import org.metadatacenter.submission.biosample.resources.AIRRBioSampleServerResource;
import org.metadatacenter.submission.biosample.resources.AMIA2016DemoBioSampleServerResource;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;

public class CEDARBioSampleServerApplication extends Application<CEDARBioSampleServerConfiguration>
{

  public static void main(final String[] args) throws Exception
  {
    new CEDARBioSampleServerApplication().run(args);
  }

  @Override public String getName()
  {
    return "CEDARBioSampleServer";
  }

  @Override public void initialize(final Bootstrap<CEDARBioSampleServerConfiguration> bootstrap)
  {
  }

  @Override public void run(final CEDARBioSampleServerConfiguration configuration, final Environment environment)
  {
    final AMIA2016DemoBioSampleServerResource amia2016DemoBioSampleServerResource = new AMIA2016DemoBioSampleServerResource();
    environment.jersey().register(amia2016DemoBioSampleServerResource);

    final AIRRBioSampleServerResource airrBioSampleServerResource = new AIRRBioSampleServerResource();
    environment.jersey().register(airrBioSampleServerResource);

    final CEDARBioSampleServerHealthCheck healthCheck = new CEDARBioSampleServerHealthCheck(configuration.getMessage());
    environment.healthChecks().register("message", healthCheck);

    // Enable CORS headers
    final FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);

    // Configure CORS parameters
    cors.setInitParameter("allowedOrigins", "*");
    cors.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin");
    cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");

    // Add URL mapping
    cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

  }
}
