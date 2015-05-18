package com.bedatadriven.appengine.cloudsql;

import com.google.appengine.api.utils.SystemProperty;

import java.util.concurrent.TimeUnit;

/**
 * Provides <em>correct</em> remaining time allowed for the request on both development and production
 * AppEngine environments.
 */
abstract class RequestTimer {
    
    private static final ThreadLocal<RequestTimer> CURRENT = new ThreadLocal<>();
    
    RequestTimer() {
    }
    
    public void frontEndRequestStarted() {
    }
    
    public void taskRequestStarted() {
    }
    
    public abstract long getRemainingMillis();
    
    
    public static RequestTimer getCurrent() {
        RequestTimer timer = CURRENT.get();
        if(timer == null) {
            timer = createTimer();
            CURRENT.set(timer);
        }
        return timer;
    }

    private static RequestTimer createTimer() {
        if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Production) {
            return new RequestTimerImpl();
        } else {
            return new RequestTimerDevImpl();
        }
    }
}
