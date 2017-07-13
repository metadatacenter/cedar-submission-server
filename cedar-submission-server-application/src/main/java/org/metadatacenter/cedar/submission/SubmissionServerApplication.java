package org.metadatacenter.cedar.submission;

import io.dropwizard.setup.Environment;
import org.metadatacenter.cedar.submission.health.SubmissionServerHealthCheck;
import org.metadatacenter.cedar.submission.resources.AIRRSubmissionServerResource;
import org.metadatacenter.cedar.submission.resources.AMIA2016DemoBioSampleServerResource;
import org.metadatacenter.cedar.submission.resources.ImmPortSubmissionServerResource;
import org.metadatacenter.cedar.submission.resources.IndexResource;
import org.metadatacenter.cedar.submission.resources.LincsSubmissionServerResource;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplication;
import org.metadatacenter.model.ServerName;

public class SubmissionServerApplication extends CedarMicroserviceApplication<SubmissionServerConfiguration> {

  public static void main(String[] args) throws Exception {
    new SubmissionServerApplication().run(args);
  }

  @Override
  protected ServerName getServerName() {
    return ServerName.SUBMISSION;
  }

  @Override
  public void initializeApp() {
  }

  @Override
  public void runApp(SubmissionServerConfiguration configuration, Environment environment) {

    final IndexResource index = new IndexResource();
    environment.jersey().register(index);

    // Register resources
    final AMIA2016DemoBioSampleServerResource amia2016DemoBioSampleServerResource = new
        AMIA2016DemoBioSampleServerResource(cedarConfig);
    environment.jersey().register(amia2016DemoBioSampleServerResource);

    final AIRRSubmissionServerResource airrSubmissionServerResource = new AIRRSubmissionServerResource(cedarConfig);
    environment.jersey().register(airrSubmissionServerResource);

    final LincsSubmissionServerResource lincsSubmissionServerResource = new LincsSubmissionServerResource(cedarConfig);
    environment.jersey().register(lincsSubmissionServerResource);

    final ImmPortSubmissionServerResource immPortSubmissionServerResource = new ImmPortSubmissionServerResource(cedarConfig);
    environment.jersey().register(immPortSubmissionServerResource);

    final SubmissionServerHealthCheck healthCheck = new SubmissionServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);
  }
}
