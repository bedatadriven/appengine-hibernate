package com.bedatadriven.appengine.cloudsql;

import com.google.apphosting.api.ApiProxy;

class RequestTimerImpl extends RequestTimer {
    @Override
    public long getRemainingMillis() {
        return ApiProxy.getCurrentEnvironment().getRemainingMillis();
    }
}
