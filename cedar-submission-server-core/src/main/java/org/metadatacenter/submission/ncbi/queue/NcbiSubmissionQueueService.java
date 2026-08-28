package org.metadatacenter.submission.ncbi.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.metadatacenter.config.CacheServerPersistent;
import org.metadatacenter.server.queue.util.QueueServiceWithBlockingQueue;
import org.metadatacenter.submission.ncbi.NcbiSubmission;
import org.metadatacenter.util.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

public class NcbiSubmissionQueueService extends QueueServiceWithBlockingQueue {

  private static final Logger log = LoggerFactory.getLogger(NcbiSubmissionQueueService.class);

  public NcbiSubmissionQueueService(CacheServerPersistent cacheConfig) {
    super(cacheConfig, NCBI_SUBMISSION_QUEUE_ID);
  }

  public void enqueueSubmission(NcbiSubmission submission) {
    if (submission == null) {
      log.warn("A null NCBI submission was not enqueued");
      return;
    }
    // Enqueueing is best-effort: serialize first, then push. A connection failure is logged and
    // the submission dropped, so an unreachable queue (Redis) can not fail the caller.
    String json;
    try {
      json = JsonMapper.MAPPER.writeValueAsString(submission);
    } catch (JsonProcessingException e) {
      log.error("The submission could not be serialized. Dropping it.", e);
      return;
    }
    try (Jedis jedis = pool.getResource()) {
      jedis.rpush(queueName, json);
    } catch (Exception e) {
      reportDroppedEvent(log, "submission", e);
    }
  }
}
