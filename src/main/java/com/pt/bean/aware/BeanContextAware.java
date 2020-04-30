package com.pt.bean.aware;

import com.pt.bean.context.BeanContext;

/**
 * @Author :pt
 * @Date :Created in 15:49 2020/4/30
 */
public interface BeanContextAware extends Aware{


    /**
     * 设置BeanContext
     * @param beanContext
     */
    void setBeanContext(BeanContext beanContext);

}
