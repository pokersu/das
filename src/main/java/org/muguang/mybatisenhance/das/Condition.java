package org.muguang.mybatisenhance.das;

import lombok.Getter;

import java.util.Stack;

@Getter
public class Condition {
    public static Condition eq(String property, Object value) {
        return new Condition(property, ConditionType.EQ, value);
    }
    public static Condition neq(String property, Object value) {
        return new Condition(property, ConditionType.NEQ, value);
    }
    public static Condition gt(String property, Object value) {
        return new Condition(property, ConditionType.GT, value);
    }
    public static Condition lt(String property, Object value) {
        return new Condition(property, ConditionType.LT, value);
    }
    public static Condition gte(String property, Object value) {
        return new Condition(property, ConditionType.GTE, value);
    }
    public static Condition lte(String property, Object value) {
        return new Condition(property, ConditionType.LTE, value);
    }
    public static Condition like(String property, Object value) {
        value = "%" + value + "%";
        return new Condition(property, ConditionType.LIKE, value);
    }
    public static Condition like_r_fuzzy(String property, Object value) {
        value = value + "%";
        return new Condition(property, ConditionType.LIKE_R_FUZZY, value);
    }
    public static Condition like_l_fuzzy(String property, Object value) {
        value = "%" + value;
        return new Condition(property, ConditionType.LIKE_L_FUZZY, value);
    }
    public static Condition in(String property, Object... values) {
        return new Condition(property, ConditionType.IN, values);
    }
    public static Condition not_in(String property, Object... values) {
        return new Condition(property, ConditionType.NOTIN, values);
    }
    public static Condition is_null(String property, Object value) {
        return new Condition(property, ConditionType.ISNULL, value);
    }
    public static Condition is_not_null(String property, Object value) {
        return new Condition(property, ConditionType.IS_NOT_NULL, value);
    }
    public static Condition between(String property, Object... value) {
        return new Condition(property, ConditionType.BETWEEN, value);
    }

    private final String property;
    private final ConditionType type;
    private final Object[] values;

    public Condition(String property, ConditionType type, Object... values) {
        this.property = property;
        this.type = type;
        this.values = values;
    }
    private final Stack<Condition> or = new Stack<>();
    private final Stack<Condition> and = new Stack<>();


    public Condition and(Condition condition) {
        this.and.push(condition);
        return this;
    }

    public Condition or(Condition condition) {
        this.or.push(condition);
        return this;
    }
}
