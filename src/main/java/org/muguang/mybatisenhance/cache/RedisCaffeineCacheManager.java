package org.muguang.mybatisenhance.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class RedisCaffeineCacheManager implements CacheManager {

    /**
     * key: cacheName value: redisCaffeineCache
     */
    private ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<String, Cache>();

    private RedisTemplate<String, Object> stringKeyRedisTemplate;

    private RedisCaffeineCacheProperties redisCaffeineCacheProperties;

    /**
     * 是否根据cacheName动态生成
     */
    private boolean dynamic = true;

    /**
     * 不动态根据cacheName创建Cache的实现时，自定义设置的缓存名
     */
    private Set<String> cacheNames;

    /**
     * 一级缓存名集合
     */
    private Set<String> firstCacheNames;

    /**
     * 一级缓存的过期时间
     */
    private Map<String, Long> expires;

    public RedisCaffeineCacheManager(RedisTemplate<String, Object> redisTemplate, RedisCaffeineCacheProperties redisCaffeineCacheProperties) {
        super();
        this.stringKeyRedisTemplate = redisTemplate;
        this.redisCaffeineCacheProperties = redisCaffeineCacheProperties;
        this.cacheNames = redisCaffeineCacheProperties.getCacheNames();
        this.firstCacheNames = redisCaffeineCacheProperties.getFirstCacheNames();
        this.expires = redisCaffeineCacheProperties.getCaffeine().getExpires();

    }


    @Override
    public Cache getCache(String name) {
        Cache cache = cacheMap.get(name);
        if (cache != null) {
            return cache;
        }
        //如果不动态根据cacheName创建Cache的实现，且没有配置静态缓存  则返回空
        if (!dynamic && cacheNames.isEmpty()) {
            return null;
        }
        //如果开启一级缓存
        if (firstCacheNames.contains(name)) {
            //开启一级缓存
            cache = new RedisCaffeineCache(name, stringKeyRedisTemplate, caffeineCache(name), redisCaffeineCacheProperties);
        }else {
            // 只开启二级缓存 即只开启redis缓存
            cache = new RedisCaffeineCache(name, stringKeyRedisTemplate, null, redisCaffeineCacheProperties);
        }
        Cache oldCache = cacheMap.putIfAbsent(name, cache);
        return oldCache == null ? cache : oldCache;
    }

    /**
     * 构建一个caffeineCache 即构建一个一级缓存
     * @param name
     * @return
     */
    private com.github.benmanes.caffeine.cache.Cache caffeineCache(String name) {
        Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder();
        if(redisCaffeineCacheProperties.getCaffeine().getExpireAfterAccess() > 0) {
            caffeineBuilder.expireAfterAccess(redisCaffeineCacheProperties.getCaffeine().getExpireAfterAccess(), TimeUnit.MILLISECONDS);
        }
        Long expire = expires.get(name);
        if (expire != null && expire > 0) {
            caffeineBuilder.expireAfterWrite(expire, TimeUnit.MILLISECONDS);
        }else if (redisCaffeineCacheProperties.getCaffeine().getExpireAfterWrite() > 0){
            caffeineBuilder.expireAfterWrite(redisCaffeineCacheProperties.getCaffeine().getExpireAfterAccess(), TimeUnit.MILLISECONDS);
        }
        if (redisCaffeineCacheProperties.getCaffeine().getRefreshAfterWrite() > 0) {
            caffeineBuilder.refreshAfterWrite(redisCaffeineCacheProperties.getCaffeine().getRefreshAfterWrite(), TimeUnit.MILLISECONDS);
        }
        if (redisCaffeineCacheProperties.getCaffeine().getInitialCapacity() > 0) {
            caffeineBuilder.initialCapacity(redisCaffeineCacheProperties.getCaffeine().getInitialCapacity());
        }
        if (redisCaffeineCacheProperties.getCaffeine().getMaximumSize() > 0) {
            caffeineBuilder.maximumSize(redisCaffeineCacheProperties.getCaffeine().getMaximumSize());
        }

        return caffeineBuilder.build();
    }

    @Override
    public Collection<String> getCacheNames() {
        return this.cacheNames;
    }

    /**
     * 清除 多级缓存中的某一级缓存
     * @param name
     * @param key
     * @param redisCaffeineCacheUniqueKey
     */
    public void clearLocal(String name, Object key,String redisCaffeineCacheUniqueKey) {
        Cache cache = cacheMap.get(name);
        if(cache == null) {
            return ;
        }

        RedisCaffeineCache redisCaffeineCache = (RedisCaffeineCache) cache;
        //判断是否是自己发送的消息 如果是自己发送的就不需要清楚缓存了
        if (redisCaffeineCacheUniqueKey!=null && redisCaffeineCacheUniqueKey.length()!=0 && redisCaffeineCache.getUniqueKey().equals(redisCaffeineCacheUniqueKey)) {
            return;
        }
        redisCaffeineCache.clearLocal(key);

    }
}
