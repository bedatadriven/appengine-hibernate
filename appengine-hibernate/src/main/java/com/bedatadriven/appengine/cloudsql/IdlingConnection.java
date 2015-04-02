package com.bedatadriven.appengine.cloudsql;

import java.util.concurrent.TimeUnit;


class IdlingConnection {
    private CloudSqlConnection connection;
    private long idleStart;

    public IdlingConnection(CloudSqlConnection connection) {
        this.connection = connection;
        this.idleStart = System.currentTimeMillis();
    }
    
    public long getIdleSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - idleStart);
    }

    public CloudSqlConnection getConnection() {
        return connection;
    }
}
