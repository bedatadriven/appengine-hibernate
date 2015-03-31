package com.bedatadriven.appengine.cloudsql;

import com.google.appengine.api.utils.SystemProperty;
import com.google.common.base.Stopwatch;
import org.hibernate.cfg.AvailableSettings;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class JPA {
    
    private static EntityManagerFactory newFactory(Map<String, String> extraProperties) {
        Map<String, String> properties = new HashMap<>();
        properties.putAll(extraProperties);
        
        properties.put(AvailableSettings.CONNECTION_PROVIDER, CloudSqlConnectionProvider.class.getName());
        if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Production) {
            properties.put("javax.persistence.jdbc.driver", "com.mysql.jdbc.GoogleDriver");
            properties.put("javax.persistence.jdbc.url", System.getProperty("cloudsql.url"));
        } else {
            properties.put("javax.persistence.jdbc.driver", "com.mysql.jdbc.Driver");
            properties.put("javax.persistence.jdbc.url", System.getProperty("cloudsql.url.dev"));
        }

        return Persistence.createEntityManagerFactory("Demo", properties);
    }
    
    public static EntityManagerFactory newFactory() {
        return newFactory(Collections.<String, String>emptyMap());
    }
    
    public static EntityManagerFactory newAutoUpdatingFactory() {
        Map<String, String> extraProperties = new HashMap<>();
        extraProperties.put(AvailableSettings.HBM2DDL_AUTO, "create-drop");
        
        return newFactory(extraProperties);
    }
}
