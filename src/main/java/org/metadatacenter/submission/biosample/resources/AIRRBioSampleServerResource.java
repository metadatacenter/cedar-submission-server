package org.metadatacenter.submission.biosample.resources;

import com.codahale.metrics.annotation.Timed;
import org.metadatacenter.submission.biosample.core.BioSampleValidator;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/validate-airr") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON) public class AIRRBioSampleServerResource
{
  private final BioSampleValidator bioSampleValidator;

  public AIRRBioSampleServerResource()
  {
    this.bioSampleValidator = new BioSampleValidator();
  }

  @POST @Timed public Response validate()
  {
    return Response.ok().build(); // TODO
  }
}