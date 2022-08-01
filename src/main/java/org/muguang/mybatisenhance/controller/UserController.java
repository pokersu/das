package org.muguang.mybatisenhance.controller;

import com.github.pagehelper.PageInfo;
import org.muguang.mybatisenhance.entity.User;
import org.muguang.mybatisenhance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService service;

    @GetMapping("/{id}")
    public User findById(@PathVariable("id") Long id) {
        return service.findById(id);
    }
}
