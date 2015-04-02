package com.bedatadriven.appengine.cloudsql;

import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;


public class CloudSqlConnectionProvider implements ConnectionProvider, Configurable, Stoppable {

    private static final Logger LOGGER = Logger.getLogger(CloudSqlConnectionProvider.class.getName());
    
    
    private boolean stopped = false;

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

        LOGGER.info("Configuring CloudSql Connection Provider");

        CloudSqlConnectionPool.INSTANCE.setFactory(new ConnectionFactory(configurationValues));
    }
    

    public void stop() {
        CloudSqlConnectionPool.INSTANCE.stop();
        stopped = true;
    }

    public Connection getConnection() throws SQLException {
        return CloudSqlConnectionPool.INSTANCE.get();
    }

    public void closeConnection(Connection conn) throws SQLException {
        CloudSqlConnectionPool.INSTANCE.release((CloudSqlConnection)conn);
    }


    public boolean supportsAggressiveRelease() {
        return false;
    }

}
