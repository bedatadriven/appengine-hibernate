package com.bedatadriven.appengine.cloudsql.cache;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.GeneralDataRegion;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Basic implementation of a memcache-backed cache region.
 */
public abstract class AbstractMemcacheRegion implements GeneralDataRegion {

    private final MemcacheService memcacheService;
    private final String name;
    private final HashFunction hashFunction;


    protected AbstractMemcacheRegion(String name) {
        this.name = name;
        this.hashFunction = Hashing.goodFastHash(32);
        this.memcacheService = MemcacheServiceFactory.getMemcacheService("hibernate." + name);
    }

    private String versionKey(Object key) {
        return "hibernate:" + name + ':' + String.valueOf(key);
    }

    private String valueKey(Object key, long versionNumber) {
        return versionKey(key) + ":" + versionNumber;
    }

    @Override
    public Object get(Object key) throws CacheException {
        Long currentVersion = getVersion(key);
        if(currentVersion == null) {
            return null;
        }
        return get(key, currentVersion);
    }

    private Object get(Object key, long currentVersion) {
        return memcacheService.get(valueKey(key, currentVersion));
    }

    private Long getVersion(Object key) {
        return (Long)memcacheService.get(versionKey(key));
    }

    @Override
    public void put(Object key, Object value) throws CacheException {
        put(key, nextTimestamp(), value);
    }

    public boolean put(Object key, long txTimestamp, Object value) {

        String versionKey = versionKey(key);


        // check for the current version cached
        MemcacheService.IdentifiableValue currentVersion = memcacheService.getIdentifiable(versionKey);
        
        if (!isNewer(currentVersion, txTimestamp)) {
            return false;
        }

        // store the value first so we don't risk the version number appearing before the
        // corresponding value is written
        memcacheService.put(valueKey(key, txTimestamp), value);

        // Now update the version key if a newer version hasn't already been stored
        while (!updateVersionKey(versionKey, currentVersion, txTimestamp)) {
            // ... in the meantime another process has updated the cache with a version
            // but we don't know if it's older or younger, so we have to try again
            currentVersion = memcacheService.getIdentifiable(versionKey);
        }
        return true;
    }

    private boolean updateVersionKey(String versionKey, MemcacheService.IdentifiableValue currentVersion, long txTimestamp) {
        if (currentVersion == null) {
            // No existing version...
            return memcacheService.put(versionKey, 
                    txTimestamp,
                    versionExpiration(),
                    MemcacheService.SetPolicy.ADD_ONLY_IF_NOT_PRESENT);
        } else {
            return memcacheService.putIfUntouched(
                    versionKey, 
                    currentVersion, 
                    txTimestamp, 
                    versionExpiration());
        }
    }


    private boolean isNewer(MemcacheService.IdentifiableValue currentVersion, long newVersion) {
        if (currentVersion == null) {
            return true;
        }
        Number currentVersionNumber = (Number) currentVersion.getValue();
        return currentVersionNumber.longValue() < newVersion;
    }

    private Expiration versionExpiration() {
        return Expiration.byDeltaMillis((int) TimeUnit.HOURS.toMillis(1));
    }

    @Override
    public void evict(Object key) throws CacheException {
        memcacheService.delete(versionKey(key));
    }

    @Override
    public void evictAll() throws CacheException {
        // no op for now
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void destroy() throws CacheException {
        // no op
    }

    @Override
    public boolean contains(Object key) {
        return memcacheService.contains(versionKey(key));
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
        // dividing by one hundred because that what's other implementations seem to do...
        return System.currentTimeMillis() / 100;
    }

    @Override
    public int getTimeout() {
        return 0;
    }
}
