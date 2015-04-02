package com.bedatadriven.appengine.cloudsql;


public class ConstantRate implements LoadFunction {
    private int concurrent;

    public ConstantRate(int concurrent) {
        this.concurrent = concurrent;
    }

    @Override
    public int apply(long milliseconds) {
        return concurrent;
    }
}
