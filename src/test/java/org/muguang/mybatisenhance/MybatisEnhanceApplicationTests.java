package org.muguang.mybatisenhance;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.muguang.mybatisenhance.entity.User;
import org.muguang.mybatisenhance.mapper.UserMapper;
import org.muguang.mybatisenhance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.List;

@SpringBootTest
class MybatisEnhanceApplicationTests {

    @Autowired
    private UserMapper mapper;
    @Test
    void testInsert() {
        User user = new User();
        user.setName("jason1");
        user.setPassword("123456");
        user.setEmail("jason@gmail.com");
        user.setPhone("13800138000");
        user.setAddress("beijing");
        user.setRemark("remark");
        user.setStatus(1);
        user.setLastLoginIp("127.0.0.1");
        user.setLastLoginTime(new Date());
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setDeleteTime(new Date());
        user.setIsDeleted(0);
        mapper.insert(user);

        User entity = mapper.findById(user.getId());
        Assertions.assertNotNull(entity);
    }

    @Test
    void testFindAll() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            mapper.findAll();
        }
        long end = System.currentTimeMillis();
        System.out.println("findAll avg cost time: " + (end - start)/1000.0 + "ms");
    }

    @Test
    void testDeleteById() {
        mapper.deleteById(11L);
        List<User> all = mapper.findAll();
        System.out.println(all);
    }
}
