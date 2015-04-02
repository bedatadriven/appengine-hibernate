package com.bedatadriven.appengine.cloudsql;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;


public class TestApplication extends Application {

    public TestApplication() {
    }


    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> set = new HashSet<>();
        set.add(TestResources.class);
        set.add(LogResources.class);
        return set;
    }
}
