package com.bedatadriven.appengine.cloudsql.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.RegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;

import java.util.logging.Level;
import java.util.logging.Logger;


public class AbstractAccessStrategy implements RegionAccessStrategy {

    private static final Logger LOGGER = Logger.getLogger(AbstractAccessStrategy.class.getName());

    public static final SoftLock LOCKING_UNSUPPORTED = null;

    private final AbstractMemcacheRegion region;

    public AbstractAccessStrategy(AbstractMemcacheRegion region) {
        this.region = region;
    }

    @Override
    public Object get(Object key, long txTimestamp) throws CacheException {
        return region.get(key);
    }

    @Override
    public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) throws CacheException {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(String.format("putFromLoad(%s, %s, %d, %s)", key, value, txTimestamp, value));
        }
        return region.put(key, txTimestamp, value);
    }

    @Override
    public boolean putFromLoad(Object key, Object value, long txTimestamp, 
                               Object version, boolean minimalPutOverride) throws CacheException {
        
        return putFromLoad(key, value, txTimestamp, version);
    }

    @Override
    public SoftLock lockItem(Object key, Object version) throws CacheException {
        return LOCKING_UNSUPPORTED;
    }

    @Override
    public SoftLock lockRegion() throws CacheException {
        return LOCKING_UNSUPPORTED;
    }

    @Override
    public void unlockItem(Object key, SoftLock lock) throws CacheException {
        // Noop
    }

    @Override
    public void unlockRegion(SoftLock lock) throws CacheException {
        // Noop
    }

    @Override
    public void remove(Object key) throws CacheException {
        region.evict(key);
    }

    @Override
    public void removeAll() throws CacheException {
        region.evictAll();
    }

    @Override
    public void evict(Object key) throws CacheException {
        region.evict(key);
    }

    @Override
    public void evictAll() throws CacheException {
        region.evictAll();
    }
}
