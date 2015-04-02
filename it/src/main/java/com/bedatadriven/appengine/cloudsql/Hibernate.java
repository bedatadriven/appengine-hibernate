package com.bedatadriven.appengine.cloudsql;

import com.google.appengine.api.utils.SystemProperty;
import com.google.common.base.Stopwatch;
import org.hibernate.cfg.AvailableSettings;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Hibernate {

    private static final Logger LOGGER = Logger.getLogger(Hibernate.class.getName());


    private static class LazyHolder {
        private static final EntityManagerFactory INSTANCE = newFactory();
    }
    
    private static EntityManagerFactory newFactory(Map<String, String> extraProperties) {
        
        LOGGER.info("Creating EntityManagerFactory...");
        
        try {

            Map<String, String> properties = new HashMap<>();
            properties.putAll(extraProperties);


            if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Production) {
                properties.put("javax.persistence.jdbc.driver", "com.mysql.jdbc.GoogleDriver");
                properties.put("javax.persistence.jdbc.url", System.getProperty("cloudsql.url"));
            } else {
                properties.put("javax.persistence.jdbc.driver", "com.mysql.jdbc.Driver");
                properties.put("javax.persistence.jdbc.url", System.getProperty("cloudsql.url.dev"));
            }

            properties.put(org.hibernate.ejb.AvailableSettings.INTERCEPTOR, TimingInterceptor.class.getName());

            return Persistence.createEntityManagerFactory("Demo", properties);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create EntityManagerFactory", e);
            throw e;
        }
    }

    public static EntityManagerFactory newFactory() {
        return newFactory(Collections.<String, String>emptyMap());
    }

    public static EntityManagerFactory newAutoUpdatingFactory() {
        Map<String, String> extraProperties = new HashMap<>();
        extraProperties.put(AvailableSettings.HBM2DDL_AUTO, "create-drop");

        return newFactory(extraProperties);
    }
    

    public static EntityManager createEntityManager() {

        if(RequestStats.current().getTransactionStopwatch().isRunning()) {
            RequestStats.current().getTransactionStopwatch().reset();
        }

        EntityManagerFactory emf = LazyHolder.INSTANCE;
        
        Stopwatch stopwatch = RequestStats.current().getConnectionStopwatch();
        stopwatch.start();
        EntityManager entityManager = emf.createEntityManager();
        stopwatch.stop();
        
        LOGGER.info("EntityManager created in " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return entityManager;
    }
}
