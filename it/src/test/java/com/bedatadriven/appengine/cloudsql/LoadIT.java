package com.bedatadriven.appengine.cloudsql;

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
        
        resetDatabase();
        warmUp();
        writeTest();
        slowQueryTest();
        
        Metrics.stop();
    }

    public void resetDatabase() {

        ConsoleStatus.start("Resetting database");

        Response response = root.path("reset").request().post(Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        if(response.getStatus() != 200) {
            throw new AssertionError("Could not reset database: " + response.getStatusInfo());
        }

        ConsoleStatus.finish();
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

    
    private void writeTest() throws InterruptedException {
        System.out.println("WRITE TEST");
        System.out.println("==========");

        LoadTester tester = new LoadTester();
        tester.add(new WriteRequest(root), LogisticGrowthFunction.rampUpTo(300).during(Period.minutes(4)));
        tester.run(10, TimeUnit.MINUTES);
        tester.stop();
    }
    
    private void slowQueryTest() throws InterruptedException {
        System.out.println("SLOW QUERY TEST");
        System.out.println("==========");

        LoadTester tester = new LoadTester();
        tester.add(new WriteRequest(root), LogisticGrowthFunction.rampUpTo(150).during(Period.minutes(3)));
        tester.add(new SlowQuery(root), new ConstantRate(3));
        tester.run(6, TimeUnit.MINUTES);
        tester.stop();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        
        LoadIT integrationTest = new LoadIT();
        integrationTest.run();
      
    }
}
