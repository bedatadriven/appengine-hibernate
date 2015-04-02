package com.bedatadriven.appengine.cloudsql;

import com.google.common.collect.UnmodifiableIterator;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import java.util.concurrent.ThreadLocalRandom;

class Reads extends UnmodifiableIterator<Invocation> {

    private WebTarget root;

    Reads(WebTarget root) {
        this.root = root;
    }


    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public Invocation next()  {
        int maxResults = 500;
        if(ThreadLocalRandom.current().nextDouble() < 0.1) {
            maxResults = 50000;
        } else if(ThreadLocalRandom.current().nextDouble() < 0.25) {
            maxResults = 5000;
        }
        
        return root
            .queryParam("maxResults", maxResults)
            .request()
            .buildGet();
    }
}
