package org.muguang.mybatisenhance.config;

import org.muguang.mybatisenhance.cache.RedisCaffeineCacheProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DasCacheManager implements CacheManager {

    private ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

    private RedisTemplate<String, Object> redisTemplate;

    private CacheProperties config;


    public DasCacheManager(RedisTemplate<String, Object> redisTemplate, CacheProperties config) {
        this.redisTemplate = redisTemplate;
        this.config = config;
    }

    @Override
    public Cache getCache(String name) {
        return null;
    }

    @Override
    public Collection<String> getCacheNames() {
        return null;
    }
}
