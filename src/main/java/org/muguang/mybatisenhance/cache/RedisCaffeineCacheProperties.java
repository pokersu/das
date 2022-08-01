package org.muguang.mybatisenhance.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
@Component
@ConfigurationProperties(prefix = "model.cache.multi")
public class RedisCaffeineCacheProperties {
    /**
     * 静态缓存名称
     */
    private Set<String> cacheNames = new HashSet<>();

    /**
     * 一级缓存名称
     */
    private Set<String> firstCacheNames = new HashSet<>();

    /**
     * 是否存储空值，默认true
     */
    private boolean storeNullValues = true;

    /**
     * 缓存redis key的前缀
     */
    private String redisKeyPrefix = "";
    private Redis redis = new Redis();
    private Caffeine caffeine = new Caffeine();

    @Data
    public class Redis {

        //设置全局过期时间，默认8天
        private long defaultExpiration = 60 * 60 * 24 * 8;

        //缓存更新时通知其他节点的topic名称
        private String topic = "cache:redis:caffeine:topic";

        //每个cacheName的过期时间，单位毫秒，优先级比defaultExpiration高
        private Map<String, Long> expires = new HashMap<>();

    }

    @Data
    public class Caffeine {

        //访问后过期时间，单位毫秒
        private long expireAfterAccess;

        //写入后过期时间，单位毫秒
        private long expireAfterWrite;

        //写入后刷新时间，单位毫秒
        private long refreshAfterWrite;

        //设置最大缓存对象个数，超过此数量时之前放入的缓存将失效
        private int maximumSize;

        //初始化大小
        private int initialCapacity;

        //每个cacheName的写入后过期时间(expireAfterWrite)，找不到就用上面定义的配置。单位毫秒
        private Map<String, Long> expires = new HashMap<>();
    }
}
