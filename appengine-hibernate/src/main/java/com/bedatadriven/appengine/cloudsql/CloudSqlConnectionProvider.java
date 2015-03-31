package com.bedatadriven.appengine.cloudsql;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class CloudSqlConnectionProvider implements ConnectionProvider, Configurable, Stoppable {

    private static final Logger LOGGER = Logger.getLogger(CloudSqlConnectionProvider.class.getName());
    
    public static final ThreadLocal<CloudSqlConnection> REQUEST_CONNECTION = new ThreadLocal<>();
    
    private String url;
    private Properties connectionProps;
    private Integer isolation;
    private int poolSize;
    private boolean autocommit;

    private boolean stopped;
    
    private Driver driver;

    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return ConnectionProvider.class.equals( unwrapType ) ||
                CloudSqlConnectionProvider.class.isAssignableFrom( unwrapType );
    }

    @Override
    @SuppressWarnings( {"unchecked"})
    public <T> T unwrap(Class<T> unwrapType) {
        if ( ConnectionProvider.class.equals( unwrapType ) ||
                CloudSqlConnectionProvider.class.isAssignableFrom( unwrapType ) ) {
            return (T) this;
        }
        else {
            throw new UnknownUnwrapTypeException( unwrapType );
        }
    }

    public void configure(Map configurationValues) {
        LOGGER.info("CloudSql Connection Provider starting.");

        String driverClassName = (String) configurationValues.get( AvailableSettings.DRIVER );
        if ( driverClassName == null ) {
            LOGGER.info("JDBC driver class not provided in property " + AvailableSettings.DRIVER);
        
        } else {
            try {
                // trying via forName() first to be as close to DriverManager's semantics
                driver = (Driver) Class.forName( driverClassName ).newInstance();
            }
            catch ( Exception e1 ) {
                try{
                    driver = (Driver) ReflectHelper.classForName( driverClassName ).newInstance();
                }
                catch ( Exception e2 ) {
                    throw new HibernateException( "Specified JDBC Driver " + driverClassName + " could not be loaded", e2 );
                }
            }
        }


        autocommit = ConfigurationHelper.getBoolean( AvailableSettings.AUTOCOMMIT, configurationValues );
        LOGGER.fine("Autocommit: " + (autocommit ? "ENABLED" : "DISABLED"));

        isolation = ConfigurationHelper.getInteger( AvailableSettings.ISOLATION, configurationValues );

        url = (String) configurationValues.get( AvailableSettings.URL );
        if ( url == null ) {
            String msg = "Connection url must be specified with the " + AvailableSettings.URL + " property";
            LOGGER.severe(msg);
            throw new HibernateException( msg );
        }

        connectionProps = ConnectionProviderInitiator.getConnectionProperties(configurationValues);

        LOGGER.info("Connecting to " + url);
    }

    public void stop() {
//        LOG.cleaningUpConnectionPool( url );
//
//        for ( Connection connection : pool ) {
//            try {
//                connection.close();
//            }
//            catch (SQLException sqle) {
//                LOG.unableToClosePooledConnection( sqle );
//            }
//        }
//        pool.clear();
        stopped = true;
    }

    public Connection getConnection() throws SQLException {
        
        
        
        if(REQUEST_CONNECTION.get() != null) {
            throw new IllegalStateException("A connection is already open for this request");
        }
        
//        LOG.tracev( "Total checked-out connections: {0}", checkedOut );
//
//        // essentially, if we have available connections in the pool, use one...
//        synchronized (pool) {
//            if ( !pool.isEmpty() ) {
//                int last = pool.size() - 1;
//                LOG.tracev( "Using pooled JDBC connection, pool size: {0}", last );
//                Connection pooled = pool.remove( last );
//                if ( isolation != null ) {
//                    pooled.setTransactionIsolation( isolation.intValue() );
//                }
//                if ( pooled.getAutoCommit() != autocommit ) {
//                    pooled.setAutoCommit( autocommit );
//                }
//                checkedOut++;
//                return pooled;
//            }
//        }

        // otherwise we open a new connection...

        LOGGER.fine("Opening new JDBC connection");
        
        
        long start = System.nanoTime();

        Connection conn;
        if ( driver != null ) {
            // If a Driver is available, completely circumvent
            // DriverManager#getConnection.  It attempts to double check
            // ClassLoaders before using a Driver.  This does not work well in
            // OSGi environments without wonky workarounds.
            conn = driver.connect( url, connectionProps );
        }
        else {
            // If no Driver, fall back on the original method.
            conn = DriverManager.getConnection( url, connectionProps );
        }

        if ( isolation != null ) {
            conn.setTransactionIsolation(isolation);
        }
        if ( conn.getAutoCommit() != autocommit ) {
            conn.setAutoCommit(autocommit);
        }

        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        
        LOGGER.fine(String.format("Created connection to: %s, Isolation Level: %s in %d ms",
                url, conn.getTransactionIsolation(), elapsed ));
        
        
        CloudSqlConnection wrappedConnection = new CloudSqlConnection(conn);
        
        
        REQUEST_CONNECTION.set(wrappedConnection);

        //checkedOut++;
        return wrappedConnection;
    }

    public void closeConnection(Connection conn) throws SQLException {

//        int currentSize = pool.size();
//        if ( currentSize < poolSize ) {
//            LOG.tracev( "Returning connection to pool, pool size: {0}", ( currentSize + 1 ) );
//            pool.add(conn);
//            return;
//        }
        
        if(REQUEST_CONNECTION.get() != conn) {
            throw new IllegalStateException("Connection does not belong to the current request!");
        }
        
        REQUEST_CONNECTION.set(null);

        LOGGER.fine("Closing JDBC connection");
        conn.close();
    }


    public boolean supportsAggressiveRelease() {
        return false;
    }

    public static void cleanupRequest() {
        CloudSqlConnection connection = REQUEST_CONNECTION.get();
        if(connection != null) {
            LOGGER.log(Level.FINE, "Cleaning up connection opened during request.");
            REQUEST_CONNECTION.set(null);
            try {
                connection.cleanup();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Exception closing connection: " + e.getMessage(), e);
            }
        }
    }
}
