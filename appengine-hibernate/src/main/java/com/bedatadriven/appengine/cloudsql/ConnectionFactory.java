package com.bedatadriven.appengine.cloudsql;

import com.google.appengine.api.utils.SystemProperty;
import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.jdbc.connections.internal.ConnectionProviderInitiator;

import java.io.Serializable;
import java.security.AlgorithmParameterGenerator;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles creation of new Connections
 */
public class ConnectionFactory implements Serializable {
    
    private static final Logger LOGGER = Logger.getLogger(ConnectionFactory.class.getName());

    private static final String PRODUCTION_DRIVER_CLASS = "com.mysql.jdbc.GoogleDriver";
    
    private static final String DEVELOPMENT_DRIVER = "com.mysql.jdbc.Driver";
    
    private transient Driver driver;
    
    private boolean autocommit;
    private Integer isolation;
    private Properties connectionProps;
    private String url;

    public ConnectionFactory() {
    }
    

    public ConnectionFactory(Map configurationValues) {
  

        LOGGER.info("Configuring CloudSql Connection Provider");

        autocommit = ConfigurationHelper.getBoolean(AvailableSettings.AUTOCOMMIT, configurationValues);
        LOGGER.fine("Autocommit: " + (autocommit ? "ENABLED" : "DISABLED"));

        isolation = ConfigurationHelper.getInteger( AvailableSettings.ISOLATION, configurationValues );

        url = (String) configurationValues.get( AvailableSettings.URL );
        if ( url == null ) {
            String msg = "Connection url must be specified with the " + AvailableSettings.URL + " property";
            LOGGER.severe(msg);
            throw new HibernateException( msg );
        }

        LOGGER.info("Using connection URL " + url);

        connectionProps = ConnectionProviderInitiator.getConnectionProperties(configurationValues);
        
    }
    
    private Driver getDriver() {
        if(driver == null) {
            if (SystemProperty.Environment.environment.value() == SystemProperty.Environment.Value.Production) {
                initProductionDriver();
            } else {
                initDevelopmentDriver();
            }
        }
        return driver;
    }

    private void initProductionDriver() {

        LOGGER.info("Using driver " + PRODUCTION_DRIVER_CLASS);
        
        try {
            driver = (Driver)Class.forName(PRODUCTION_DRIVER_CLASS).newInstance();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to instantiate " + PRODUCTION_DRIVER_CLASS + ", ensure that " +
                    "<use-google-connector-j>true</use-google-connector-j> is declared in appengine-web.xml", e);
        }
    }


    private void initDevelopmentDriver() {

        LOGGER.info("Using driver " + DEVELOPMENT_DRIVER);

        try {
            driver = (Driver)Class.forName(DEVELOPMENT_DRIVER).newInstance();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to instantiate " + DEVELOPMENT_DRIVER, e);
        }
    }
    
    
    public Connection create() throws SQLException {
        Connection conn = connectWithRetries();

        LOGGER.fine(String.format("Created connection to: %s, Isolation Level: %s",
            url, conn.getTransactionIsolation()));

        return activate(conn);
    }

    private Connection connectWithRetries() throws SQLException {
        int attemptNumber = 0;
        while(true) {
            try {
                return getDriver().connect(url, connectionProps);

            } catch (SQLRecoverableException connectionException) {

                if (attemptNumber > 2) {
                    throw connectionException;
                }

                LOGGER.warning("Failed to connect to database: " + connectionException.getMessage());

                try {
                    // Backoff exponentially
                    Thread.sleep(((int)
                        Math.round(Math.pow(2, attemptNumber)) * 1000) +
                            ThreadLocalRandom.current().nextInt(1, 1000));
                } catch (InterruptedException interruptedException) {
                    LOGGER.severe("Thread interrupted while waiting to retry.");
                    throw connectionException;
                }

                attemptNumber++;
            }
        }
    }

    public Connection activate(Connection conn) throws SQLException {

        if (isolation != null) {
            conn.setTransactionIsolation(isolation);
        }
        if (conn.getAutoCommit() != autocommit) {
            conn.setAutoCommit(autocommit);
        }
        for (String property : conn.getClientInfo().stringPropertyNames()) {
            LOGGER.fine(property + " = " + conn.getClientInfo().getProperty(property));
        }
        return conn;
    }
}
