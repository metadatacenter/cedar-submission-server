package org.metadatacenter.submission.ncbi.queue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.metadatacenter.server.queue.util.EmbeddedRedis;
import org.metadatacenter.server.queue.util.QueueService;
import org.metadatacenter.server.queue.util.QueueTestConfig;
import org.metadatacenter.submission.ncbi.NcbiSubmission;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Timeout(30)
class NcbiSubmissionQueueProcessorTest {

  private EmbeddedRedis redis;
  private NcbiSubmissionQueueService queue;
  private NcbiSubmissionQueueProcessor processor;

  @AfterEach
  void tearDown() throws Exception {
    if (processor != null) {
      processor.stop();
    } else if (queue != null) {
      queue.close();
    }
    if (redis != null) {
      redis.close();
    }
  }

  @Test
  void aSubmissionIsHandledAndAcknowledged() throws Exception {
    RecordingExecutor executor = prepare();
    queue.enqueueSubmission(submission("submission-1"));

    processor.start();
    awaitHandled(executor);
    awaitQueueSettled();

    assertEquals("submission-1", executor.lastEvent.getSubmission().getId());
    assertQueueEmpty();
  }

  @Test
  void aLegacyNullWakeUpIsDiscardedWithoutCallingTheExecutor() throws Exception {
    RecordingExecutor executor = prepare();
    try (Jedis jedis = new Jedis("127.0.0.1", redis.port())) {
      jedis.rpush(QueueTestConfig.queueName(QueueService.NCBI_SUBMISSION_QUEUE_ID), "null");
    }

    processor.start();
    awaitQueueSettled();

    assertEquals(0, executor.handledCount);
    assertQueueEmpty();
  }

  private RecordingExecutor prepare() {
    redis = EmbeddedRedis.start();
    queue = new NcbiSubmissionQueueService(QueueTestConfig.onPort(redis.port()));
    RecordingExecutor executor = new RecordingExecutor();
    processor = new NcbiSubmissionQueueProcessor(queue, executor);
    return executor;
  }

  private static NcbiSubmission submission(String id) {
    return new NcbiSubmission(id, "https://metadatacenter.org/users/user-1",
        List.of("/tmp/" + id + ".xml"), "folder-" + id, true);
  }

  private static void awaitHandled(RecordingExecutor executor) throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (executor.handledCount == 0 && System.nanoTime() < deadline) {
      Thread.sleep(10);
    }
    assertEquals(1, executor.handledCount);
  }

  private void awaitQueueSettled() throws InterruptedException {
    String pending = QueueTestConfig.queueName(QueueService.NCBI_SUBMISSION_QUEUE_ID);
    String processing = pending + "-processing";
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    try (Jedis jedis = new Jedis("127.0.0.1", redis.port())) {
      while ((jedis.llen(pending) != 0 || jedis.llen(processing) != 0) && System.nanoTime() < deadline) {
        Thread.sleep(10);
      }
    }
  }

  private void assertQueueEmpty() {
    String pending = QueueTestConfig.queueName(QueueService.NCBI_SUBMISSION_QUEUE_ID);
    try (Jedis jedis = new Jedis("127.0.0.1", redis.port())) {
      assertEquals(0, jedis.llen(pending));
      assertEquals(0, jedis.llen(pending + "-processing"));
    }
  }

  private static final class RecordingExecutor extends NcbiSubmissionExecutorService {
    private volatile int handledCount;
    private volatile NcbiSubmissionQueueEvent lastEvent;

    private RecordingExecutor() {
      super(null);
    }

    @Override
    public void handleEvent(NcbiSubmissionQueueEvent event) {
      lastEvent = event;
      handledCount++;
    }
  }
}
