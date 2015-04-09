package com.bedatadriven.appengine.cloudsql;


import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import org.hibernate.HibernateException;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CloudSqlConnectionPool {

    public static final CloudSqlConnectionPool INSTANCE = new CloudSqlConnectionPool();

    private static final Logger LOGGER = Logger.getLogger(CloudSqlConnectionPool.class.getName());

    /**
     * When App Engine instances talk to Google Cloud SQL, each App Engine instance cannot have more
     * than 12 concurrent connections to a Cloud SQL instance.
     * https://cloud.google.com/appengine/docs/java/cloud-sql/#Java_Size_and_access_limits
     */
    public static final int MAX_CONNECTIONS = 12;
    
    public static final int MAX_IDLE_SECONDS = 10;

    private ConnectionFactory factory;
    
    private final Semaphore connections = new Semaphore(MAX_CONNECTIONS, true);


    /**
     * Ensure that a single connection is allocated per request.
     */
    private final ThreadLocal<CloudSqlConnection> requestConnection = new ThreadLocal<>();


    private CloudSqlConnectionPool() {

    }

    public void setFactory(ConnectionFactory connectionFactory) {
        this.factory = connectionFactory;
    }

    public CloudSqlConnection get() throws SQLTimeoutException {
        Preconditions.checkState(factory != null, "Connection factory has not been set.");
        Preconditions.checkState(requestConnection.get() == null, "There is already an open connection for this request");

        CloudSqlConnection connection = lease();

        requestConnection.set(connection);

        return connection;
    }

    private CloudSqlConnection lease() throws SQLTimeoutException {
        Stopwatch stopwatch = Stopwatch.createStarted();

        long queueLength = connections.getQueueLength();
        if(queueLength > 0) {
            LOGGER.warning("Approximately " + queueLength + " request(s) waiting for a connection");
        }
        
        try {
            if(!connections.tryAcquire(10, TimeUnit.SECONDS)) {
                throw new SQLTimeoutException("Timed out while waiting for a connection");
            }

        } catch (InterruptedException e) {
            throw new SQLTimeoutException("Interrupted while waiting for a connection");
        }

        long waitTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        if(waitTime > 500) {
            LOGGER.warning("Waited " + waitTime + " ms for a connection");
        }

        

        // If that's not possible or enabled, create a new connection
        try {
            CloudSqlConnection newConnection = create();

            LOGGER.fine("Leased new connection. Connections remaining: " + connections.availablePermits());

            return newConnection;
            
            
        } catch (Exception e) {
            connections.release();
            throw new HibernateException("Could not open a new connection ", e);
        }

    }

    public void release(CloudSqlConnection connection) {
        if (requestConnection.get() != connection) {
            throw new IllegalStateException("Connection does not belong to the current request!");
        }

        requestConnection.set(null);

        LOGGER.fine("Closing connection...");
        connections.release();
        terminate(connection);
    }


    private void terminate(CloudSqlConnection connection) {
        try {
            connection.close();
            LOGGER.info("Successfully closed connection " +  connection.getConnectionId());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to close connection with timed-out query", e);
        }
    }

    private CloudSqlConnection create() throws SQLException {
        CloudSqlConnection connection = new CloudSqlConnection(factory.create());
        try {
            connection.initConnectionId();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize connection", e);
            try {
                connection.close();
            } catch (Exception closingException) {
                LOGGER.log(Level.SEVERE, "Failed to close connection after initConnectionId() threw", e);
            }
            return null;
        }
        return connection;
    }

    public void cleanupRequest() {
        CloudSqlConnection connection = requestConnection.get();
        if(connection != null) {
            LOGGER.log(Level.INFO, "Releasing this request's open connection");
            release(connection);
        }
    }

    public void stop() {
     
    }
}
