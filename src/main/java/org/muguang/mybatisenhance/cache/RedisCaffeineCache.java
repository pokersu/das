package org.muguang.mybatisenhance.cache;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class RedisCaffeineCache extends AbstractValueAdaptingCache {

    private RedisTemplate<String, Object> redisTemplate;

    private String uniqueKey = UUID.randomUUID().toString();

    /**
     * 多级缓存的名称
     */
    private String name;

    /**
     * redis key前缀
     */
    private String cachePrefix;

    /**
     * redis key 的过期时间
     */
    private Map<String, Long> expires;

    /**
     * redis 全局过期时间
     */
    private long defaultExpiration = 0;

    /**
     * 定义一个全局的公平锁
     */
    private static ReentrantLock fairLock = new ReentrantLock(true);


    /**
     * 是否开启一级缓存
     */
    private boolean firstCache;

    /**
     * 一级缓存
     */
    private Cache<Object, Object> caffeineCache;

    /**
     * redis消息队列的topic
     */
    private String topic = "cache:redis:caffeine:topic";


    public RedisCaffeineCache(String name, RedisTemplate<String, Object> redisTemplate, Cache<Object, Object> caffeineCache,RedisCaffeineCacheProperties redisCaffeineCacheProperties) {
        super(redisCaffeineCacheProperties.isStoreNullValues());
        this.name = name;
        this.redisTemplate = redisTemplate;
        this.cachePrefix = redisCaffeineCacheProperties.getRedisKeyPrefix();
        this.expires = redisCaffeineCacheProperties.getRedis().getExpires();
        this.defaultExpiration = redisCaffeineCacheProperties.getRedis().getDefaultExpiration();
        this.topic = redisCaffeineCacheProperties.getRedis().getTopic();
        this.caffeineCache = caffeineCache;
        if (this.caffeineCache == null) {
            this.firstCache = false;
        }else{
            this.firstCache = true;
        }
    }


    /**
     * 执行底层查询操作
     * @param key
     * @return
     */
    @Override
    protected Object lookup(Object key) {
        //如果开启了一级缓存，则先从一级缓存中查询
        if(firstCache){
            Object value = caffeineCache.getIfPresent(key);
            if(null != value) {
                log.info("从caffeine中获取数据：{}",value);
                return value;
            }
        }
        String redisKey = getKey(key);
        Object value = redisTemplate.opsForValue().get(redisKey);
        if(null != value) {
            log.info("从redis中获取数据：{}",value);
            caffeineCache.put(key, value);
        }
        return value;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    /**
     * get cache value from  RedisCaffeineCache
     * @param key
     * @param valueLoader
     * @param <T>
     * @return
     */
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object value = lookup(key);
        if(null != value) {
            return (T) value;
        }
        fairLock.lock();
        try {
            value = lookup(key);
            if(null != value) {
                return (T) value;
            }
            value = valueLoader.call();
            Object storeValue = toStoreValue(value);
            put(key, storeValue);
            return (T) value;

        } catch (Exception e) {
            throw  new ValueRetrievalException(key, valueLoader, e);
        }finally {
            fairLock.unlock();
        }
    }

    /**
     * put value into RedisCaffeineCache
     * @param key
     * @param value
     */
    @Override
    public void put(Object key, Object value) {
        if (!super.isAllowNullValues() && ObjectUtils.isEmpty(value)) {
            this.evict(key);
            return;
        }

        long expire =  getExpire();
        if(expire > 0) {
            redisTemplate.opsForValue().set(getKey(key), toStoreValue(value), expire, TimeUnit.MILLISECONDS);
        }else {
            redisTemplate.opsForValue().set(getKey(key), toStoreValue(value));
        }

        if (firstCache) {
            //发送消息到消息队列
            push(new CacheMessage(name,key,uniqueKey));
            caffeineCache.put(key, value);
        }
    }

    /**
     * 如果不能存在put,存在返回oldValue
     * @param key
     * @param value
     * @return
     */
    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        String cacheKey = getKey(key);
        Object oldValue = null;
        synchronized (key) {
            oldValue = redisTemplate.opsForValue().get(cacheKey);
            if (ObjectUtils.isEmpty(oldValue)) {
                long expire = getExpire();
                if (expire > 0) {
                    redisTemplate.opsForValue().set(cacheKey, toStoreValue(value), expire, TimeUnit.MILLISECONDS);
                }else {
                    redisTemplate.opsForValue().set(cacheKey, toStoreValue(value));
                }
                if (firstCache) {
                    push(new CacheMessage(name,key,uniqueKey));
                    caffeineCache.put(key, value);
                }
            }
        }
        return toValueWrapper(oldValue);
    }

    @Override
    public void evict(Object key) {
        //删除redis中的缓存
        redisTemplate.delete(getKey(key));
        //删除一级缓存
        if (firstCache) {
            push(new CacheMessage(name,key,uniqueKey));
            caffeineCache.invalidate(key);
        }
    }

    @Override
    public void clear() {
        ScanOptions options = ScanOptions.scanOptions().match(this.name + ":*").build();
        Cursor<String> cursor = redisTemplate.scan(options);
        Set<String> keys = new HashSet<>();
        cursor.forEachRemaining(keys::add);
        redisTemplate.delete(keys);

        if (firstCache) {
            push(new CacheMessage(name,null,uniqueKey));
            caffeineCache.invalidateAll();
        }
    }
    /**
     * 重写fromStoreValue方法 ，避免获取NULLValue报错的问题
     * @param storeValue
     * @return
     */
    @Override
    protected Object fromStoreValue(Object storeValue) {
        if (super.isAllowNullValues() && (storeValue == NullValue.INSTANCE || storeValue instanceof NullValue)) {
            return null;
        }
        return storeValue;
    }

    /**
     * 获取redisKey缓存名称
     *
     * @return
     */
    private String getKey(Object key) {
        return name.concat(":").concat(StringUtils.isEmpty(cachePrefix) ? key.toString() : cachePrefix.concat(":").concat(key.toString()));
    }

    /**
     * 获取redis 缓存过期时间
     * @return
     */
    private long getExpire() {
        Long cacheNameExpire = expires.get(name);
        return cacheNameExpire == null ? defaultExpiration : cacheNameExpire;
    }

    public Cache<Object, Object> getLocalCache() {
        return caffeineCache;
    }

    /**
     * 通知其他缓存删除一级缓存
     * @param message
     */
    private void push(CacheMessage message) {
        redisTemplate.convertAndSend(topic, message);
    }

    /**
     * 清除多级缓存值
     * @param key
     */
    public void clearLocal(Object key) {
        if (!firstCache) {
            return;
        }
        if (key == null) {
            caffeineCache.invalidateAll();
        } else {
            caffeineCache.invalidate(key);
        }
    }

    public String getUniqueKey(){
        return this.uniqueKey;
    }
}
