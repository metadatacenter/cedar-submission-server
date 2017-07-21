package org.metadatacenter.submission.health;

import com.codahale.metrics.health.HealthCheck;

public class SubmissionServerHealthCheck extends HealthCheck {

  public SubmissionServerHealthCheck() {
  }

  @Override
  protected Result check() throws Exception {
    if (2 * 2 == 5) {
      return Result.unhealthy("Unhealthy, because 2 * 2 == 5");
    }
    return Result.healthy();
  }
}