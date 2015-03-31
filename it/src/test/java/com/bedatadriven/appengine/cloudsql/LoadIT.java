package com.bedatadriven.appengine.cloudsql;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.representation.Form;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class LoadIT {

    private static final int BASE_ROW_COUNT = 10_000
            ;
    private LoadTester tester;

    public LoadIT() throws IOException {
        tester = new LoadTester();
    }
    
    public void run() throws InterruptedException {

        tester.start();
        //tester.resetDatabase();
        tester.warmUp();
      //ester.populate(BASE_ROW_COUNT);
        
        int maxConcurrentRequests = findMaxConcurrentRequests();
        
        tester.stop();
        
        System.out.println("Max concurrent requests: " + maxConcurrentRequests);
        if(maxConcurrentRequests < 50) {
            System.out.println("FAILED");
            System.exit(-1);
        }
    }


    private int findMaxConcurrentRequests() throws InterruptedException {
        int rate = 10;  
        int maxConcurrent = 0;
        while(true) {
            System.out.printf("Target rate: %d concurrent request(s)\n", rate);
            tester.setConcurrentReads(rate);
            tester.setConcurrentWrites(rate);

            if(!tester.testConcurrency(90, TimeUnit.SECONDS)) {
                return maxConcurrent;
            }
            
            maxConcurrent = rate;
            rate += 10;
        }
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        
        LoadIT integrationTest = new LoadIT();
        integrationTest.run();
      
    }
}
