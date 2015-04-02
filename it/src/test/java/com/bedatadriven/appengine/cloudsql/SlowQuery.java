package com.bedatadriven.appengine.cloudsql;

import com.google.common.collect.UnmodifiableIterator;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import java.util.Iterator;


public class SlowQuery extends UnmodifiableIterator<Invocation> {
    private WebTarget root;

    public SlowQuery(WebTarget root) {
        this.root = root;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public Invocation next() {
        return root
                .queryParam("maxResults", 50000)
                .request()
                .buildGet();
    }
}
