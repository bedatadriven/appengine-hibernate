package com.bedatadriven.appengine.cloudsql;

import com.google.common.base.Strings;
import org.hibernate.QueryTimeoutException;
import org.hibernate.exception.JDBCConnectionException;

import javax.persistence.PersistenceException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
        
        LOGGER.fine("Starting...");
        
        if(isTaskQueueRequest(servletRequest)) {
            RequestTimer.getCurrent().taskRequestStarted();
        } else {
            RequestTimer.getCurrent().frontEndRequestStarted();
        }
        
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
            CloudSqlConnectionPool.INSTANCE.cleanupRequest();
        }
    }

    private boolean isTaskQueueRequest(ServletRequest servletRequest) {
        if(servletRequest instanceof HttpServletRequest) {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            return !Strings.isNullOrEmpty(request.getHeader("X-AppEngine-QueueName"));
        } else {
            return false;
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
