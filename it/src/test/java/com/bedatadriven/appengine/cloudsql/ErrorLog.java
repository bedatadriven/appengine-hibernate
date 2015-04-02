package com.bedatadriven.appengine.cloudsql;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ErrorLog {
    
    
    
    static {


        // turn off error dumsp froom jersey
        Logger.getLogger("org.glassfish").setLevel(Level.OFF);
    }
    
    private static PrintStream errorLog;
    public static PrintStream requestLog;

    public static void open() throws FileNotFoundException {
        errorLog = new PrintStream(new File("target/request-errors.log"));
        errorLog.println("time,request.id,pending.count,latency,connection.count");
        requestLog = new PrintStream(new File("target/request.log"));
        
    }
    
    public static void log(Exception e) {
        e.printStackTrace(errorLog);
        errorLog.flush();
    }


    public static void log(Response.StatusType statusInfo) {
        errorLog.println(statusInfo.getStatusCode() + " " + statusInfo.getReasonPhrase());
        errorLog.flush();
    }
}
