package org.muguang.mybatisenhance.service.impl;

import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.*;
import org.muguang.mybatisenhance.das.Condition;
import org.muguang.mybatisenhance.das.Conditions;
import org.muguang.mybatisenhance.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
class UserServiceTest {

    static User USER;
    @Autowired
    private UserService userService;



    @Order(-1)
    @Test
    public void beforeAll() {
        List<User> users = userService.listAll();
        users.forEach(u->{
            userService.delete(u.getId());
        });
    }

    @Order(99999)
    @Test
    public void afterAll() {
        List<User> users = userService.listAll();
        users.forEach(u->{
            userService.delete(u.getId());
        });
    }

    @Order(1)
    @Test
    void testSave() {
        User user = new User();
        user.setName("jason");
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
        userService.save(user);

        Assertions.assertNotNull(user.getId());
        USER = user;
    }


    @Order(2)
    @Test
    void testFindById() {
        User user = userService.findById(USER.getId());
        Assertions.assertNotNull(user);
    }

    @Order(3)
    @Test
    void testFindByIds() {
        List<User> users = userService.findByIds(Set.of(USER.getId()));
        Assertions.assertNotNull(users);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals(USER.getId(), users.get(0).getId());
    }

    @Order(4)
    @Test
    void testFindOneByConditions() {
        Conditions conditions = new Conditions(Condition.eq("name", "jason").and(Condition.eq("status", 1)));
        User entity = userService.findOneByConditions(conditions);
        Assertions.assertNotNull(entity);
        Assertions.assertEquals(USER.getId(), entity.getId());
    }

    @Order(5)
    @Test
    void testFindOneByParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "jason");
        params.put("status", 1);
        User entity = userService.findOneByParams(params);
        Assertions.assertNotNull(entity);
        Assertions.assertEquals(USER.getId(), entity.getId());
    }

    @Order(6)
    @Test
    void testListByParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "jason");
        params.put("status", 1);
        List<User> users = userService.listByParams(params);
        Assertions.assertNotNull(users);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals(USER.getId(), users.get(0).getId());
    }

    @Order(7)
    @Test
    void testListByConditions() {
        Conditions conditions = new Conditions(Condition.eq("name", "jason").and(Condition.eq("status", 1)));
        List<User> users = userService.listByConditions(conditions);
        Assertions.assertNotNull(users);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals(USER.getId(), users.get(0).getId());
    }

    @Order(8)
    @Test
    void testListAll() {
        List<User> users = userService.listAll();
        Assertions.assertNotNull(users);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals(USER.getId(), users.get(0).getId());
    }

    @Order(9)
    @Test
    void testQueryPageData() {
        Conditions conditions = new Conditions(Condition.eq("name", "jason").and(Condition.eq("status", 1)));
        PageInfo<User> page = userService.queryPageData(conditions, 1, 10);
        Assertions.assertEquals(1, page.getPages());
        Assertions.assertEquals(1, page.getPageNum());
        Assertions.assertEquals(1, page.getTotal());
        Assertions.assertEquals(USER.getId(), page.getList().get(0).getId());
    }

    @Order(10)
    @Test
    void testUpdate() {
        USER.setName("updated");
        userService.update(USER);
    }

    @Order(11)
    @Test
    void testDelete() {
        userService.delete(USER.getId());
    }

//    @Order(12)
//    @Test
    void testInsertBatch() {
        List<User> users = IntStream.range(0, 10000).mapToObj(i -> {
            User user = new User();
            user.setName("jason" + i);
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
            return user;
        }).collect(Collectors.toList());


        userService.saveBatch(users);
    }
}