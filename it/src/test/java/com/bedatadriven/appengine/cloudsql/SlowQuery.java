package com.bedatadriven.appengine.cloudsql;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;


public class SlowQuery implements Runnable {
    private WebTarget root;

    public SlowQuery(WebTarget root) {
        this.root = root;
    }
    
    @Override
    public void run() {
        while(true) {
            Response response = root
                    .path("greetings/latest")
                    .queryParam("maxResults", 50000)
                    .request()
                    .get();

            if (response.getStatus() != 200 && response.getStatus() != 503) {
                RequestSubmitter.FAILURES.inc();
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                System.out.println("SlowQuery interrupted, stopping.");
                return;
            }
        }
    }
}
