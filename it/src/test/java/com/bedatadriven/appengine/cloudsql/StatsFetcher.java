package com.bedatadriven.appengine.cloudsql;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

/**
 * Created by alex on 1-4-15.
 */
public class StatsFetcher implements Runnable {
    @Override
    public void run() {
        Client client = ClientBuilder.newClient();
        String response = client.target("https://hibernate.ai-capacity-test.appspot.com/test/stats").request().get(String.class);
        
    }
}
