package org.metadatacenter.submission.ncbi.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.metadatacenter.config.CacheServerPersistent;
import org.metadatacenter.submission.ncbi.NcbiSubmission;
import org.metadatacenter.util.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class NcbiSubmissionQueueService {

  public final static String NCBI_SUBMISSION_QUEUE_ID = "ncbiSubmission";
  private final String ncbiSubmissionQueueName;
  protected static final Logger log = LoggerFactory.getLogger(NcbiSubmissionQueueService.class);
  private final CacheServerPersistent cacheConfig;
  private JedisPool pool;

  public NcbiSubmissionQueueService(CacheServerPersistent cacheConfig) {
    this.cacheConfig = cacheConfig;
    ncbiSubmissionQueueName = cacheConfig.getQueueName(NCBI_SUBMISSION_QUEUE_ID);
    pool = new JedisPool(new JedisPoolConfig(), cacheConfig.getConnection().getHost(),
        cacheConfig.getConnection().getPort(), cacheConfig.getConnection().getTimeout());
  }

  public void enqueueSubmission(NcbiSubmission submission) {
    NcbiSubmissionQueueEvent event = new NcbiSubmissionQueueEvent(submission);
    enqueueEvent(event);
  }

  private void enqueueEvent(NcbiSubmissionQueueEvent event) {
    Jedis jedis = pool.getResource();
    String json = null;
    try {
      json = JsonMapper.MAPPER.writeValueAsString(event);
    } catch (JsonProcessingException e) {
      log.error("Error while enqueueing event", e);
    }
    jedis.rpush(ncbiSubmissionQueueName, json);
    jedis.close();
  }

  public Jedis getJedis() {
    return pool.getResource();
  }
}
