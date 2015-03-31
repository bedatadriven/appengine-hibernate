package com.bedatadriven.appengine.cloudsql;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CloudSqlConnection implements Connection {
    
    private final Connection delegate;
    private final QueryExecutor executor;

    /**
     * We will mark a connection as closed if there is a timeout
     */
    private boolean closed = false;
    
    private final Logger LOGGER = Logger.getLogger(CloudSqlConnection.class.getName());
    
    public CloudSqlConnection(Connection conn) {
        this.delegate = conn;
        this.executor = new QueryExecutor();
    }

    private Statement wrap(Statement statement) {
        return (Statement)Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] { Statement.class },
                new StatementProxy(executor, statement));    }
    
    private PreparedStatement wrap(PreparedStatement statement) {
        return (PreparedStatement)Proxy.newProxyInstance(getClass().getClassLoader(), 
                new Class[] { PreparedStatement.class }, 
                new StatementProxy(executor, statement));
    }
    
    private void checkState() throws SQLException {
        if(closed) {
            throw new SQLException("Connection has been closed after a query timeout");
        }
    }
    

    @Override
    public Statement createStatement() throws SQLException {
        checkState();
        
        return wrap(delegate.createStatement());
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        checkState();
        
        return wrap(delegate.prepareStatement(sql));
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        checkState();
        
        return delegate.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        checkState();
        
        return delegate.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        checkState();        

        delegate.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        checkState();

        return delegate.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        checkState();
        
        delegate.commit();
    }

    @Override
    public void rollback() throws SQLException {
        checkState();
        
        delegate.rollback();
    }

    @Override
    public void close() throws SQLException {
        try {
            executor.shutdown();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to shutdown query executor", e);
        }
        delegate.close();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed || delegate.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        checkState();
        
        return delegate.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        checkState();
        
        delegate.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return delegate.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        checkState();
        
        delegate.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        checkState();
        
        return delegate.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        checkState();
        
        delegate.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        checkState();
        
        return delegate.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        checkState();
        
        return delegate.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        checkState();
        
        delegate.clearWarnings();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        checkState();
        
        return wrap(delegate.createStatement(resultSetType, resultSetConcurrency));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        checkState();
        
        
        return wrap(delegate.prepareStatement(sql, resultSetType, resultSetConcurrency));
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        checkState();
        
        return delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        checkState();
        
        return delegate.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        checkState();
        
        delegate.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        checkState();
        
        delegate.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
        checkState();
        
        return delegate.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        checkState();
        
        return delegate.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        checkState();
        
        return delegate.setSavepoint(name);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        checkState();
        
        delegate.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        checkState();
        
        delegate.releaseSavepoint(savepoint);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        checkState();
        
        return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        checkState();
        
        return wrap(delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        checkState();
        
        return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        checkState();
        
        return wrap(delegate.prepareStatement(sql, autoGeneratedKeys));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        checkState();
        
        return wrap(delegate.prepareStatement(sql, columnIndexes));
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        checkState();
        
        return wrap(delegate.prepareStatement(sql, columnNames));
    }

    @Override
    public Clob createClob() throws SQLException {
        checkState();
        
        return delegate.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        checkState();
        
        return delegate.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        checkState();
        
        return delegate.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        checkState();
        
        return delegate.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return !closed && delegate.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        delegate.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        delegate.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        checkState();
        
        return delegate.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        checkState();
        
        return delegate.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        checkState();
        
        return delegate.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        checkState();
        
        return delegate.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        checkState();
        
        delegate.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
        checkState();
        
        return delegate.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        checkState();
        
        delegate.abort(executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        delegate.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return delegate.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }

    public void cleanup() {
        if(executor.timedOut()) {
            // don't risk delaying if 
            executor.shutdown();
            
            LOGGER.severe("There were query timeouts, not attempting to close connection.");
        
        } else {
            try {
                if (!isClosed()) {
                    close();
                }
            } catch (Exception e) {
                LOGGER.severe("Failed to close connection");

            }
        }
        
    }
}
