package org.muguang.mybatisenhance.das;

import com.github.benmanes.caffeine.cache.AsyncCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.ignite.IgniteCache;
import org.springframework.cache.support.AbstractValueAdaptingCache;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class LocalCache extends AbstractValueAdaptingCache {

    private final String name;
    private final IgniteCache<Object, Object> igniteCache;
    private final AsyncCache<Object, Object> caffeineCache;
    private final Lock lock = new ReentrantLock(true);


    protected LocalCache(String name, IgniteCache<Object, Object> igniteCache, AsyncCache<Object, Object> caffeineCache, boolean allowNullValue) {
        super(allowNullValue);
        this.name = name;
        this.igniteCache = igniteCache;
        this.caffeineCache = caffeineCache;
    }

    @Override
    protected Object lookup(Object key) {
        CompletableFuture<Object> future = caffeineCache.get(key, igniteCache::get);
        return future.join();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object value = lookup(key);
        if (value!=null){
            return (T)value;
        } else {
            lock.lock();
            try {
                T v = valueLoader.call();
                put(key, v);
                return v;
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void put(Object key, Object value) {
        caffeineCache.put(key, CompletableFuture.supplyAsync(()->value));
        igniteCache.putAsync(key, value);
    }

    @Override
    public void evict(Object key) {
        caffeineCache.synchronous().invalidate(key);
        igniteCache.removeAsync(key);
    }

    @Override
    public void clear() {
        caffeineCache.synchronous().cleanUp();
        igniteCache.clearAsync();
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        igniteCache.put(key, value);
        caffeineCache.synchronous().put(key, value);
        return () -> value;
    }

    @Override
    public boolean evictIfPresent(Object key) {
        caffeineCache.synchronous().invalidate(key);
        return igniteCache.removeAsync(key).get();
    }

    @Override
    public boolean invalidate() {
        igniteCache.clearAsync().get();
        caffeineCache.synchronous().invalidateAll();
        return true;
    }

    public boolean clearLocal(Object key) {
        caffeineCache.synchronous().invalidate(key);
        return true;
    }
}
