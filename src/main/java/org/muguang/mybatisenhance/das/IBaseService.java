package org.muguang.mybatisenhance.das;

import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IBaseService<T> {

    T save(T t);
    List<T> saveBatch(List<T> list);
    T update(T t);
    void delete(Long id);
    T findById(Long id);
    List<T> findByIds(Set<Long> ids);

    T findOneByConditions(Conditions conditions);
    T findOneByParams(Map<String, Object> params);
    List<T> listByParams(Map<String, Object> params);
    List<T> listByConditions(Conditions conditions);
    List<T> listAll();

    PageInfo<T> queryPageData(Conditions conditions, int pageNum, int pageSize);
}
