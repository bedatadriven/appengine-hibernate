package com.bedatadriven.appengine.cloudsql;

import com.google.common.collect.UnmodifiableIterator;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;


class WriteRequest extends UnmodifiableIterator<Invocation> {

    private final WebTarget root;

    public WriteRequest(WebTarget root) {
        this.root = root;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public Invocation next() {

        MultivaluedStringMap form = new MultivaluedStringMap();
        form.putSingle("author", Dictionary.INSTANCE.nextAuthor());
        form.putSingle("content", Dictionary.INSTANCE.randomContent());

        return root
                .path("greeting")
                .request()
                .buildPost(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }

}
