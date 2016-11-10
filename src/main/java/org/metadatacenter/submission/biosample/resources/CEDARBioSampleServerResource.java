package org.metadatacenter.submission.biosample.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.metadatacenter.submission.biosample.AMIA2016DemoBioSampleTemplate;
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

@Path("/validate") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON) public class CEDARBioSampleServerResource
{
  private static final ObjectMapper mapper = new ObjectMapper();

  private final BioSampleValidator bioSampleValidator;

  public CEDARBioSampleServerResource()
  {
    this.bioSampleValidator = new BioSampleValidator();
  }

  //CEDARBioSampleValidationResponseCEDARBioSampleValidationResponse

  //http://kielczewski.eu/2013/05/developing-restful-web-services-using-dropwizard-part-ii/
  @POST @Timed public Response validate(AMIA2016DemoBioSampleTemplate amiaBioSampleSubmissionInstance)
  {
    try {
      return Response.ok(this.bioSampleValidator.validateAMIABioSampleSubmission(amiaBioSampleSubmissionInstance))
        .build();
    } catch (JAXBException e) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    } catch (IOException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    } catch (DatatypeConfigurationException e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }
}