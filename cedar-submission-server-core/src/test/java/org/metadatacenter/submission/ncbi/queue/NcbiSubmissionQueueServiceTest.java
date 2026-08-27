package org.metadatacenter.submission.ncbi.queue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.metadatacenter.config.CacheServerPersistent;
import org.metadatacenter.server.queue.util.EmbeddedRedis;
import org.metadatacenter.server.queue.util.QueueService;
import org.metadatacenter.server.queue.util.QueueTestConfig;
import org.metadatacenter.submission.ncbi.NcbiSubmission;
import org.metadatacenter.util.json.JsonMapper;
import redis.clients.jedis.Jedis;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The NCBI submission queue against a real Redis.
 * <p>
 * The drop path carries extra weight here: queue failure must not turn submission acceptance into
 * an HTTP failure, and invalid null work must never become a durable poison message.
 */
@Timeout(30)
class NcbiSubmissionQueueServiceTest {

  private static EmbeddedRedis redis;
  private static CacheServerPersistent config;

  private NcbiSubmissionQueueService submissionQueue;

  @BeforeAll
  static void startRedis() {
    redis = EmbeddedRedis.start();
    config = QueueTestConfig.onPort(redis.port());
  }

  @AfterAll
  static void stopRedis() {
    redis.close();
  }

  @BeforeEach
  void setUp() {
    submissionQueue = new NcbiSubmissionQueueService(config);
    try (Jedis jedis = new Jedis("127.0.0.1", redis.port())) {
      jedis.flushAll();
    }
  }

  @AfterEach
  void tearDown() {
    submissionQueue.close();
  }

  private static NcbiSubmission submission(String id) {
    return new NcbiSubmission(id, "https://metadatacenter.org/users/user-1",
        List.of("/tmp/" + id + "-a.xml", "/tmp/" + id + "-b.xml"), "folder-" + id, true);
  }

  @Test
  void anEnqueuedSubmissionComesBackIntact() throws Exception {
    submissionQueue.enqueueSubmission(submission("submission-1"));

    submissionQueue.initializeBlockingQueue();
    List<String> popped = submissionQueue.waitForMessages();

    assertNotNull(popped);
    assertEquals(QueueTestConfig.queueName(QueueService.NCBI_SUBMISSION_QUEUE_ID), popped.get(0));

    NcbiSubmission read = JsonMapper.MAPPER.readValue(popped.get(1), NcbiSubmission.class);
    assertEquals("submission-1", read.getId());
    assertEquals("https://metadatacenter.org/users/user-1", read.getCedarUserId());
    assertEquals("folder-submission-1", read.getSubmissionFolder());
    assertEquals(2, read.getLocalFilePaths().size(), "the file list should survive the round trip");
    assertEquals(true, read.getUploadSubmitReadyFile());
  }

  @Test
  void submissionsQueueInTheOrderTheyWereEnqueued() throws Exception {
    submissionQueue.enqueueSubmission(submission("first"));
    submissionQueue.enqueueSubmission(submission("second"));

    submissionQueue.initializeBlockingQueue();
    assertEquals("first", JsonMapper.MAPPER
        .readValue(submissionQueue.waitForMessages().get(1), NcbiSubmission.class).getId());
    assertEquals("second", JsonMapper.MAPPER
        .readValue(submissionQueue.waitForMessages().get(1), NcbiSubmission.class).getId());
  }

  @Test
  void nothingIsDroppedWhileRedisIsReachable() {
    submissionQueue.enqueueSubmission(submission("delivered"));

    assertEquals(0, submissionQueue.getDroppedEventCount());
    submissionQueue.initializeBlockingQueue();
    assertEquals(1, submissionQueue.messageCount());
  }

  @Test
  void anUnreachableRedisDropsAndCountsWithoutThrowing() {
    NcbiSubmissionQueueService offline =
        new NcbiSubmissionQueueService(QueueTestConfig.onPort(EmbeddedRedis.freePort()));
    try {
      assertDoesNotThrow(() -> offline.enqueueSubmission(submission("dropped")));

      assertEquals(1, offline.getDroppedEventCount());
    } finally {
      offline.close();
    }
  }

  @Test
  void aNullSubmissionIsNotEnqueued() {
    assertDoesNotThrow(() -> submissionQueue.enqueueSubmission(null));

    submissionQueue.initializeBlockingQueue();
    assertEquals(0, submissionQueue.messageCount(), "null work must not become a durable queue message");
  }
}
