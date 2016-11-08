package org.metadatacenter.submission.biosample;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

public class CEDARBioSampleServerConfiguration extends Configuration
{
  @NotEmpty private String message;

  @JsonProperty public String getMessage()
  {
    return message;
  }

  @JsonProperty public void setMessage(String message)
  {
    this.message = message;
  }
}
