package org.metadatacenter.cedar.submission;

import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.metadatacenter.cedar.submission.health.SubmissionServerHealthCheck;
import org.metadatacenter.cedar.submission.resources.AMIA2016DemoBioSampleServerResource;
import org.metadatacenter.cedar.submission.resources.IndexResource;
import org.metadatacenter.cedar.submission.resources.LincsSubmissionServerResource;
import org.metadatacenter.cedar.submission.resources.NcbiAirrSubmissionServerResource;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplication;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.ServerName;
import org.metadatacenter.server.cache.util.CacheService;
import org.metadatacenter.submission.ncbiairr.queue.NcbiAirrSubmissionExecutorService;
import org.metadatacenter.submission.ncbiairr.queue.NcbiAirrSubmissionQueueProcessor;
import org.metadatacenter.submission.ncbiairr.queue.NcbiAirrSubmissionQueueService;

public class SubmissionServerApplication extends CedarMicroserviceApplication<SubmissionServerConfiguration> {

  private static NcbiAirrSubmissionExecutorService ncbiAirrSubmissionExecutorService;
  private static CacheService cacheService;

  public static void main(String[] args) throws Exception {
    new SubmissionServerApplication().run(args);
  }

  @Override
  protected ServerName getServerName() {
    return ServerName.SUBMISSION;
  }

  @Override
  protected void initializeWithBootstrap(Bootstrap<SubmissionServerConfiguration> bootstrap, CedarConfig cedarConfig) {
  }

  @Override
  public void initializeApp() {
    cacheService = new CacheService(cedarConfig.getCacheConfig().getPersistent());

    NcbiAirrSubmissionQueueService ncbiAirrSubmissionQueueService =
        new NcbiAirrSubmissionQueueService(cedarConfig.getCacheConfig().getPersistent());

    NcbiAirrSubmissionServerResource.injectServices(ncbiAirrSubmissionQueueService);

    ncbiAirrSubmissionExecutorService = new NcbiAirrSubmissionExecutorService();
  }

  @Override
  public void runApp(SubmissionServerConfiguration configuration, Environment environment) {

    final IndexResource index = new IndexResource();
    environment.jersey().register(index);

    // Register resources
    final AMIA2016DemoBioSampleServerResource amia2016DemoBioSampleServerResource = new
        AMIA2016DemoBioSampleServerResource(cedarConfig);
    environment.jersey().register(amia2016DemoBioSampleServerResource);

    final NcbiAirrSubmissionServerResource airrSubmissionServerResource =
        new NcbiAirrSubmissionServerResource(cedarConfig);
    environment.jersey().register(airrSubmissionServerResource);

    final LincsSubmissionServerResource lincsSubmissionServerResource = new LincsSubmissionServerResource(cedarConfig);
    environment.jersey().register(lincsSubmissionServerResource);

    final SubmissionServerHealthCheck healthCheck = new SubmissionServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);

    // Submission processor
    NcbiAirrSubmissionQueueProcessor ncbiAirrSubmissionProcessor = new NcbiAirrSubmissionQueueProcessor(cacheService,
        cedarConfig.getCacheConfig().getPersistent(), ncbiAirrSubmissionExecutorService);
    environment.lifecycle().manage(ncbiAirrSubmissionProcessor);
  }
}
