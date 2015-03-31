package com.bedatadriven.appengine.cloudsql;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;


public class LoadTester {

    private final List<String> dictionary;
    private final List<String> authors;
    private final AtomicLong authorIndex = new AtomicLong(0);
    
    private final WebResource greetings;
    
    private final Counter pendingRequests = new Counter();
    private final ExecutorService executor;
    
    private final RequestSubmitter writer;
    private final RequestSubmitter reader;

    private Meter writes = new Meter();
    private Meter reads = new Meter();
    private Counter failures = new Counter();
    
    private double deadlockRate = 0;
    
    private final ConsoleStatus status = new ConsoleStatus();
    
    public LoadTester() throws IOException {

        dictionary = loadList("dict.txt");
        authors = loadList("authors.txt");
        
        greetings = Client.create().resource("http://hibernate.ai-capacity-test.appspot.com/test");
        executor = Executors.newCachedThreadPool();
        
        writer = new RequestSubmitter(10, new Write(), executor);
        reader = new RequestSubmitter(10, new Read(), executor);
    }
    
    public void start() {
        executor.submit(writer);
        executor.submit(reader);
    }
    
    public LoadTester setConcurrentReads(int count) {
        reader.setConcurrency(count);
        return this;
    }
    
    public LoadTester setConcurrentWrites(int count) {
        writer.setConcurrency(count);
        return this;
    }
    
    public void resetDatabase() {
        
        status.start("Resetting database");
        
        Form form = new Form();
        form.putSingle("reset", "true");
        String resetResponse = Client.create().resource("http://hibernate.ai-capacity-test.appspot.com/test")
                .post(String.class, form);

        status.finish();
    }


    public void warmUp() throws InterruptedException {
        writes = new Meter();

        status.start("Warming up");
        
        while(writes.getCount()  < 20) {
            Thread.sleep(500);
            status.tick();
        }
        status.finish();
    }
    
    public void populate(int targetCount) throws InterruptedException {
        writes = new Meter();
        writer.setBackoffEnabled(true);

        status.start("Populating database");
        
        while(writes.getCount() < targetCount) {
            Thread.sleep(1000);
            status.updateProgress()
                    .percentage("%3.0f%%  ", writes.getCount(), targetCount)
                    .stat("Writes: %7d", writes.getCount())
                    .stat("Rate: %5.1f w/s", writes.getOneMinuteRate());
            
            if(failures.getCount() > writes.getCount()) {
                status.failed();
                System.exit(-2);
            }
        }
        status.finish("%d rows written.", writes.getCount());
    }


    public boolean testConcurrency(long duration, TimeUnit unit) throws InterruptedException {
        writes = new Meter();
        failures = new Counter();

        status.start(String.format("Testing concurrency=%3d", (reader.getConcurrency() + writer.getConcurrency())));
        
        Stopwatch stopwatch = Stopwatch.createStarted();
        while(stopwatch.elapsed(unit) < duration) {
            
            Thread.sleep(500);
            
            status.updateProgress()
                    .stat("Pending: %4d", pendingRequests.getCount())
                    .stat("Failures: %4d", failures.getCount())
                    .stat("Writes/s: %5.1f", writes.getOneMinuteRate())
                    .stat("Reads/s: %5.1f", reads.getOneMinuteRate());
            
            
            if(failures.getCount() > 5) {
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
    

    private static List<String> loadList(String resourceName) throws IOException {
        return Resources.readLines(Resources.getResource(LoadTester.class, resourceName), Charsets.UTF_8);
    }

    private class Read implements Callable<Boolean> {
        
        @Override
        public Boolean call() throws Exception {
            int maxResults = 500;
            if(ThreadLocalRandom.current().nextDouble() < 0.1) {
                maxResults = 50000;
            } else if(ThreadLocalRandom.current().nextDouble() < 0.25) {
                maxResults = 5000;
            }


            try {
                pendingRequests.inc();
                WebResource query = greetings
                        .queryParam("maxResults", Integer.toString(maxResults));
                
                if(ThreadLocalRandom.current().nextDouble() < deadlockRate) {
                    query.queryParam("deadlock", "true");
                }
                
                ClientResponse content = query.get(ClientResponse.class);
                InputStream in = content.getEntityInputStream();
                ByteStreams.copy(in, ByteStreams.nullOutputStream());

                reads.mark();
                return true;

            } catch (Exception e) {
                failures.inc();
                return false;
            } finally {
                pendingRequests.dec();
            }
        }
    }


    private class Write implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            long nextAuthor = authorIndex.getAndIncrement() % authors.size();
            String author = authors.get((int)nextAuthor);
            
            Form form = new Form();
            form.putSingle("author", author);
            form.putSingle("content", randomContent());
    
            try {
                pendingRequests.inc();
                greetings.post(form);
                writes.mark();
                return true;
                
            } catch (Exception e) {
                failures.inc();
                return false;
            
            } finally {
                pendingRequests.dec();
            }
        }
    
        private String randomContent() {
            StringBuilder sb = new StringBuilder();
            for(int i=0;i<5;++i) {
                if(i>0) {
                    sb.append(' ');
                }
                sb.append(dictionary.get(ThreadLocalRandom.current().nextInt(0, dictionary.size())));
            }
            return sb.toString();
        }
    }
}
