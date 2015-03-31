package com.bedatadriven.appengine.cloudsql.cache;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import org.hibernate.cache.spi.GeneralDataRegion;

import java.util.Collections;
import java.util.Map;

public abstract class AbstractRegion implements GeneralDataRegion {

    protected final String regionName;
    protected final MemcacheService memcache;

    protected AbstractRegion(String regionName) {
        this.regionName = regionName;
        this.memcache = MemcacheServiceFactory.getMemcacheService();
    }


    @Override
    public long getSizeInMemory() {
        return -1;
    }

    @Override
    public long getElementCountInMemory() {
        return -1;
    }

    @Override
    public long getElementCountOnDisk() {
        return -1;
    }

    @Override
    public Map toMap() {
        return Collections.emptyMap();
    }

    @Override
    public long nextTimestamp() {
        return System.currentTimeMillis() / 100;
    }

    @Override
    public int getTimeout() {
        return 0;
    }


    @Override
    public String getName() {
        return regionName;
    }
}
