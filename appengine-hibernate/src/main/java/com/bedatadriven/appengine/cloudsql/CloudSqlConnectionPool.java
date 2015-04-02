package com.bedatadriven.appengine.cloudsql;


import com.google.appengine.api.LifecycleManager;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import org.hibernate.HibernateException;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    private static final int MAX_CONNECTIONS = 12;
    
    private static final int MAX_IDLE_SECONDS = 10;
    
    private static final int MAX_WAIT_MILLISECONDS = 1500;

    private ConnectionFactory factory;
    private final ConcurrentLinkedQueue<IdlingConnection> idle = new ConcurrentLinkedQueue<>();
    
    private final AtomicInteger openConnections = new AtomicInteger(0);
    
    private final AtomicBoolean shutdownHookRegistered = new AtomicBoolean(false);

    /**
     * Ensure that a single connection is allocated per request.
     */
    private final ThreadLocal<CloudSqlConnection> requestConnection = new ThreadLocal<>();
    
    
    private CloudSqlConnectionPool() {
        
    }
    
    public void setFactory(ConnectionFactory connectionFactory) {
        this.factory = connectionFactory;
    }
    
    public CloudSqlConnection get() {
        Preconditions.checkState(factory != null, "Connection factory has not been set.");
        Preconditions.checkState(requestConnection.get() == null, "There is already an open connection for this request");

        CloudSqlConnection connection = lease();
        
        requestConnection.set(connection);
        
        return connection;
    }

    private CloudSqlConnection lease() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        while(stopwatch.elapsed(TimeUnit.MILLISECONDS) < MAX_WAIT_MILLISECONDS) {

            // Try to reuse an existing connection
            IdlingConnection connection = idle.poll();
            if (connection != null) {
                if (connection.getIdleSeconds() > MAX_IDLE_SECONDS) {
                    evict(connection);
                    
                } else if(tryActivate(connection)) {    
                    LOGGER.fine("Leased idle connection " + connection.getConnection().getConnectionId() +
                            " from pool. Idle count = " + idle.size() + ", " +
                            "open connection count = " + openConnections.get());
                    return connection.getConnection();
                }
            }

            // If that's not possible, create a new connection if we haven't 
            // reached our limit
            if(openConnections.get() < MAX_CONNECTIONS) {
                CloudSqlConnection newConnection = tryCreate();
                if(newConnection != null) {
                    LOGGER.fine("Leased new connection. Idle count = " + idle.size() + ", " +
                            "open connection count = " + openConnections.get());
                    return newConnection;
                }
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new HibernateException("Interrupted while waiting to try again for a connection");
            }
        }
        throw new HibernateException("Timed out while trying to connect");
    }

    public void release(CloudSqlConnection connection) {
        if (requestConnection.get() != connection) {
            throw new IllegalStateException("Connection does not belong to the current request!");
        }

        requestConnection.set(null);

        if (connection.timedOut()) {
            LOGGER.info("Disposing of connection that had time out");
            openConnections.decrementAndGet();
            terminate(connection);
            
        } else {
            connection.shutdownExecutor();
            idle.add(new IdlingConnection(connection));
            ensureShutdownHookRegistered();
        }
    }

    private void ensureShutdownHookRegistered() {
        if(shutdownHookRegistered.compareAndSet(false, true)) {
            LifecycleManager.getInstance().setShutdownHook(new LifecycleManager.ShutdownHook() {
                @Override
                public void shutdown() {
                    LOGGER.severe("Shutting down: closing all idle connections...");
                    stop();
                }
            });
        }
    }

    private void terminate(CloudSqlConnection connection) {
        try {
            connection.close();
            LOGGER.info("Successfully closed connection " +  connection.getConnectionId());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to close connection with timed-out query", e);
        }
    }

    private CloudSqlConnection tryCreate() {
        try {
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
            openConnections.incrementAndGet();
            return connection;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect", e);
            return null;
        }
    }

    private boolean tryActivate(IdlingConnection connection) {
        try {
            factory.activate(connection.getConnection());
            return true;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to activate idling connection", e);
            openConnections.decrementAndGet();
            return false;
        }
    }

    private void evict(IdlingConnection connection) {
        try {
            connection.getConnection().close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to close idled connection", e);
        }
        
        openConnections.decrementAndGet();
    }
    
    public void cleanupRequest() {
        CloudSqlConnection connection = requestConnection.get();
        if(connection != null) {
            LOGGER.log(Level.INFO, "Releasing this request's open connection");
            release(connection);
        }
    }

    public void stop() {
        IdlingConnection connection;
        while((connection=idle.poll())!=null) {
            evict(connection);
        }
    }
}
