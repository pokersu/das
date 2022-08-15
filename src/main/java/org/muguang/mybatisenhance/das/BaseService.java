package org.muguang.mybatisenhance.das;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseService<T> implements IBaseService<T> {

    @Autowired
    protected BaseMapper<T, Long> mapper;

    @Override
    public T save(T t) {
        mapper.insert(t);
        return t;
    }


    @Override
    public List<T> saveBatch(List<T> list) {
        mapper.insertBatch(list);
        return list;
    }

    @Override
    public T update(T t) {
        mapper.update(t);
        return t;
    }

    @Override
    public void delete(Long id) {
        mapper.delete(id);
    }

    @Override
    public T findById(Long id) {
        return mapper.findById(id);
    }

    @Override
    public List<T> findByIds(Set<Long> ids) {
        return mapper.findByIds(ids);
    }

    @Override
    public T findOneByConditions(Conditions conditions) {
        return mapper.findOneByConditions(conditions);
    }

    @Override
    public T findOneByParams(Map<String, Object> params) {
        return mapper.findOneByParams(params);
    }

    @Override
    public List<T> listByParams(Map<String, Object> params) {
        return mapper.listByParams(params);
    }

    @Override
    public List<T> listByConditions(Conditions conditions) {
        return mapper.listByConditions(conditions);
    }

    @Override
    public List<T> listAll() {
        return mapper.listAll();
    }

    @Override
    public PageInfo<T> queryPageData(Conditions conditions, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<T> data = mapper.listByConditions(conditions);
        return new PageInfo<>(data);
    }
}
