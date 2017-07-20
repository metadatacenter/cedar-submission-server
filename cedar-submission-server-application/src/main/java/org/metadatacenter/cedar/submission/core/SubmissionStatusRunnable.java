package org.metadatacenter.cedar.submission.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// executor submit new SubmissionStatusRUnnable
//executor.shutdown();
//executor.awaitTermination();

public class SubmissionStatusRunnable implements Runnable
{
  final static Logger logger = LoggerFactory.getLogger(SubmissionStatusRunnable.class);

  private static final int NUMBER_OF_THREADS = 10;

  private final ConcurrentHashMap<String, SubmissionStatusTaskDescriptor> submissions;

  public SubmissionStatusRunnable(ConcurrentHashMap<String, SubmissionStatusTaskDescriptor> submissions)
  {
    this.submissions = submissions;
  }

  @Override public void run()
  {
    ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    while (true) {
      List<Future<SubmissionStatus>> submissionStatusFutures = new ArrayList<>();

      for (SubmissionStatusTaskDescriptor submissionStatusTaskDescriptor : submissions.values()) {
        SubmissionStatusTask submissionStatusTask = submissionStatusTaskDescriptor.getSubmissionStatusTask();
        Future<SubmissionStatus> submissionStatusFuture = executor.submit(submissionStatusTask);
        submissionStatusFutures.add(submissionStatusFuture);
      }

      for (Future<SubmissionStatus> submissionStatusFuture : submissionStatusFutures) {
        try {
          SubmissionStatus submissionStatus = submissionStatusFuture.get(100, TimeUnit.MILLISECONDS);
          SubmissionStatusTaskDescriptor currentSubmissionStatusTaskDescriptor =  submissions.get(submissionStatus.getSubmissionID());


          // TODO
        } catch (InterruptedException e) {
          // TODO
        } catch (ExecutionException e) {
          // TODO
        } catch (TimeoutException e) {
          // TODO
        }
      }
    }
  }
}

