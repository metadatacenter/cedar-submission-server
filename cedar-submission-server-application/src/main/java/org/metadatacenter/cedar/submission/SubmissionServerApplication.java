package org.metadatacenter.cedar.submission;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.metadatacenter.bridge.CedarDataServices;
import org.metadatacenter.cedar.submission.health.SubmissionServerHealthCheck;
import org.metadatacenter.cedar.submission.resources.AIRRSubmissionServerResource;
import org.metadatacenter.cedar.submission.resources.AMIA2016DemoBioSampleServerResource;
import org.metadatacenter.cedar.submission.resources.IndexResource;
import org.metadatacenter.cedar.util.dw.CedarDropwizardApplicationUtil;
import org.metadatacenter.config.CedarConfig;

public class SubmissionServerApplication extends Application<SubmissionServerConfiguration> {

  protected static CedarConfig cedarConfig;

  public static void main(String[] args) throws Exception {
    new SubmissionServerApplication().run(args);
  }

  @Override
  public String getName() {
    return "submission-server";
  }

  @Override
  public void initialize(Bootstrap<SubmissionServerConfiguration> bootstrap) {
    cedarConfig = CedarConfig.getInstance();
    CedarDataServices.getInstance(cedarConfig);

    // TODO Temp CedarDropwizardApplicationUtil.setupKeycloak();
  }

  @Override
  public void run(SubmissionServerConfiguration configuration, Environment environment) {

    final IndexResource index = new IndexResource();
    environment.jersey().register(index);

    // Register resources
    final AMIA2016DemoBioSampleServerResource amia2016DemoBioSampleServerResource = new AMIA2016DemoBioSampleServerResource();
    environment.jersey().register(amia2016DemoBioSampleServerResource);

    final AIRRSubmissionServerResource airrSubmissionServerResource = new AIRRSubmissionServerResource(cedarConfig);
    environment.jersey().register(airrSubmissionServerResource);

    final SubmissionServerHealthCheck healthCheck = new SubmissionServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);

    CedarDropwizardApplicationUtil.setupEnvironment(environment);

  }
}
