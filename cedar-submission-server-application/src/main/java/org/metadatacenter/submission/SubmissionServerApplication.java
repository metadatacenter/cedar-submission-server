package org.metadatacenter.submission;

import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplication;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.ServerName;
import org.metadatacenter.server.cache.util.CacheService;
import org.metadatacenter.submission.health.SubmissionServerHealthCheck;
import org.metadatacenter.submission.ncbi.queue.NcbiSubmissionExecutorService;
import org.metadatacenter.submission.ncbi.queue.NcbiSubmissionQueueProcessor;
import org.metadatacenter.submission.ncbi.queue.NcbiSubmissionQueueService;
import org.metadatacenter.submission.notifications.StatusNotifier;
import org.metadatacenter.submission.resources.*;

public class SubmissionServerApplication extends CedarMicroserviceApplication<SubmissionServerConfiguration> {

  private static NcbiSubmissionExecutorService ncbiSubmissionExecutorService;
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

    NcbiSubmissionQueueService ncbiSubmissionQueueService =
        new NcbiSubmissionQueueService(cedarConfig.getCacheConfig().getPersistent());

    NcbiCairrSubmissionServerResource.injectServices(ncbiSubmissionQueueService);

    ncbiSubmissionExecutorService = new NcbiSubmissionExecutorService(cedarConfig);

    StatusNotifier.initialize(cedarConfig);
  }

  @Override
  public void runApp(SubmissionServerConfiguration configuration, Environment environment) {

    final IndexResource index = new IndexResource();
    environment.jersey().register(index);

    // Register resources
    final AMIA2016DemoBioSampleServerResource amia2016DemoBioSampleServerResource = new
        AMIA2016DemoBioSampleServerResource(cedarConfig);
    environment.jersey().register(amia2016DemoBioSampleServerResource);

    final NcbiCairrSubmissionServerResource cairrSubmissionServerResource =
        new NcbiCairrSubmissionServerResource(cedarConfig);
    environment.jersey().register(cairrSubmissionServerResource);

    final LincsSubmissionServerResource lincsSubmissionServerResource = new LincsSubmissionServerResource(cedarConfig);
    environment.jersey().register(lincsSubmissionServerResource);

    final ImmPortSubmissionServerResource immPortSubmissionServerResource = new ImmPortSubmissionServerResource
        (cedarConfig);
    environment.jersey().register(immPortSubmissionServerResource);

    final SubmissionServerHealthCheck healthCheck = new SubmissionServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);

    // NCBI submission processor
    NcbiSubmissionQueueProcessor ncbiSubmissionProcessor = new NcbiSubmissionQueueProcessor(cacheService,
        cedarConfig.getCacheConfig().getPersistent(), ncbiSubmissionExecutorService);
    environment.lifecycle().manage(ncbiSubmissionProcessor);
  }
}
