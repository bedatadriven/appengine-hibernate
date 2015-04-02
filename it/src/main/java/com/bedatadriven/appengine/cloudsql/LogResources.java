package com.bedatadriven.appengine.cloudsql;


import com.google.appengine.api.log.LogQuery;
import com.google.appengine.api.log.LogService;
import com.google.appengine.api.log.LogServiceFactory;
import com.google.appengine.api.log.RequestLogs;
import com.google.appengine.api.modules.ModulesService;
import com.google.appengine.api.modules.ModulesServiceFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

@Path("/logs")
public class LogResources {
    
    
    @GET
    @Path("/pending")
    @Produces("text/plain")
    public String getMedianPendingTime() {

        LogService logService = LogServiceFactory.getLogService();
        LogQuery logQuery = LogQuery.Builder.withIncludeAppLogs(false)
                .startTimeMillis(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(60))
                .endTimeMillis(System.currentTimeMillis());


        StringBuilder content = new StringBuilder();
        content.append("request.id,loading,pending,latency,mcycles\n");
        Iterable<RequestLogs> logs = logService.fetch(logQuery);
        for (RequestLogs log : logs) {
            content.append(log.getRequestId()).append(",");
            content.append(log.isLoadingRequest() ? "TRUE" : "FALSE").append(",");
            content.append(log.getPendingTimeUsec()).append(",");
            content.append(log.getLatencyUsec()).append(",");
            content.append(log.getMcycles());
            content.append("\n");
        }
        return content.toString();
    }
        
}
