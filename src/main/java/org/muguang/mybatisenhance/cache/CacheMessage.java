package org.muguang.mybatisenhance.cache;

import lombok.Data;

import java.io.Serializable;

@Data
public class CacheMessage implements Serializable {

    private static final long serialVersionUID = 9114171861634916597L;

    private String name;

    private Object key;

    private String redisCaffeineCacheUniqueKey;

    public CacheMessage() {
    }

    public CacheMessage(String name, Object key, String redisCaffeineCacheUniqueKey) {
        this.name = name;
        this.key = key;
        this.redisCaffeineCacheUniqueKey = redisCaffeineCacheUniqueKey;
    }

    public CacheMessage(String name, Object key) {
        this.name = name;
        this.key = key;
    }
}
