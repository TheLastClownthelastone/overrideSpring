package com.pt.bean.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author :pt
 * @Date :Created in 14:24 2020/4/30
 */
//作用范围 属性和方法上
@Target({ElementType.FIELD,ElementType.METHOD})
//当JVM在加载class文件中还存在该注解
//SOURCE 表示在编译成class文件的时候不存在，只在源文件中在
//CLASS 表示在class文件还存在当时被JVM加载的时候不存在
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {

    String name() default "";
}
