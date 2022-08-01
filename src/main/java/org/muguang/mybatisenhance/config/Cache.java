package org.muguang.mybatisenhance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.Callable;


@Slf4j
public class Cache extends AbstractValueAdaptingCache {

    private final String uniqueKey = UUID.randomUUID().toString().replace("-", "");

    private RedisTemplate<String, Object> redisTemplate;
    private com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache;

    protected Cache(boolean allowNullValues) {
        super(allowNullValues);
    }


    @Override
    protected Object lookup(Object key) {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Object getNativeCache() {
        return null;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return null;
    }

    @Override
    public void put(Object key, Object value) {

    }

    @Override
    public void evict(Object key) {

    }

    @Override
    public void clear() {

    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        return super.putIfAbsent(key, value);
    }

    @Override
    public boolean evictIfPresent(Object key) {
        return super.evictIfPresent(key);
    }

    @Override
    public boolean invalidate() {
        return super.invalidate();
    }
}
