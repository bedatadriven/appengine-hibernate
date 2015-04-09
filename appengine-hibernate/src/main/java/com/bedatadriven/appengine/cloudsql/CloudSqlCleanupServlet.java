package com.bedatadriven.appengine.cloudsql;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public abstract class CloudSqlCleanupServlet extends HttpServlet {

    public static final long MAX_QUERY_TIME_SECONDS = 120;

    private static final Logger LOGGER = Logger.getLogger(CloudSqlCleanupServlet.class.getName());

    @Override
    protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        EntityManager entityManager = openEntityManager();
        List<Object[]> connections = entityManager.createNativeQuery("SHOW PROCESSLIST").getResultList();
        for(Object[] connection : connections) {
            Number connectionId = (Number) connection[0];
            String host = (String)connection[2];
            String command = (String)connection[4];
            Number time = (Number)connection[5];
            String info = (String)connection[7];

            if(isAppEngineConnection(host)) {
                if(shouldBeClosed(command, time.longValue())) {
                    LOGGER.info(String.format("Cleaning up connection %d. Command: %s, Time: %d, Info: %s",
                            connectionId.longValue(), command, time.longValue(), info));
                }
            }
        }
        entityManager.close();
    }

    protected final boolean shouldBeClosed(String command, long time) {

        if("Sleep".equals(command) && time >  (CloudSqlConnectionPool.MAX_IDLE_SECONDS + 15)) {
            return true;
        }
        if("Query".equals(command) && time > 60) {
            return true;
        }
        return false;
    }

    protected abstract EntityManager openEntityManager();

    private boolean isAppEngineConnection(String host) {
        return "localhost".equals(host);
    }
}
