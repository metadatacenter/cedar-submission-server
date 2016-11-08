package org.metadatacenter.submission.biosample.resources;

import com.codahale.metrics.annotation.Timed;
import org.metadatacenter.submission.biosample.CEDARBioSampleValidationResponse;
import org.metadatacenter.submission.biosample.api.Saying;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Path("/hello-world") @Produces(MediaType.APPLICATION_JSON) public class CEDARBioSampleServerResource
{
  private final String message;
  private final String defaultName;
  private final AtomicLong counter;

  public CEDARBioSampleServerResource(String message, String defaultName)
  {
    this.message = message;
    this.defaultName = defaultName;
    this.counter = new AtomicLong();
  }

  @GET @Timed public CEDARBioSampleValidationResponse sayHello(@QueryParam("name") Optional<String> name)
  {
    final String value = String.format(message, name.orElse(defaultName));
    CEDARBioSampleValidationResponse response = new CEDARBioSampleValidationResponse();
    response.setIsValid(true);
    return response;
  }
}