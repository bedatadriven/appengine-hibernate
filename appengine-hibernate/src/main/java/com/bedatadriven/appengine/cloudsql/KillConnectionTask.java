package com.bedatadriven.appengine.cloudsql;

import com.google.appengine.api.taskqueue.DeferredTask;

import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background task to kill connections which timed out
 */
public class KillConnectionTask implements DeferredTask {
    
    private static final Logger LOGGER = Logger.getLogger(KillConnectionTask.class.getName());
    
    private long connectionId;
    private ConnectionFactory connectionFactory;

    public KillConnectionTask() {
    }

    public KillConnectionTask(ConnectionFactory connectionFactory, CloudSqlConnection connection) {
        this.connectionFactory = connectionFactory;
        this.connectionId = connection.getConnectionId();
    }

    public long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(long connectionId) {
        this.connectionId = connectionId;
    }

    @Override
    public void run() {
        ConnectionPool.INSTANCE.setFactory(connectionFactory);
        
        CloudSqlConnection cloudSqlConnection = ConnectionPool.INSTANCE.get();
        try {
            LOGGER.info("Killing connection " + connectionId);
            try(Statement statement = cloudSqlConnection.createStatement()) {
                statement.executeUpdate("KILL " + connectionId);
            } catch(Exception e) {
                LOGGER.log(Level.WARNING, "Failed to kill connection " + connectionId + ", it may already be closed.", e);
            }
        } finally {
            try {
                ConnectionPool.INSTANCE.release(cloudSqlConnection);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to release connection", e);
            }
        }
    }
}
