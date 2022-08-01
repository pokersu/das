package org.muguang.mybatisenhance.config;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class CacheInvalidEvent implements Serializable {
    private static final long serialVersionUID = -6674325754880814060L;

    private String instanceKey;
    private String name;
    private Object key;

}
