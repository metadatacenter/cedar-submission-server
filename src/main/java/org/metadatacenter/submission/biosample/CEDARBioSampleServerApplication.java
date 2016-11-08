package org.metadatacenter.submission.biosample;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.metadatacenter.submission.biosample.health.CEDARBioSampleServerHealthCheck;
import org.metadatacenter.submission.biosample.resources.CEDARBioSampleServerResource;

public class CEDARBioSampleServerApplication extends Application<CEDARBioSampleServerConfiguration>
{

  public static void main(final String[] args) throws Exception
  {
    new CEDARBioSampleServerApplication().run(args);
  }

  @Override public String getName()
  {
    return "CEDARBioSampleServer";
  }

  @Override public void initialize(final Bootstrap<CEDARBioSampleServerConfiguration> bootstrap)
  {
  }

  @Override public void run(final CEDARBioSampleServerConfiguration configuration, final Environment environment)
  {
    final CEDARBioSampleServerResource resource = new CEDARBioSampleServerResource();
    environment.jersey().register(resource);

    final CEDARBioSampleServerHealthCheck healthCheck = new CEDARBioSampleServerHealthCheck(configuration.getMessage());
    environment.healthChecks().register("message", healthCheck);
  }
}
