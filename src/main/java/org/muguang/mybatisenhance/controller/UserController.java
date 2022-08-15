package org.muguang.mybatisenhance.controller;

import com.github.pagehelper.PageInfo;
import org.muguang.mybatisenhance.das.Conditions;
import org.muguang.mybatisenhance.entity.User;
import org.muguang.mybatisenhance.service.impl.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;


@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService service;

    @GetMapping("/{id}")
    public User findById(@PathVariable("id") Long id) {
        return service.findById(id);
    }

    @GetMapping("/save")
    public User save() {
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
        service.save(user);
        return user;
    }

    @GetMapping("/page")
    public PageInfo<User> page(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                               @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return service.queryPageData(new Conditions(null), pageNum, pageSize);
    }

}
