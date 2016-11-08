package org.metadatacenter.submission.biosample.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.metadatacenter.submission.biosample.AMIA2016DemoBioSampleTemplate;
import org.metadatacenter.submission.biosample.CEDARBioSampleValidationResponse;
import org.metadatacenter.submission.biosample.core.BioSampleValidator;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
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

  @POST @Timed public CEDARBioSampleValidationResponse validate(
    AMIA2016DemoBioSampleTemplate amiaBioSampleSubmissionInstance)
    throws JAXBException, IOException, DatatypeConfigurationException
  {
    //    AMIA2016DemoBioSampleTemplate amiaBioSampleSubmissionInstance = mapper
    //      .readValue(body, AMIA2016DemoBioSampleTemplate.class);

    return this.bioSampleValidator.validateAMIABioSampleSubmission(amiaBioSampleSubmissionInstance);
  }
}