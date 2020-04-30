package com.pt.bean.context;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.ClassScaner;
import cn.hutool.core.util.StrUtil;
import com.pt.bean.annotation.Autowired;
import com.pt.bean.annotation.Bean;
import com.pt.bean.aware.BeanContextAware;
import com.pt.init.InitFunc;
import com.pt.init.InitOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author :pt
 * @Date :Created in 10:52 2020/4/30
 * 初始化所有的bean
 */
@InitOrder(1)
public class DefaultBeanContext implements BeanContext ,InitFunc {

    private static final Logger LOGGER= LoggerFactory.getLogger(DefaultBeanContext.class);

    /**
     * 保存所有的bean，初始化过后一次后不会改变
     */
    private static Map<String,Object> beanMap;

    /**
     * 没加载一次bean做一次判断标识，加上volatile是变量在JMM中具有可见性
     */
    private static volatile  boolean inited;

    private static Lock lock=new ReentrantLock();


    private DefaultBeanContext(){}


    @Override
    public void init() {
            doint();
    }

    @Override
    public Object getBean(String name) {
        return inited()?beanMap.get(name):null;
    }

    @Override
    public <T> T getBean(String name, Class<T> clazz) {
        Object bean = getBean(name);
        //进行强转
        return bean==null?null:(T)bean;
    }

    /**
     * 判断bean是否加载完毕
     * @return
     */
    private boolean inited(){
        while (!inited){
            doint();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return inited;
    }



    private void doint(){

        lock.lock();
        try {
            if (!inited){
                LOGGER.info("[DefaultBeanContext] doInit》》》》开始加载所有的bean类,并且进行DI操作");
                // 初始化bean
                initBean();
                // 对象注入
                injectAnnotation();

                processBeanContextAware();
                inited=true;
                LOGGER.info("[DefaultBeanContext] doInit success!》》》》所有的bean加载完毕");
            }
        } finally {
            lock.unlock();
        }


    }

    /**
     * 初始化bean
     */
    private void initBean(){
        LOGGER.info("[DefaultBeanContext] start initBean");
        try {
            //扫描指定的package下所有@Bean的类
            Set<Class<?>> classSet = ClassScaner.scanPackageByAnnotation("", Bean.class);
            //创建初始化的长度扩1
            beanMap=new LinkedHashMap<>(classSet.size()+1);
            if(CollectionUtil.isNotEmpty(classSet)){
                for(Class<?> cls:classSet){
                    /*获取该类@Bean下面的内容*/
                    Bean bean = cls.getAnnotation(Bean.class);
                    if(bean!=null){
                        //如果bean中的name 属性为空的话，采用该类的类名作为key
                        String beanName=StrUtil.isNotBlank(bean.name())?bean.name():cls.getName();
                        //保证对应的bean只有一份
                        if(beanMap.containsKey(beanName)){
                            LOGGER.warn("[DefaultBeanContext] duplicate bean with name={}", beanName);
                            continue;
                        }
                        //往bean的集合中添加bean key为@Bean中的name 或者是该类的 类名
                        beanMap.put(beanName,cls.newInstance());
                    }
                }
            }else{
                LOGGER.warn("[DefaultBeanContext] no bean classes scanned!");
            }
        } catch (Exception e) {
            LOGGER.error("[DefaultBeanContext] initBean error,cause:{}", e.getMessage(), e);
        }

    }

    /**
     * 对象注入
     *  注释处理器
     *  如果对应的Autowire配置了name属性，则根据name进行注入实例应用，
     *  否则根据属性对应的类型，注入对应的实例
     */
    private void injectAnnotation(){
        LOGGER.info("[DefaultBeanContext] start injectAnnotation");
        //forEach遍历map
        for(Map.Entry<String,Object> entry:beanMap.entrySet()){
            Object bean = entry.getValue();
            if(bean!=null){
                properAnnotaion(bean);
                fieldAnnotation(bean);
            }
        }
    }

    /**
     * 注入BeanContext到BeanContextAware中
     */
    private void processBeanContextAware(){
        LOGGER.info("[DefaultBeanContext] start processBeanContextAware");
        try {
            //扫描指定包下面的的该类的子类或者实现类
            Set<Class<?>> classSet = ClassScaner.scanPackageBySuper("", BeanContextAware.class);
            if(CollectionUtil.isNotEmpty(classSet)){
                for(Class<?> cls: classSet){
                    // 如果cls是BeanContextAware实现类
                    if(!cls.isInterface()&& BeanContextAware.class.isAssignableFrom(cls)){
                        //获取构造器
                        Constructor<?> constructor = cls.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        BeanContextAware aware = (BeanContextAware) constructor.newInstance();
                        aware.setBeanContext(getInstance());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理在set方法加入的注解,给用set方法并且加上了
     * @param bean
     */
    private void properAnnotaion(Object bean){
        LOGGER.info("[DefaultBeanContext] start propertyAnnotation");
        try {
            /**
             * Introspector.getBeanInfo(bean.getClass()) 返回一个beanInfo 暴露出该类中的所有的方法和属性
             */
            PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(bean.getClass()).getPropertyDescriptors();
            for(PropertyDescriptor descriptor: propertyDescriptors){
                //获取所有的set方法
                Method setter = descriptor.getWriteMethod();
                //判断当前setter方法有没有使用@Autowired注解
                if(setter!=null && setter.isAnnotationPresent(Autowired.class)){
                    Autowired autowired = setter.getAnnotation(Autowired.class);
                    //获取@Autowired中name属性值
                    String name;
                    Object value=null;
                    //当name当存在不为空的时候
                    if(StrUtil.isNotBlank(autowired.name())){
                        //根据指定的name 的key去beanMap中找到对象
                        name=autowired.name();
                        value=beanMap.get(name);
                    }else{
                        //当name为空,遍历beanMap
                        for(Map.Entry<String,Object> entry :beanMap.entrySet()){
                           if(descriptor.getPropertyType().isAssignableFrom(entry.getValue().getClass())){
                                value=entry.getValue();
                                break;
                           }
                        }
                    }
                    //设置访问权
                    setter.setAccessible(true);
                    //执行setter方法将值注入进去
                    setter.invoke(bean,value);
                }
            }
            LOGGER.info("[DefaultBeanContext] propertyAnnotation success!");
        } catch (Exception e) {
            LOGGER.info("[DefaultBeanContext] propertyAnnotation error,cause:{}", e.getMessage(), e);
        }
    }


    /**
     * 将@Autowried 加载属性上的注入值
     * @param bean
     */
    private void fieldAnnotation(Object bean){

        LOGGER.info("[DefaultBeanContext] start fieldAnnotation");
        try {
            //获取所有的属性
            Field[] fields = bean.getClass().getDeclaredFields();
            for(Field field: fields){
                //判断在该属性上是否添加了Autowired注解
                if(field!=null && field.isAnnotationPresent(Autowired.class)){
                    Autowired autowired = field.getAnnotation(Autowired.class);
                    String name;
                    Object value=null;
                    //@Autowired的name是否为空
                    if(StrUtil.isNotBlank(autowired.name())){
                        //不为空的话
                        name=autowired.name();
                        value=beanMap.get(name);
                    }else{
                        //遍历beanMap查看其中是否对应类型或者子类实现类
                        for(Map.Entry<String,Object> entry :beanMap.entrySet()){
                            if(field.getType().isAssignableFrom(entry.getValue().getClass())){
                                value=entry.getValue();
                                break;
                            }
                        }
                    }
                    //运行私有的修饰可以访问
                    field.setAccessible(true);
                    //进行注入
                    field.set(bean,value);
                }
            }
            LOGGER.info("[DefaultBeanContext] fieldAnnotation success!");
        } catch (Exception e) {
            LOGGER.info("[DefaultBeanContext] fieldAnnotation error,cause:{}", e.getMessage(), e);
        }

    }


    public static BeanContext getInstance(){
        return DefaultBeanContextHolder.context;
    }



    private static final class DefaultBeanContextHolder{
        private static DefaultBeanContext context = new DefaultBeanContext();
    }



}

