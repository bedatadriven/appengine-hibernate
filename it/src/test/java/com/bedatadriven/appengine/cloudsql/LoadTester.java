package com.bedatadriven.appengine.cloudsql;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Stopwatch;

import javax.ws.rs.client.Invocation;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class LoadTester {

    
    
    private final ExecutorService executor;
    private final MetricRegistry metrics = new MetricRegistry();
    private final String testName;

    public LoadTester(String testName) {
        this.executor = Executors.newCachedThreadPool();
        this.testName = testName;
    }
    
    public void add(Runnable runnable) {
        executor.submit(runnable);
    }
    
    public void add(Iterator<Invocation> request, LoadFunction loadFunction) {
        executor.submit(new RequestSubmitter(executor, request, loadFunction));
    }

    public boolean run(long duration, TimeUnit unit) throws InterruptedException {

        ConsoleStatus.start(testName);
        
        Stopwatch stopwatch = Stopwatch.createStarted();
        while(stopwatch.elapsed(unit) < duration) {
            
            Thread.sleep(500);
            
            ConsoleStatus.updateProgress()
                    .stat("Cnxs: %3.0f", RequestSubmitter.CONNECTION_COUNT.getSnapshot().getMean())
                    .stat("Pending: %4d", RequestSubmitter.PENDING_REQUESTS.getCount())
                    .stat("Failures: %4d", RequestSubmitter.FAILURES.getCount())
                    .timingHistogram("Txn", RequestSubmitter.TRANSACTION_TIME)
                    .timingHistogram("Roundtrip", RequestSubmitter.REQUEST_TIMER);
            
            if(RequestSubmitter.FAILURES.getCount() > 5) {
                System.out.println();
                return false;
            }
        }

        System.out.println();

        return true;
    }

    
    public void stop() {
        executor.shutdownNow();
    }

}
