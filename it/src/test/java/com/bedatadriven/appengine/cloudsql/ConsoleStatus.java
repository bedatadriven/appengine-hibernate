package com.bedatadriven.appengine.cloudsql;

import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import java.io.Console;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

/**
 * Simple console magic...
 */
public class ConsoleStatus {
   
    private static String currentTask;
    private static PrintStream out = System.out;

    private ConsoleStatus() {}
    
    public static void start(String taskName) {
        out.print(taskName);
        out.print("...");
        out.flush();
        currentTask = taskName;
    }
    
    public static void updateProgress(String progress) {
        out.print("\r");
        out.print(currentTask);
        out.print(": ");
        out.print(progress);
        out.flush();
    }
 
    public static void finish(String message) {
        out.print("\r");
        out.print(currentTask);
        out.print(": ");
        out.println(message);
        out.flush();
    }

    public static void finish() {
        finish("Done.");
    }

    public static void finish(String message, Object... args) {
        finish(String.format(message, args));
    }
    
    public static void tick() {
        out.print(".");
        out.flush();
    }

    public static void failed() {
        finish("FAILED");
    }

    public static ConsoleStatus updateProgress() {
        updateProgress("");
        return new ConsoleStatus();
    }
    
    public ConsoleStatus stat(String format, Object... args) {
        out.print(String.format(format, args));
        out.print("   ");
        out.flush();
        return this;
    }
    
    public ConsoleStatus timingHistogram(String label, Timer timer) {
        Snapshot snapshot = timer.getSnapshot();
        double nanosecondsPerUnit = TimeUnit.MILLISECONDS.toNanos(1);
        return stat("%s: %4.1f/s   %5.0f   %5.0f ms", 
                label.toUpperCase(),
                timer.getOneMinuteRate(),
              //  snapshot.getValue(0.25D) / nanosecondsPerUnit,
                snapshot.getValue(0.50D) / nanosecondsPerUnit,
              //  snapshot.getValue(0.75D) / nanosecondsPerUnit,
                snapshot.getValue(0.95D) / nanosecondsPerUnit);
    }

    public ConsoleStatus percentage(String format, long numerator, long denominator) {
        double realNumerator = numerator;
        double realDenominator = denominator;
        double percentage = (realNumerator / realDenominator) * 100d;
        out.print(String.format(format, percentage));
        out.flush();
        return this;
    }
}
