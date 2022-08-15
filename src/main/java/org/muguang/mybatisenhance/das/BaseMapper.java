package org.muguang.mybatisenhance.das;

import lombok.Getter;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.builder.annotation.ProviderContext;
import org.apache.ibatis.jdbc.SQL;
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.reflection.invoker.Invoker;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface BaseMapper<E, PK> {

    @InsertProvider(type = BaseSqlProvider.class, method = "insert")
    void insert(E e);

    @InsertProvider(type = BaseSqlProvider.class, method = "insertBatch")
    void insertBatch(@Param("entities") List<E> entities);

    @UpdateProvider(type = BaseSqlProvider.class, method = "update")
    void update(E e);

    @DeleteProvider(type = BaseSqlProvider.class, method = "delete")
    void delete(PK k);

    @SelectProvider(type = BaseSqlProvider.class, method = "findById")
    E findById(PK k);

    @SelectProvider(type = BaseSqlProvider.class, method = "findByIds")
    List<E> findByIds(@Param("ids") Set<PK> ids);

    @SelectProvider(type = BaseSqlProvider.class, method = "findOneByConditions")
    E findOneByConditions(@Param("conditions") Conditions conditions);

    @SelectProvider(type = BaseSqlProvider.class, method = "findOneByParams")
    E findOneByParams(Map<String, Object> params);

    @SelectProvider(type = BaseSqlProvider.class, method = "findByProperty")
    List<E> findByProperty(String property, Object value);

    @SelectProvider(type = BaseSqlProvider.class, method = "listAll")
    List<E> listAll();

    @SelectProvider(type = BaseSqlProvider.class, method = "listByConditions")
    List<E> listByConditions(@Param("conditions") Conditions conditions);

    @SelectProvider(type = BaseSqlProvider.class, method = "listByParams")
    List<E> listByParams(Map<String, Object> params);



    default void warmUp() {
        BaseSqlProvider.getClassMetaData(BaseSqlProvider.getGenericClass(this.getClass()));
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
                
                this.pkProp = Stream.of(this.fields).filter(f -> f.isAnnotationPresent(Id.class))
                        .findFirst()
                        .map(Field::getName);
                this.pkColumn = this.pkProp.map(ClassMetadata::camelToUnderline);
                this.pkSeq = Stream.of(this.fields).filter(f -> f.isAnnotationPresent(Id.class))
                        .map(f->f.getAnnotation(Id.class))
                        .map(Id::seq)
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


        public String insertBatch(List<Object> entities, ProviderContext context){
            ClassMetadata metaData = getClassMetaData(getGenericClass(context.getMapperType()));

            metaData.getPkProp().ifPresent(pk->{
                Invoker pkGetter = metaData.getReflector().getGetInvoker(pk);
                entities.forEach(entity->{
                    try {
                        if (pkGetter.invoke(entity, null)!=null){
                            return;
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    String seq = metaData.getPkSeq().orElseThrow(() -> new IllegalArgumentException("primary key must be sequence"));
                    try {
                        Object pkVal = PostgresIdGenerator.nextId(seq);
                        metaData.getReflector().getSetInvoker(pk).invoke(entity, new Object[]{pkVal});
                    }catch (Exception e){
                        throw new RuntimeException(e);
                    }
                });
            });
            String[] propertiesQuoted = Stream.of(metaData.getProps()).map(f -> "#{item." + f + "}").toArray(String[]::new);
            String[] columns = metaData.getColumns();
            String tableName = metaData.getTableName();
            String batchInsertValues = " values <foreach item='item' collection='entities' open='' separator=',' close=''>(" + String.join(",", propertiesQuoted) + ")</foreach>";
            return "<script>" + new SQL().INSERT_INTO(tableName).INTO_COLUMNS(columns).toString() + batchInsertValues + "</script>";
        }
        public String insert(Object entity, ProviderContext context){
            Class<?> mapper = context.getMapperType();
            final String sqlKey = mapper.getName() + ".insert";

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
                    Object pkVal = PostgresIdGenerator.nextId(seq);
                    metaData.getReflector().getSetInvoker(pk).invoke(entity, new Object[]{pkVal});
                }catch (Exception e){
                    throw new RuntimeException(e);
                }
            });

            return SQL_CACHE.computeIfAbsent(sqlKey, k -> {
                String[] propertiesQuoted = Stream.of(metaData.getProps()).map(f -> "#{" + f + "}").toArray(String[]::new);
                String[] columns = metaData.getColumns();
                String tableName = metaData.getTableName();

                return new SQL().INSERT_INTO(tableName).INTO_COLUMNS(columns).INTO_VALUES(propertiesQuoted).toString();
            });
        }

        public String update(ProviderContext context){
            Class<?> mapper = context.getMapperType();
            final String sqlKey = mapper.getName() + ".update";
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

        public String delete(ProviderContext context){
            Class<?> mapper = context.getMapperType();
            final String sqlKey = mapper.getName() + ".delete";
            return SQL_CACHE.computeIfAbsent(sqlKey, k->{
                ClassMetadata metaData = getClassMetaData(getGenericClass(context.getMapperType()));
                String tableName = metaData.getTableName();
                String primaryKey = metaData.getPkColumn().orElseThrow(() -> new IllegalArgumentException("can not find pk field"));
                return new SQL().DELETE_FROM(tableName).WHERE(primaryKey + " = #{pk}").toString();
            });
        }


        public String findOneByConditions(Conditions conditions, ProviderContext context){
            ClassMetadata metaData = getClassMetaData(getGenericClass(context.getMapperType()));
            String tableName = metaData.getTableName();
            String[] columns = metaData.getColumns();
            String whereClause = buildWhereClause(metaData, conditions.getRoot());
            String orderClause = buildOrderClause(metaData, conditions.getOrders());
            return "<script>" + new SQL().SELECT(columns).FROM(tableName).WHERE(whereClause).toString() + " " + orderClause + " </script>";
        }

        public String findOneByParams(Map<String, Object> params, ProviderContext context) {
            ClassMetadata metaData = getClassMetaData(getGenericClass(context.getMapperType()));
            String tableName = metaData.getTableName();
            String[] columns = metaData.getColumns();
            String whereClause = buildWhereClause(metaData, params);
            return "<script>" + new SQL().SELECT(columns).FROM(tableName).WHERE(whereClause).toString() + " </script>";
        }



        public String findById(ProviderContext context){
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

        public String findByIds(ProviderContext context) {
            ClassMetadata metaData = getClassMetaData(getGenericClass(context.getMapperType()));
            String tableName = metaData.getTableName();

            String primaryKey = metaData.getPkColumn().orElseThrow(() -> new IllegalArgumentException("can not find pk field"));
            String[] columns = metaData.getColumns();
            return "<script>" + new SQL().SELECT(columns).FROM(tableName).WHERE(primaryKey + " in <foreach item='item' collection='ids' open='(' separator=',' close=')'>#{item}</foreach>").toString() + "</script>";
        }

        public String findByProperty(String property, Object value, ProviderContext context) {
            ClassMetadata metaData = getClassMetaData(getGenericClass(context.getMapperType()));
            String tableName = metaData.getTableName();
            String[] columns = metaData.getColumns();
            Map<String, String> propColumns = metaData.getPropColumn();
            if (!propColumns.containsKey(property)) {
                throw new IllegalArgumentException("can not find property " + property);
            }
            String column = propColumns.get(property);
            String whereClause;
            if (value == null) {
                whereClause = column + " is null";
            }else{
                whereClause = column + " = #{" + value + "}";
            }
            return "<script>" + new SQL().SELECT(columns).FROM(tableName).WHERE(whereClause).toString() + "</script>";
        }

        public String listAll(ProviderContext context){
            Class<?> mapper = context.getMapperType();
            final String sqlKey = mapper.getName() + ".listAll";
            return SQL_CACHE.computeIfAbsent(sqlKey, k -> {
                ClassMetadata metaData = getClassMetaData(getGenericClass(context.getMapperType()));
                String tableName = metaData.getTableName();
                String[] columns = metaData.getColumns();
                return new SQL().SELECT(columns).FROM(tableName).toString();
            });
        }


        public String listByConditions(Conditions conditions, ProviderContext context){
            ClassMetadata metaData = getClassMetaData(getGenericClass(context.getMapperType()));
            String tableName = metaData.getTableName();
            String[] columns = metaData.getColumns();
            String whereClause = buildWhereClause(metaData, conditions.getRoot());
            String orderClause = buildOrderClause(metaData, conditions.getOrders());
            return "<script>" + new SQL().SELECT(columns).FROM(tableName).WHERE(whereClause).toString() + " " + orderClause + " </script>";
        }

        public String listByParams(Map<String, Object> params, ProviderContext context) {
            ClassMetadata metaData = getClassMetaData(getGenericClass(context.getMapperType()));
            String tableName = metaData.getTableName();
            String[] columns = metaData.getColumns();
            String whereClause = buildWhereClause(metaData, params);
            return "<script>" + new SQL().SELECT(columns).FROM(tableName).WHERE(whereClause).toString() + " </script>";
        }

        private String buildOrderClause(ClassMetadata metaData, String[] orders) {
            if (orders==null || orders.length==0) {
                return "";
            }
            Map<String, String> propColumns = metaData.getPropColumn();
            return Arrays.stream(orders).map(String::trim).map(f -> {
                String[] split = f.split(" ");
                String prop = split[0];
                String order = split[1];
                if (!propColumns.containsKey(prop)) {
                    throw new IllegalArgumentException("can not find property " + prop);
                }
                return propColumns.get(prop) + " " + order;
            }).collect(Collectors.joining(","));
        }


        public static String buildWhereClause(ClassMetadata metadata, Map<String, Object> params){
            Map<String, String> propColumns = metadata.getPropColumn();
            return params.entrySet().stream().map(e->{
                String prop = e.getKey();
                Object val = e.getValue();
                if (!propColumns.containsKey(prop)) {
                    throw new IllegalArgumentException("can not find property " + prop);
                }
                String column = propColumns.get(prop);
                if (val==null){
                    return column + " is null";
                }else{
                    return column + " = #{" + prop + "}";
                }
            }).collect(Collectors.joining(" and "));
        }
        public static String buildWhereClause(ClassMetadata metadata, Condition condition){
            if (condition == null){
                return "1=1";
            }
            if (!condition.getOr().isEmpty()){
                Condition right = condition.getOr().pop();
                return "( "+buildWhereClause(metadata, condition)+" or "+buildWhereClause(metadata, right)+" )";
            }
            if (!condition.getAnd().isEmpty()){
                Condition right = condition.getAnd().pop();
                return "( "+buildWhereClause(metadata, condition)+" and "+buildWhereClause(metadata, right)+" )";
            }

            Map<String, String> propColumns = metadata.getPropColumn();
            String property = condition.getProperty();
            String column = propColumns.get(property);
            String operator = null;
            String clause = null;
            switch (condition.getType()){
                case EQ: operator = " = "; break;
                case NEQ: operator = " != "; break;
                case GT: operator = " > "; break;
                case LT: operator = " < "; break;
                case GTE: operator = " >= "; break;
                case LTE: operator = " <= "; break;
                case LIKE:
                case LIKE_L_FUZZY:
                case LIKE_R_FUZZY: operator = " like "; break;
                case BETWEEN: clause = column + " between #{conditions.params." + property + "_L} and #{conditions.params." + property + "_R}" ; break;
                case IN: clause = column + " in <foreach item='item' collection='conditions.params."+property+"' open='(' separator=',' close=')'>#{item}</foreach>"; break;
                case NOTIN: clause = column + " not in <foreach item='item' collection='conditions.params."+property+"' open='(' separator=',' close=')'>#{item}</foreach>"; break;
                case ISNULL: clause = column + " is null"; break;
                case IS_NOT_NULL: clause = column + " is not null"; break;
            }
            if (clause!=null) return clause;
            if (operator == null) throw new IllegalArgumentException("unknown operator");
            return column + operator + "#{conditions.params." +property + "}";
        }

        private static Class<?> getGenericClass(Class<?> clazz) {
            Type[] genericInterfaces = clazz.getGenericInterfaces();
            return (Class<?>) ((ParameterizedType) genericInterfaces[0]).getActualTypeArguments()[0];
        }
    }
}
