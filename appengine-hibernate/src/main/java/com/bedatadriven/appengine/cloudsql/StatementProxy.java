package com.bedatadriven.appengine.cloudsql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Proxy for {@code Statement}s and {@code PreparedStatement}s
 */
public class StatementProxy implements InvocationHandler {
    
    private static final Logger LOGGER = Logger.getLogger(StatementProxy.class.getName());
    
    private final QueryExecutor executor;
    private final Statement delegate;

    public StatementProxy(QueryExecutor executor, Statement delegate) {
        this.executor = executor;
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
        
        if (isQuery(method.getName())) {
            /*
             * Invoke queries in a separate worker thread with a timeout
             */
            return executor.executeQueryWithTimeout(delegate, new Callable<Object>() {

                @Override
                public Object call() throws Exception {
                    return method.invoke(delegate, args);
                }
            });
        } else {
            return method.invoke(delegate, args);
        }
    }


    private boolean isQuery(String name) {
        return name.equals("execute") || name.equals("executeUpdate") || name.equals("executeQuery");
    }

}
