package org.metadatacenter.submission.ncbiairr.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.metadatacenter.config.CacheServerPersistent;
import org.metadatacenter.submission.ncbiairr.NcbiAirrSubmission;
import org.metadatacenter.util.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class NcbiAirrSubmissionQueueService {

  public final static String NCBI_SUBMISSION_QUEUE_ID = "ncbiAirrSubmission";
  protected static final Logger log = LoggerFactory.getLogger(NcbiAirrSubmissionQueueService.class);
  private final CacheServerPersistent cacheConfig;
  private JedisPool pool;

  public NcbiAirrSubmissionQueueService(CacheServerPersistent cacheConfig) {
    this.cacheConfig = cacheConfig;
    pool = new JedisPool(new JedisPoolConfig(), cacheConfig.getConnection().getHost(),
        cacheConfig.getConnection().getPort(), cacheConfig.getConnection().getTimeout());
  }

  public void enqueueSubmission(NcbiAirrSubmission submission) {
    NcbiAirrSubmissionQueueEvent event = new NcbiAirrSubmissionQueueEvent(submission);
    enqueueEvent(event);
  }

  private void enqueueEvent(NcbiAirrSubmissionQueueEvent event) {
    Jedis jedis = pool.getResource();
    String json = null;
    try {
      json = JsonMapper.MAPPER.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      log.error("Error while enqueueing event", e);
    }
    jedis.rpush(cacheConfig.getQueueName(NCBI_SUBMISSION_QUEUE_ID), json);
    jedis.close();
  }

  public Jedis getJedis() {
    return pool.getResource();
  }
}
