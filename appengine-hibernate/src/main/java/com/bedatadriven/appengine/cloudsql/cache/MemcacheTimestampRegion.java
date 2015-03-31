package com.bedatadriven.appengine.cloudsql.cache;

import org.hibernate.cache.spi.TimestampsRegion;

import java.util.Properties;


public class MemcacheTimestampRegion extends AbstractSimpleRegion implements TimestampsRegion {

    public MemcacheTimestampRegion(String regionName, Properties properties) {
        super(regionName);
    }
}
