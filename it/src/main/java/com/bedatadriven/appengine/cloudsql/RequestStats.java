package com.bedatadriven.appengine.cloudsql;

import com.google.apphosting.api.ApiProxy;
import com.google.common.base.Stopwatch;

import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;


public class RequestStats {
    public static final String CONNECT_TIME_HEADER = "X-Connect-Time";
    public static final String CONNECTION_COUNT_HEADER = "X-Connection-Count";
    public static final String TRANSACT_TIME_HEADER = "X-Transaction-Time";
    public static final String REQUEST_TIME_HEADER = "X-Request-Time";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    private static final ThreadLocal<RequestStats> STATS = new ThreadLocal<>();
    private long connectionCount;


    public static RequestStats current() {
        RequestStats stats = STATS.get();
        if(stats == null) {
            stats = new RequestStats();
            STATS.set(stats);
        }
        return stats;
    }
    
    private Stopwatch transactionStopwatch = Stopwatch.createUnstarted();
    private Stopwatch connectionStopwatch = Stopwatch.createUnstarted();
    
    public RequestStats() {
    }

    public Stopwatch getTransactionStopwatch() {
        return transactionStopwatch;
    }

    public Stopwatch getConnectionStopwatch() {
        return connectionStopwatch;
    }

    public static Response wrap(Response.ResponseBuilder builder) {
        builder.header(CONNECT_TIME_HEADER, current().connectionStopwatch.elapsed(TimeUnit.NANOSECONDS));
        builder.header(TRANSACT_TIME_HEADER, current().transactionStopwatch.elapsed(TimeUnit.NANOSECONDS));
        builder.header(CONNECTION_COUNT_HEADER, current().connectionCount);
        builder.header(REQUEST_ID_HEADER,
                ApiProxy.getCurrentEnvironment().getAttributes().get("com.google.appengine.runtime.request_log_id"));




        long totalRequestTime = TimeUnit.SECONDS.toMillis(60) - ApiProxy.getCurrentEnvironment().getRemainingMillis();
        if(totalRequestTime > 0) {
            builder.header(REQUEST_TIME_HEADER, TimeUnit.MILLISECONDS.toNanos(totalRequestTime));
        }
        
        return builder.build();
    }

    public void setConnectionCount(long count) {
        this.connectionCount = count;
    }
}
