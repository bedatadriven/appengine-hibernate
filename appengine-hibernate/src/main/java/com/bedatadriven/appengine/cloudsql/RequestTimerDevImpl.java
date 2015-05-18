package com.bedatadriven.appengine.cloudsql;

import java.util.concurrent.TimeUnit;

/**
 * Provides a RequestTimer implementation that provides correct 
 * time remaining on the local development server.
 * 
 * <p>This indirection is necessary because 
 * {@link com.google.apphosting.api.ApiProxy.Environment#getRemainingMillis()} always returns 60_000, even
 * if the current request is a task queue invocation.</p>
 */
class RequestTimerDevImpl extends RequestTimer {


    private long requestStartTime;
    private long limit;

    public void frontEndRequestStarted() {
        this.requestStartTime = System.currentTimeMillis();
        this.limit = 60_000;
    }

    public void taskRequestStarted() {
        this.requestStartTime = System.currentTimeMillis();
        this.limit = TimeUnit.MINUTES.toMillis(10);
    }

    @Override
    public long getRemainingMillis() {
        long requestDuration = System.currentTimeMillis() - requestStartTime;
        long timeRemaining = limit - requestDuration;
        return timeRemaining;
    }


}
