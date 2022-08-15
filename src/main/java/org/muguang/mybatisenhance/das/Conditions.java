package org.muguang.mybatisenhance.das;


import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

@Getter
public class Conditions {


    private final Map<String, Object> params = new HashMap<>();
    private final Condition root;
    private String[] orders;

    public Conditions(Condition root) {
        this.root = root;
        fillParams(root);
    }

    public Conditions(Condition root, String... orders) {
        this.root = root;
        this.orders = orders;
        fillParams(root);
    }

    private void fillParams(final Condition cond){
        if (cond==null) return;

        Stack<Condition> and = cond.getAnd();
        Stack<Condition> or = cond.getOr();
        for (Condition condition : and) {
            fillParams(condition);
        }
        for (Condition condition : or) {
            fillParams(condition);
        }

        ConditionType type = cond.getType();
        String property = cond.getProperty();
        Object[] values = cond.getValues();

        if (type == ConditionType.BETWEEN) {
            params.put(property + "_L", values[0]);
            params.put(property + "_R", values[1]);
        } else {
            if (values.length==1){
                params.put(property, values[0]);
            } else {
                params.put(property, values);
            }
        }
    }

}
