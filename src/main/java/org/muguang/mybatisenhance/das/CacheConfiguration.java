package org.muguang.mybatisenhance.das;

import org.apache.ignite.Ignite;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfiguration {

    @Bean
    public DasCacheManager cacheManager(Ignite ignite) {
        return new DasCacheManager(ignite);
    }
}
