package org.muguang.mybatisenhance.das;

import lombok.Getter;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.builder.annotation.ProviderContext;
import org.apache.ibatis.jdbc.SQL;
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public interface BaseMapper<E, Pk> {

    @InsertProvider(type = BaseSqlProvider.class, method = "insert")
    void insert(E e);

    @UpdateProvider(type = BaseSqlProvider.class, method = "updateById")
    void updateById(E e);

    @DeleteProvider(type = BaseSqlProvider.class, method = "deleteById")
    void deleteById(Pk k);

    @SelectProvider(type = BaseSqlProvider.class, method = "findById")
    E findById(Pk k);

    @SelectProvider(type = BaseSqlProvider.class, method = "findAll")
    List<E> findAll();

    default void warmUp() {
        BaseSqlProvider.getClassMetaData(BaseSqlProvider.getGenericClass(this.getClass()));
    }

    @Component
    class IdGen implements ApplicationContextAware{

        private static ApplicationContext ctx;
        @Override
        public void setApplicationContext(ApplicationContext context) throws BeansException {
            ctx = context;
        }

        public static Object getIdFromSequence(String seq) throws SQLException {
            SqlSessionFactory factory = ctx.getBean(SqlSessionFactory.class);
            SqlSession sqlSession = SqlSessionUtils.getSqlSession(factory);
            Connection conn = sqlSession.getConnection();
            ResultSet rs = conn.prepareStatement("select nextval('" + seq + "'::regclass)").executeQuery();
            rs.next();
            return rs.getObject(1);
        }
    }

    class BaseSqlProvider {

        private static final ConcurrentMap<Class<?>, ClassMetadata> REFLECT_CACHE = new ConcurrentHashMap<>();
        private static final ConcurrentMap<String, String> SQL_CACHE = new ConcurrentHashMap<>();


        @Getter
        static class ClassMetadata {

            private static final String CAMEL_TO_UNDERLINE = "([a-z])([A-Z])";
            private static final String CAMEL_TO_UNDERLINE_REPLACE_PATTERN = "$1_$2";
            private final String tableName;
            private final Optional<String> pkProp;
            private final Optional<String> pkColumn;
            private final Optional<String> pkSeq;
            private final Field[] fields;
            private final String[] props;
            private final String[] columns;
            private final Reflector reflector;

            private final Map<String, String> columnProp = new HashMap<>();

            private final Map<String, String> propColumn = new HashMap<>();

            public ClassMetadata(Class<?> clazz){
                this.tableName = getTableName(clazz);
                this.reflector = new Reflector(clazz);
                this.fields = Stream.of(clazz.getDeclaredFields())
                        .filter(f -> !Modifier.isTransient(f.getModifiers()))
                        .toArray(Field[]::new);
                this.props = Stream.of(this.fields).map(Field::getName).toArray(String[]::new);
                
                this.pkProp = Stream.of(this.fields).filter(f -> f.isAnnotationPresent(org.muguang.mybatisenhance.das.Pk.class))
                        .findFirst()
                        .map(Field::getName);
                this.pkColumn = this.pkProp.map(ClassMetadata::camelToUnderline);
                this.pkSeq = Stream.of(this.fields).filter(f -> f.isAnnotationPresent(org.muguang.mybatisenhance.das.Pk.class))
                        .map(f->f.getAnnotation(org.muguang.mybatisenhance.das.Pk.class))
                        .map(org.muguang.mybatisenhance.das.Pk::seq)
                        .findFirst();
                this.columns = Stream.of(this.fields)
                        .map(f -> {
                            String col;
                            if (f.isAnnotationPresent(Col.class)){
                                col = f.getAnnotation(Col.class).value();
                            } else {
                                col = camelToUnderline(f.getName());
                            }
                            this.columnProp.put(col, f.getName());
                            this.propColumn.put(f.getName(), col);
                            return col;
                        })
                        .toArray(String[]::new);
            }


            private static String getTableName(Class<?> clazz){
                if (clazz.isAnnotationPresent(Tbl.class)){
                    Tbl table = clazz.getAnnotation(Tbl.class);
                    return table.value();
                }else{
                    String clazzName = clazz.getSimpleName();
                    return ClassMetadata.camelToUnderline(clazzName);
                }
            }

            private static String camelToUnderline(String str) {
                return str.replaceAll(CAMEL_TO_UNDERLINE, CAMEL_TO_UNDERLINE_REPLACE_PATTERN).toLowerCase();
            }
        }

        private static ClassMetadata getClassMetaData(Class<?> clazz) {
            return REFLECT_CACHE.computeIfAbsent(clazz, k -> new ClassMetadata(clazz));
        }


        public String insert(Object entity, ProviderContext context){
            Class<?> mapper = context.getMapperType();
            final String sqlKey = mapper.getName() + ".insert";

            return SQL_CACHE.computeIfAbsent(sqlKey, k -> {
                ClassMetadata metaData = getClassMetaData(getGenericClass(context.getMapperType()));

                metaData.getPkProp().ifPresent(pk->{
                    Invoker pkGetter = metaData.getReflector().getGetInvoker(pk);
                    try {
                        if (pkGetter.invoke(entity, null)!=null){
                            return;
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    String seq = metaData.getPkSeq().orElseThrow(() -> new IllegalArgumentException("primary key must be sequence"));
                    try {
                        Object pkVal = IdGen.getIdFromSequence(seq);
                        metaData.getReflector().getSetInvoker(pk).invoke(entity, new Object[]{pkVal});
                    }catch (Exception e){
                        throw new RuntimeException(e);
                    }
                });
                String[] propertiesQuoted = Stream.of(metaData.getProps()).map(f -> "#{" + f + "}").toArray(String[]::new);
                String[] columns = metaData.getColumns();
                String tableName = metaData.getTableName();
                return new SQL().INSERT_INTO(tableName).INTO_COLUMNS(columns).INTO_VALUES(propertiesQuoted).toString();
            });
        }

        public String updateById(Object entity, ProviderContext context){
            Class<?> mapper = context.getMapperType();
            final String sqlKey = mapper.getName() + ".updateById";
            return SQL_CACHE.computeIfAbsent(sqlKey, k -> {
                ClassMetadata metaData = getClassMetaData(getGenericClass(context.getMapperType()));

                String primaryKey = metaData.getPkColumn().orElseThrow(() -> new IllegalArgumentException("can not find pk field"));
                String pkPropertyName = metaData.getPkProp().orElseThrow(() -> new IllegalArgumentException("can not find pk field"));
                String[] paramBundles = Stream.of(metaData.getColumns())
                        .filter(f -> !f.equals(primaryKey))
                        .map(c -> c + " = #{" + metaData.getColumnProp().get(c) + "}")
                        .toArray(String[]::new);
                String tableName = metaData.getTableName();
                return new SQL().UPDATE(tableName).SET(paramBundles)
                        .WHERE(primaryKey + " = #{" + pkPropertyName + "}").toString();
            });
        }

        public String deleteById(Object pk, ProviderContext context){
            Class<?> mapper = context.getMapperType();
            final String sqlKey = mapper.getName() + ".deleteById";
            ClassMetadata metaData = getClassMetaData(getGenericClass(context.getMapperType()));
            String tableName = metaData.getTableName();
            String primaryKey = metaData.getPkColumn().orElseThrow(() -> new IllegalArgumentException("can not find pk field"));
            return new SQL().DELETE_FROM(tableName).WHERE(primaryKey + " = #{pk}").toString();
        }


        public String findById(Object pk, ProviderContext context){
            Class<?> mapper = context.getMapperType();
            final String sqlKey = mapper.getName() + ".findById";
            return SQL_CACHE.computeIfAbsent(sqlKey, k -> {
                ClassMetadata metaData = getClassMetaData(getGenericClass(context.getMapperType()));
                String tableName = metaData.getTableName();

                String primaryKey = metaData.getPkColumn().orElseThrow(() -> new IllegalArgumentException("can not find pk field"));
                String[] columns = metaData.getColumns();
                return new SQL().SELECT(columns).FROM(tableName).WHERE(primaryKey + " = #{pk}").toString();
            });
        }

        public String findAll(ProviderContext context){
            Class<?> mapper = context.getMapperType();
            final String sqlKey = mapper.getName() + ".findAll";
            return SQL_CACHE.computeIfAbsent(sqlKey, k -> {
                ClassMetadata metaData = getClassMetaData(getGenericClass(context.getMapperType()));
                String tableName = metaData.getTableName();
                String[] columns = metaData.getColumns();
                return new SQL().SELECT(columns).FROM(tableName).toString();
            });
        }

        private static Class<?> getGenericClass(Class<?> clazz) {
            Type[] genericInterfaces = clazz.getGenericInterfaces();
            return (Class<?>) ((ParameterizedType) genericInterfaces[0]).getActualTypeArguments()[0];
        }
    }
}
