package org.muguang.mybatisenhance.das;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteEvents;
import org.apache.ignite.events.CacheEvent;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.lang.IgnitePredicate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class DasCacheManager implements CacheManager {

    public static final long MaximumSize = 100_000L;
    public static final int InitialCapacity = 10_000;
    public static final long ExpireAfterAccess = 6 * 60 * 60 * 1000L;

    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();
    private final Map<String, Long> expires = new HashMap<>();
    private final Ignite ignite;


    public DasCacheManager(Ignite ignite) {
        this.ignite = ignite;
//        IgniteEvents events = this.ignite.events();
//        IgnitePredicate<CacheEvent> filter =evt-> {
//            System.out.println("remote event:" + evt.name());
//            return true;
//        };

//        events.remoteListen((IgniteBiPredicate<UUID, CacheEvent>) (uuid, e)->{
//            String cacheName = e.cacheName();
//            Object key = e.key();
//            Cache cache = cacheMap.get(cacheName);
//            if (cache!=null){
//                ((LocalCache) cache).clearLocal(key);
//            }
//            return true;
//        }, filter, EventType.EVT_CACHE_OBJECT_PUT, EventType.EVT_CACHE_OBJECT_REMOVED, EventType.EVT_CACHE_OBJECT_EXPIRED);

    }


    private com.github.benmanes.caffeine.cache.AsyncCache<Object, Object> caffeineCache(String name) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        builder.initialCapacity(InitialCapacity);
        builder.maximumSize(MaximumSize);
        builder.expireAfterAccess(ExpireAfterAccess, TimeUnit.MILLISECONDS);
        Long expire = expires.get(name);
        if (expire!=null && expire>0){
            builder.expireAfterWrite(expire, TimeUnit.MILLISECONDS);
        }else{
            builder.expireAfterWrite(ExpireAfterAccess, TimeUnit.MILLISECONDS);
        }
        return builder.buildAsync();
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, n->{
            AsyncCache<Object, Object> caffeineCache = this.caffeineCache(n);
            IgniteCache<Object, Object> igniteCache =
                    this.ignite.getOrCreateCache(n).withExpiryPolicy(new AccessedExpiryPolicy(new Duration(TimeUnit.MILLISECONDS, ExpireAfterAccess)));
            return new LocalCache(n, igniteCache, caffeineCache, true);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return cacheMap.keySet();
    }
}
