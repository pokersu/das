package org.muguang.mybatisenhance.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@EnableConfigurationProperties(RedisCaffeineCacheProperties.class)
@EnableCaching
public class RedisCaffeineCacheAutoConfiguration {
    @Autowired
    private RedisCaffeineCacheProperties redisCaffeineCacheProperties;

    @Bean
    public RedisCaffeineCacheManager cacheManager(RedisTemplate<String, Object> redisTemplate) {
        return new RedisCaffeineCacheManager(redisTemplate, redisCaffeineCacheProperties);
    }

    @Bean
    @ConditionalOnBean(name = {"redisTemplate", "cacheManager"})
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisTemplate<String, Object> redisTemplate, RedisCaffeineCacheManager redisCaffeineCacheManager) {
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(redisTemplate.getConnectionFactory());
        CacheMessageListener cacheMessageListener = new CacheMessageListener(redisTemplate, redisCaffeineCacheManager);
        redisMessageListenerContainer.addMessageListener(cacheMessageListener, new ChannelTopic(redisCaffeineCacheProperties.getRedis().getTopic()));
        return redisMessageListenerContainer;
    }
}
