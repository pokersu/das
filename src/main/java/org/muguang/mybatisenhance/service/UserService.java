package org.muguang.mybatisenhance.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.muguang.mybatisenhance.entity.User;
import org.muguang.mybatisenhance.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@Transactional
public class UserService {

    private final AtomicLong counter = new AtomicLong(0);
    @Autowired
    private UserMapper userMapper;


    public void insert(User user) {
        userMapper.insert(user);
    }

    public void updateById(User user) {
        userMapper.updateById(user);
    }

    public void deleteById(Long id) {
        userMapper.deleteById(id);
    }
    @Cacheable(value = "user",key = "#id")
    public User findById(Long id) {
        log.info("start findById counter: {}", counter.getAndIncrement());
        return userMapper.findById(id);
    }

    public List<User> findAll() {
        return userMapper.findAll();
    }

    public PageInfo<User> page(int pageNum, int pageSize) {
        log.info("start page pageNum: {}, pageSize: {}", pageNum, pageSize);
        PageHelper.startPage(pageNum, pageSize);
        List<User> users = userMapper.findAll();
        return new PageInfo<>(users);
    }
}
