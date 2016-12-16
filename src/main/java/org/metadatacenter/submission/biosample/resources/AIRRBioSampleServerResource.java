package org.metadatacenter.submission.biosample.resources;

import com.codahale.metrics.annotation.Timed;
import org.metadatacenter.submission.biosample.AIRRTemplate;
import org.metadatacenter.submission.biosample.core.BioSampleValidator;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;

@Path("/validate-airr") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON) public class AIRRBioSampleServerResource
{
  private final BioSampleValidator bioSampleValidator;

  public AIRRBioSampleServerResource()
  {
    this.bioSampleValidator = new BioSampleValidator();
  }

  @POST @Timed public Response validate(AIRRTemplate airrInstance)
  {
    try {
      return Response.ok(this.bioSampleValidator.validateAIRRInstance(airrInstance)).build();
    } catch (JAXBException e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    } catch (IOException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    } catch (DatatypeConfigurationException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }
}
