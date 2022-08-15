package org.muguang.mybatisenhance.das;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PostgresIdGenerator implements ApplicationContextAware {


    private static ApplicationContext ctx;
    private static final ConcurrentMap<String, AtomicLong> Limitation = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, AtomicLong> CurrentVal = new ConcurrentHashMap<>();
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        ctx = context;
    }

    public static Long nextId(String seq) throws SQLException {

        AtomicLong curVal = CurrentVal.get(seq);
        AtomicLong limitVal = Limitation.get(seq);

        if (curVal==null || limitVal==null){
            CurrentVal.put(seq, new AtomicLong(getIdFromSequence(seq)));
            Limitation.put(seq, new AtomicLong(CurrentVal.get(seq).get()));
            return CurrentVal.get(seq).get();
        }
        for (;;){
            long oldLimitValue = limitVal.get();
            long curValue = CurrentVal.get(seq).get();
            if (curValue<oldLimitValue || limitVal.compareAndSet(oldLimitValue, getIdsFromSequence(seq, 20))){
                break;
            }
        }

        if (!(curVal.longValue() < limitVal.longValue())) {
            throw new SQLException("id limit exceeded");
        }
        return curVal.incrementAndGet();
    }

    private static Long getIdFromSequence(String seq) throws SQLException {
        SqlSessionFactory factory = ctx.getBean(SqlSessionFactory.class);
        SqlSession sqlSession = SqlSessionUtils.getSqlSession(factory);
        Connection conn = sqlSession.getConnection();
        ResultSet rs = conn.prepareStatement("select nextval('" + seq + "'::regclass)").executeQuery();
        rs.next();
        return rs.getLong(1);
    }

    private static Long getIdsFromSequence(String seq, int count) throws SQLException {
        SqlSessionFactory factory = ctx.getBean(SqlSessionFactory.class);
        SqlSession sqlSession = SqlSessionUtils.getSqlSession(factory);
        Connection conn = sqlSession.getConnection();

        ResultSet rs = conn.prepareStatement("select setval('" + seq + "'::regclass, (SELECT last_value+" + count + " FROM " + seq + "))").executeQuery();
        rs.next();
        return rs.getLong(1);
    }
}
