package com.bedatadriven.appengine.cloudsql.cache;

import org.hibernate.cache.CacheException;


public abstract class AbstractSimpleRegion extends AbstractRegion {
    
    protected AbstractSimpleRegion(String regionName) {
        super(regionName);
    }

    protected final String valueKey(Object key) {
        return "hibernate:" + getName() + ':' + String.valueOf(key);
    }
    
    @Override
    public Object get(Object key) throws CacheException {
        return memcache.get(valueKey(key));
    }

    @Override
    public void put(Object key, Object value) throws CacheException {
        memcache.put(key, value);
    }

    @Override
    public void evict(Object key) throws CacheException {
        memcache.delete(key);
    }

    @Override
    public void evictAll() throws CacheException {
        
    }

    @Override
    public void destroy() throws CacheException {

    }

    @Override
    public boolean contains(Object key) {
        return memcache.contains(valueKey(key));
    }
}
