package com.pt.bean.context;

/**
 * @Author :pt
 * @Date :Created in 11:04 2020/4/30
 * Bean的上下文
 */
public interface BeanContext {


    /**
     * 获取Bean 通过bean 的名称
     * @param name
     * @return
     */
    Object getBean(String name);


    /**
     * <T> 是用来声明参数中用到的泛型，指定是一个泛型的方法
     * @param name
     * @param clazz
     * @param <T>
     * @return
     */
    <T> T getBean(String name,Class<T> clazz);
}
