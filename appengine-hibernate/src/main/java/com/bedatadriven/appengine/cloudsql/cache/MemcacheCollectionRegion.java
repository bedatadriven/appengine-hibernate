package com.bedatadriven.appengine.cloudsql.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;

import java.util.Properties;

public class MemcacheCollectionRegion extends AbstractSimpleRegion implements CollectionRegion {
    private Properties properties;
    private CacheDataDescription metadata;

    public MemcacheCollectionRegion(String regionName, Properties properties, CacheDataDescription metadata) {
        super(regionName);
        this.properties = properties;
        this.metadata = metadata;
    }


    @Override
    public CollectionRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTransactionAware() {
        return false;
    }

    @Override
    public CacheDataDescription getCacheDataDescription() {
        return metadata;
    }
    
    
}
