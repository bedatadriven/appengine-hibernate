package com.bedatadriven.appengine.cloudsql;

import java.util.concurrent.*;


public class RequestSubmitter implements Runnable {

    private final Callable<Boolean> task;
    private final java.util.concurrent.Executor executor;

    private int concurrency;
    private boolean backoffEnabled;

    public RequestSubmitter(int concurrency, Callable<Boolean> task, Executor executor) {
        this.concurrency = concurrency;
        this.task = task;
        this.executor = executor;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }
    
    public boolean isBackoffEnabled() {
        return backoffEnabled;
    }

    public void setBackoffEnabled(boolean backoffEnabled) {
        this.backoffEnabled = backoffEnabled;
    }

    @Override
    public void run() {
        ExecutorCompletionService<Boolean> ecs = new ExecutorCompletionService<>(executor);
        long pending = 0;

        try {
            while(true) {
                while(pending < concurrency) {
                    ecs.submit(task);
                    pending++;
                }
                if(pending  >  0) {
                    // wait for something to finish
                    boolean succeeded = ecs.take().get();
                    pending--;
                    if(!succeeded && backoffEnabled) {
                        // Give the server some air to breath...
                        Thread.sleep(ThreadLocalRandom.current().nextLong(1500, 5000));
                    }
                } else {
                    // wait for our concurrency factor to be increased...
                    Thread.sleep(500);
                }
            }
        } catch (InterruptedException ignored) {
            System.out.println("Request Submitter interrupted.");

        } catch (ExecutionException e) {
            // Not expected: looking for a true/false from the task
            // if an exception is thrown then there is a programming error somehw
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
