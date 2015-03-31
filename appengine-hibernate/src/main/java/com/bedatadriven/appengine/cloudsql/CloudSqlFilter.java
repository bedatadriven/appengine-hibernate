package com.bedatadriven.appengine.cloudsql;

import org.hibernate.QueryTimeoutException;
import org.hibernate.exception.JDBCConnectionException;

import javax.persistence.PersistenceException;
import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLTimeoutException;
import java.util.logging.Logger;

/**
 * Cleans up any open connections at the end of a request
 */
public class CloudSqlFilter implements Filter {
    
    private static final Logger LOGGER = Logger.getLogger(CloudSqlFilter.class.getName());
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            filterChain.doFilter(servletRequest, servletResponse);

        } catch (Exception e) {
            if(isDatabaseProblem(e)) {
                LOGGER.severe("Request failed due to SQL timeout.");
                HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
                httpServletResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                httpServletResponse.getWriter().write("503 Service Unavailable");
            }
            throw e;
            
        } finally {
            CloudSqlConnectionProvider.cleanupRequest();
        }
    }
    
    private boolean isDatabaseProblem(Throwable e) {
        if(e instanceof PersistenceException) {
            return e instanceof javax.persistence.QueryTimeoutException ||
                    isDatabaseProblem(e.getCause());
        }
        return e instanceof QueryTimeoutException ||
                e instanceof JDBCConnectionException;
    }

    @Override
    public void destroy() {

    }
}
