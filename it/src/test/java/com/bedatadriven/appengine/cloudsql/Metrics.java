package com.bedatadriven.appengine.cloudsql;

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class Metrics {
    
    public static final MetricRegistry REGISTRY = new MetricRegistry();
    private static CsvReporter CSV_REPORTER;

    public static void start() {
        File metricsOut = new File("target/metrics");
        if(!metricsOut.exists()) {
            metricsOut.mkdirs();
        }
        CSV_REPORTER = CsvReporter.forRegistry(REGISTRY).build(metricsOut);
        CSV_REPORTER.start(10, TimeUnit.SECONDS);
    }

    public static void stop() {
        CSV_REPORTER.stop();
    }
}
