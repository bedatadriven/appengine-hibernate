package com.bedatadriven.appengine.cloudsql;


public interface LoadFunction {

    /**
     * 
     * @param milliseconds the time in milliseconds elapsed since the beginning of the test
     * @return the number of concurrent requests to target
     */
    int apply(long milliseconds);
}
