package com.pt.init;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author :pt
 * @Date :Created in 10:53 2020/4/30
 * 初始化器，用来区分bean的路由的加载顺序
 */
@Retention(RetentionPolicy.RUNTIME)
//作用在类上
@Target({ElementType.TYPE})
public @interface InitOrder {
    /**
     * 最低优先级
     */
    int LOWEST_PRECEDENCE=Integer.MAX_VALUE;

    /**
     * 最高优先级别
     */
    int HIGHEST_PRECEDENCE=Integer.MIN_VALUE;


    /**
     * 默认的最低有线加载级别
     * @return
     */
    int value() default LOWEST_PRECEDENCE;



}
