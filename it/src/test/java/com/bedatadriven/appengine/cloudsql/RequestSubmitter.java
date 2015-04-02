package com.bedatadriven.appengine.cloudsql;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.SlidingTimeWindowReservoir;
import com.codahale.metrics.Timer;
import com.google.common.base.Stopwatch;
import com.google.common.io.ByteStreams;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.*;

/**
 * Submits requests to the server with a specific concurrency
 */
public class RequestSubmitter implements Runnable {

    public static final Counter FAILURES = Metrics.REGISTRY.counter("failed");
    public static final Counter PENDING_REQUESTS = Metrics.REGISTRY.counter("pending");
    
    public static final Histogram CONNECTION_COUNT = Metrics.REGISTRY.register("connections",
            new Histogram(new SlidingTimeWindowReservoir(10, TimeUnit.SECONDS)));
    

    public static final Timer REQUEST_TIMER = Metrics.REGISTRY.timer("requests");
    public static final Timer TRANSACTION_TIME = Metrics.REGISTRY.timer("transactions");

    private final java.util.concurrent.Executor executor;
    private final Iterator<Invocation> requests;

    private boolean backoffEnabled;
    
    private final LoadFunction loadFunction;
    private final Stopwatch runningTime;

    public RequestSubmitter(Executor executor, 
                            Iterator<Invocation> requestSupplier, 
                            LoadFunction loadFunction) {
        this.requests = requestSupplier;
        this.executor = executor;
        this.loadFunction = loadFunction;
        this.runningTime = Stopwatch.createStarted();
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

                long concurrency = loadFunction.apply(
                        runningTime.elapsed(TimeUnit.MILLISECONDS));

                while(pending < concurrency) {
                    ecs.submit(new Invoker(requests.next()));
                    pending++;
                }
                if(pending  >  0) {
                    // wait for something to finish
                    boolean succeeded = ecs.take().get();
                    pending--;

                    while(ecs.poll() != null) {
                        pending --;
                    }
                } else {
                    // wait for our concurrency factor to be increased...
                    Thread.sleep(500);
                }
            }
        } catch (InterruptedException ignored) {
            System.out.println("Request Submitter interrupted, stopping.");

        } catch (ExecutionException e) {
            // Not expected: looking for a true/false from the task
            // if an exception is thrown then there is a programming error and we should stop the test
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private class Invoker implements Callable<Boolean> {

        private final Invocation invocation;

        public Invoker(Invocation invocation) {
            this.invocation = invocation;
        }

        @Override
        public Boolean call() throws Exception {
            Response response;
            PENDING_REQUESTS.inc();
            try {
                Timer.Context time = REQUEST_TIMER.time();
                try {
                    response = invocation.invoke();

                } catch (Exception e) {
                    ErrorLog.log(e);
                    FAILURES.inc();
                    return false;
                }

                if (response.getStatus() != 200) {
                    ErrorLog.log(response.getStatusInfo());
                    FAILURES.inc();
                    return false;
                }
                
                try (InputStream in = response.readEntity(InputStream.class)) {
                    ByteStreams.copy(in, ByteStreams.nullOutputStream());
                }
                
                long latency = time.stop();

                long connectionTime = Long.parseLong(response.getHeaderString(RequestStats.CONNECT_TIME_HEADER));
                
                long transactionTime = Long.parseLong(response.getHeaderString(RequestStats.TRANSACT_TIME_HEADER));
                if(transactionTime != 0) {
                    TRANSACTION_TIME.update(transactionTime, TimeUnit.NANOSECONDS);
                }
                
                long connectionCount = Long.parseLong(response.getHeaderString(RequestStats.CONNECTION_COUNT_HEADER));
                if(connectionCount != 0) {
                    CONNECTION_COUNT.update(connectionCount);
                }

                ErrorLog.requestLog.println(String.format("%d,%s,%d,%d,%d",
                        System.currentTimeMillis(),
                        response.getHeaderString(RequestStats.REQUEST_ID_HEADER),
                        PENDING_REQUESTS.getCount(),
                        TimeUnit.NANOSECONDS.toMillis(latency),
                        connectionCount));
                
                return true;
                
            } finally {
                PENDING_REQUESTS.dec();
            }
        }
    }

}
