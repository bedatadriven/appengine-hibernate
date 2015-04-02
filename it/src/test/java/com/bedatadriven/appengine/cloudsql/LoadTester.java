package com.bedatadriven.appengine.cloudsql;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Stopwatch;

import javax.ws.rs.client.Invocation;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class LoadTester {


    public static final double MAX_FAILURES_PER_MINUTE = 5;
    
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
            
            double failuresOneMinute = RequestSubmitter.FAILURES.getOneMinuteRate() * 60d;
            
            ConsoleStatus.updateProgress()
                    .stat("Pending: %4d", RequestSubmitter.PENDING_REQUESTS.getCount())
                    .stat("Failures (1min): %3.1f", failuresOneMinute)
                    .timingHistogram("Txn", RequestSubmitter.TRANSACTION_TIME)
                    .timingHistogram("Roundtrip", RequestSubmitter.REQUEST_TIMER);
            
            
            if(failuresOneMinute > MAX_FAILURES_PER_MINUTE) {
                ConsoleStatus.failed();
                System.err.printf("Request failure rate exceeded %2.0f/min:\n", MAX_FAILURES_PER_MINUTE);
                System.err.printf("  One-minute rate: %2.1f/min\n", failuresOneMinute);
                System.err.printf("  Total count    : %d\n", RequestSubmitter.FAILURES.getCount());
                System.exit(-1);
            }
            
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
