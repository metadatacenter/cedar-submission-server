package org.metadatacenter.submission.biosample.health;

import com.codahale.metrics.health.HealthCheck;

public class CEDARBioSampleServerHealthCheck extends HealthCheck
{
  private final String message;

  public CEDARBioSampleServerHealthCheck(String message)
  {
    this.message = message;
  }

  @Override protected Result check() throws Exception
  {
    final String saying = String.format(message, "TEST");
    if (!saying.contains("TEST")) {
      return Result.unhealthy("message doesn't include a name");
    }
    return Result.healthy();
  }
}