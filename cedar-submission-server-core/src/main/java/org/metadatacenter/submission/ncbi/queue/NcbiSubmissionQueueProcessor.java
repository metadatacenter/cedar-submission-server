package org.metadatacenter.submission.ncbi.queue;

import io.dropwizard.lifecycle.Managed;
import org.metadatacenter.server.queue.util.RepeatedFailureLogger;
import org.metadatacenter.submission.ncbi.NcbiSubmission;
import org.metadatacenter.util.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NcbiSubmissionQueueProcessor implements Managed {

  private static final Logger log = LoggerFactory.getLogger(NcbiSubmissionQueueProcessor.class);

  private static final int RETRY_DELAY_SECONDS = 10;

  private final NcbiSubmissionQueueService ncbiSubmissionQueueService;
  private final NcbiSubmissionExecutorService ncbiSubmissionExecutorService;
  private volatile boolean doProcessing;
  private ExecutorService executor;
  private final RepeatedFailureLogger consumerFailureLogger = new RepeatedFailureLogger();

  public NcbiSubmissionQueueProcessor(NcbiSubmissionQueueService ncbiSubmissionQueueService,
                                      NcbiSubmissionExecutorService ncbiSubmissionExecutorService) {
    this.ncbiSubmissionQueueService = ncbiSubmissionQueueService;
    this.ncbiSubmissionExecutorService = ncbiSubmissionExecutorService;
    doProcessing = true;
  }

  private void digestMessages() {
    log.info("NcbiSubmissionQueueProcessor.start()");
    while (doProcessing) {
      try {
        consumeMessages();
      } catch (Exception e) {
        if (doProcessing) {
          // The consumer must never die silently: log the failure and keep retrying, so a queue
          // (Redis) outage suspends processing instead of ending it. An outage lasts across many
          // retries, so only the first failure carries a stack trace
          consumerFailureLogger.report(log, "The NCBI submission queue consumer failed, probably because "
              + "the queue (Redis) became unreachable. Retrying in " + RETRY_DELAY_SECONDS + " seconds.",
              "failures", e);
          try {
            Thread.sleep(RETRY_DELAY_SECONDS * 1000L);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
          }
        }
      }
    }
    log.info("NcbiSubmissionQueueProcessor finished gracefully");
  }

  private void consumeMessages() {
    ncbiSubmissionQueueService.initializeBlockingQueue();
    List<String> submissionMessages;
    log.info("Waiting for submissions in the NCBI submission queue.");
    while (doProcessing) {
      submissionMessages = ncbiSubmissionQueueService.waitForMessages();
      if (submissionMessages == null || submissionMessages.isEmpty()) {
        continue;
      }
      log.info("Got submission message.");
      String value = submissionMessages.get(1);
      NcbiSubmission submission;
      try {
        submission = JsonMapper.MAPPER.readValue(value, NcbiSubmission.class);
      } catch (IOException e) {
        log.error("There was an error while deserializing submission", e);
        ncbiSubmissionQueueService.deadLetter(value);
        continue;
      }
      // Older shutdown logic put a JSON null sentinel on the durable queue. Consume and
      // acknowledge any such residue instead of handing a null submission to the executor.
      if (submission == null) {
        ncbiSubmissionQueueService.acknowledge(value);
        continue;
      }
      NcbiSubmissionQueueEvent event = new NcbiSubmissionQueueEvent(submission);
      try {
        log.info(" no. files: " + submission.getLocalFilePaths().size());
        log.info(" created at: " + event.getCreatedAt());
        ncbiSubmissionExecutorService.handleEvent(event);
        if (!ncbiSubmissionQueueService.acknowledge(value)) {
          throw new IllegalStateException("The handled NCBI submission could not be acknowledged");
        }
      } catch (Exception e) {
        log.error("There was an error while handling the message", e);
        ncbiSubmissionQueueService.deadLetter(value);
      }
    }
  }

  @Override
  public void start() throws Exception {
    executor = Executors.newSingleThreadExecutor();
    executor.submit(this::digestMessages);
  }

  @Override
  public void stop() throws Exception {
    log.info("NcbiSubmissionQueueProcessor.stop()");
    log.info("Set looping flag to false");
    doProcessing = false;
    ncbiSubmissionQueueService.interruptWait();
    if (executor != null) {
      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
    log.info("Close Jedis");
    ncbiSubmissionQueueService.close();
  }
}
