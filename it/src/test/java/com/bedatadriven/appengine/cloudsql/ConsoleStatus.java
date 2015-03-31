package com.bedatadriven.appengine.cloudsql;

import java.io.PrintStream;

/**
 * Simple console magic...
 */
public class ConsoleStatus {
    
    private String currentTask;
    private PrintStream out = System.out;

    public void start(String taskName) {
        out.print(taskName);
        out.print("...");
        out.flush();
        currentTask = taskName;
    }
    
    public void updateProgress(String progress) {
        out.print("\r");
        out.print(currentTask);
        out.print(": ");
        out.print(progress);
        out.flush();
    }
 
    public void finish(String message) {
        out.print("\r");
        out.print(currentTask);
        out.print(": ");
        out.println(message);
        out.flush();
    }

    public void finish() {
        finish("Done.");
    }

    public void finish(String message, Object... args) {
        finish(String.format(message, args));
    }
    
    public void tick() {
        out.print(".");
        out.flush();
    }

    public void failed() {
        finish("FAILED");
    }

    public ConsoleStatus updateProgress() {
        updateProgress("");
        return this;
    }
    
    public ConsoleStatus stat(String format, Object... args) {
        out.print(String.format(format, args));
        out.print("   ");
        out.flush();
        return this;
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
