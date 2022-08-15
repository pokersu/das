package org.muguang.mybatisenhance;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@MapperScan("org.muguang.mybatisenhance.mapper")
@SpringBootApplication
public class DasApplication {

    public static void main(String[] args) {
        SpringApplication.run(DasApplication.class, args);
    }

}
