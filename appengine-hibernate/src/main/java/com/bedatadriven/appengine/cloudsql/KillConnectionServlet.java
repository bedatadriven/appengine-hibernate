package com.bedatadriven.appengine.cloudsql;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KillConnectionServlet extends HttpServlet {
    
    public static final long MAX_QUERY_TIME_SECONDS = 120;
    
    private static final Logger LOGGER = Logger.getLogger(KillConnectionServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        CloudSqlConnection cloudSqlConnection = ConnectionPool.INSTANCE.get();
        try {
            try (Statement statement = cloudSqlConnection.createStatement()) {
                List<Long> toKill = new ArrayList<>();

                try (ResultSet resultSet = statement.executeQuery("SHOW PROCESSLIST")) {
                    while (resultSet.next()) {
                        long time = resultSet.getLong("Time");
                        if (time > MAX_QUERY_TIME_SECONDS) {
                            toKill.add(time);
                        }
                    }
                }

                for (Long connectionId : toKill) {
                    try {
                        statement.executeUpdate("KILL " + connectionId);
                    } catch (Exception e) {
                        LOGGER.warning("Failed to kill " + connectionId + ": " + e.getMessage());
                    }
                }
                statement.close();

            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Exception while killing zombie processes", e);
        } finally {
            try {
                ConnectionPool.INSTANCE.release(cloudSqlConnection);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to release connection", e);
            }
        }
    }
}
