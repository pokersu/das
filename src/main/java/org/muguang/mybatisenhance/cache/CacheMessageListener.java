package org.muguang.mybatisenhance.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
public class CacheMessageListener implements MessageListener {

    private RedisTemplate<String, Object> redisTemplate;

    private RedisCaffeineCacheManager redisCaffeineCacheManager;

    public CacheMessageListener(RedisTemplate<String, Object> redisTemplate, RedisCaffeineCacheManager redisCaffeineCacheManager) {
        super();
        this.redisTemplate = redisTemplate;
        this.redisCaffeineCacheManager = redisCaffeineCacheManager;
    }


    @Override
    public void onMessage(Message message, byte[] pattern) {
        CacheMessage cacheMessage = (CacheMessage) redisTemplate.getValueSerializer().deserialize(message.getBody());
        log.debug("receive a redis topic message, clear local cache, the cacheName is {}, the key is {},the uniqueKey is {}", cacheMessage.getName(), cacheMessage.getKey(), cacheMessage.getRedisCaffeineCacheUniqueKey());
        redisCaffeineCacheManager.clearLocal(cacheMessage.getName(), cacheMessage.getKey(), cacheMessage.getRedisCaffeineCacheUniqueKey());
    }
}
