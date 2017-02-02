package org.metadatacenter.cedar.submission.resources;

import com.codahale.metrics.annotation.Timed;
import org.metadatacenter.submission.biosample.AMIA2016DemoBioSampleTemplate;
import org.metadatacenter.submission.BioSampleValidator;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;

@Path("/command")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AMIA2016DemoBioSampleServerResource {
  private final BioSampleValidator bioSampleValidator;

  public AMIA2016DemoBioSampleServerResource() {
    this.bioSampleValidator = new BioSampleValidator();
  }

  @POST
  @Timed
  @Path("/validate-biosample")
  public Response validate(AMIA2016DemoBioSampleTemplate amia2016BioSampleInstance) {
    try {
      return Response.ok(this.bioSampleValidator.validateAMIA2016DemoBioSampleInstance(amia2016BioSampleInstance))
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