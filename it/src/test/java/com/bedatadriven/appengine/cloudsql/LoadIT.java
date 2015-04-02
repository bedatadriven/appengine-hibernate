package com.bedatadriven.appengine.cloudsql;

import com.google.common.base.Stopwatch;
import org.joda.time.Period;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class LoadIT {

    private WebTarget root;

    public LoadIT() throws IOException {
    }
    
    public void run() throws InterruptedException, FileNotFoundException {

        root = ClientBuilder.newClient().target("http://hibernate.ai-capacity-test.appspot.com/test");

        Metrics.start();
        ErrorLog.open();
        
        warmUp();
        exceptionTest();
        writeTest();
        slowQueryTest();
        
        connectionCleanupTest();
        
        Metrics.stop();
    }
    
    public void warmUp() {
        ConsoleStatus.start("Warm up");

        // fire off a few requests to make sure the server is awake
        for(int i=0;i<10;++i) {
            root.path("greetings").request().get();
            ConsoleStatus.tick();
        }
        ConsoleStatus.finish();
    }
    
    
    public void exceptionTest() {
        ConsoleStatus.start("Exception reporting");
        
        Response response = root.path("exception").request().get();
        ConsoleStatus.finish(Integer.toString(response.getStatus()));

        if(response.getStatus() != 200) {
            System.err.println(response.readEntity(String.class));
            System.exit(-1);
        }
    }

    /**
     * Ensure that simple write requests are able to scale up to 300 concurrent requests without
     * encountering errors.
     */
    private void writeTest() throws InterruptedException {
        LoadTester tester = new LoadTester("Write Test");
        tester.add(new WriteRequest(root), LogisticGrowthFunction.rampUpTo(300).during(Period.minutes(4)));
        tester.run(10, TimeUnit.MINUTES);
        tester.stop();
    }

    /**
     * Ensure that a few "toxic" queries don't lead to cycle of instance shutdowns and restarts
     */
    private void slowQueryTest() throws InterruptedException {
        LoadTester tester = new LoadTester("Slow Query Test");
        tester.add(new WriteRequest(root), LogisticGrowthFunction.rampUpTo(400).during(Period.minutes(4)));
        
        for(int i=1;i<10;++i) {
            tester.add(new SlowQuery(root));
        }
        tester.run(6, TimeUnit.MINUTES);
        tester.stop();
    }

    /**
     * Ensure that once a spike in activity drops that connections are freed
     */
    private void connectionCleanupTest() throws InterruptedException {
        int numConnections = -1;
        ConsoleStatus.start("Connection cleanup test");
        Stopwatch stopwatch = Stopwatch.createStarted();
        while(stopwatch.elapsed(TimeUnit.MINUTES) < 5) {
            Thread.sleep(TimeUnit.SECONDS.toMillis(15));
            try {
                numConnections = Integer.parseInt(root.path("numConnections").request().get(String.class));
            } catch (Exception ignored) {
            }

            ConsoleStatus.updateProgress()
                .stat("Open Connections: %d", numConnections)
                .stat("Time Remaining: %d s", (TimeUnit.MINUTES.toSeconds(5) - stopwatch.elapsed(TimeUnit.SECONDS)));
        }
        
        if(numConnections < 5) {
            ConsoleStatus.finish("OK: " + numConnections + " remain open.");
        } else {
            ConsoleStatus.finish("FAIL: " + numConnections + " remain open.");
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        LoadIT integrationTest = new LoadIT();
        integrationTest.run();
      
    }
}
