package com.me.guanpj.jdatabase.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Jie on 2017/4/17.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Column {
    boolean id() default false;
    String name() default "";
    ColumnType type() default ColumnType.UNKNOWN;
    boolean autofresh() default false;
    public enum ColumnType{
        TONE, TMANY, SERIALIZABLE, UNKNOWN
    }
}
