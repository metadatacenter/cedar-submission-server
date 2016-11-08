package org.metadatacenter.submission.biosample;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

public class CEDARBioSampleServerConfiguration extends Configuration
{
  @NotEmpty private String message;

  @NotEmpty private String defaultName = "Stranger";

  @JsonProperty public String getMessage()
  {
    return message;
  }

  @JsonProperty public void setMessage(String message)
  {
    this.message = message;
  }

  @JsonProperty public String getDefaultName()
  {
    return defaultName;
  }

  @JsonProperty public void setDefaultName(String name)
  {
    this.defaultName = name;
  }
}
