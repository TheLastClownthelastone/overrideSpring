package com.pt.init;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.ClassScaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author :pt
 * @Date :Created in 9:59 2020/4/30
 * 初始化执行器,该类不能被继承
 * 该对象是单列的，在堆中只能含有一个该类型的对象，
 * 才用高并发下的懒汉模式进行单例
 */
public final class InitExecutor {

    private static Logger LOGGER= LoggerFactory.getLogger(InitExecutor.class);


    private InitExecutor(){}


    private static InitExecutor executor;

    private static Lock lock=new ReentrantLock();

    private AtomicBoolean flag=new AtomicBoolean(false);



    //使用原子化的boolean 进行判断,采用CAS
    private static AtomicBoolean atomicBoolean=new AtomicBoolean(false);



    private static   InitExecutor getInstance(){
        lock.lock();
        try {
            if(executor!=null){

            }else {
                executor=new InitExecutor();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return executor;
    }


    /**
     * 多线程该方法只能执行一次
     */
    public   void  init(){
        if(!flag.compareAndSet(false,true)){
            return;
        }
        try {
            //扫描实现了InitFunc的实现类(加载bean 和路由)
            Set<Class<?>> classSet = ClassScaner.scanPackageBySuper("", InitFunc.class);
            if(CollectionUtil.isNotEmpty(classSet)){
                List<OrderWrapper> initList=new ArrayList<>();
                for(Class<?> cls : classSet){
                    //如果当前的cls不是接口并且是InitFunc的实现类
                    if(!cls.isInterface()&& InitFunc.class.isAssignableFrom(cls)){
                        //获取cls的构造方法
                        Constructor<?> constructor = cls.getDeclaredConstructor();
                        //设置构造方法私有可以进行访问
                        constructor.setAccessible(true);
                        InitFunc initFunc = (InitFunc) constructor.newInstance();
                        LOGGER.info("[InitExecutor] found InitFunc: " + initFunc.getClass().getCanonicalName());
                        insertSorted(initList, initFunc);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 有序的插入到list中，设置OrderWrapper的 权重
     * @param list
     * @param func
     */
    private static void insertSorted(List<OrderWrapper> list,InitFunc func){
        int order = resolveOrder(func);
        
        int idx=0;
        for(;idx<list.size();idx++){
            if(list.get(idx).getOrder()>order){
                break;
            }
        }
    }

    /**
     * 获取类上的InitOrder注解中的value值
     */

    private static int resolveOrder(InitFunc func){
        //如果类上面没有加上InitOrder注解的，给它设置最小的优先加载权限
        if(!func.getClass().isAnnotationPresent(InitOrder.class)){
            //最小权限
            return InitOrder.LOWEST_PRECEDENCE;
        }else{
            return func.getClass().getAnnotation(InitOrder.class).value();
        }
    }






    /**
     * 用静态类部类，设置执行属性和InitFunc
     */
    private static class OrderWrapper{
        private final int order;
        private final InitFunc func;

        public OrderWrapper(int order, InitFunc func) {
            this.order = order;
            this.func = func;
        }
        int getOrder(){return order;}

        InitFunc getFunc(){return func;}
    }



}
