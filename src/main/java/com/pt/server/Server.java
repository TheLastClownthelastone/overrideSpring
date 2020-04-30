package com.pt.server;

/**
 * @Author :pt
 * @Date :Created in 9:56 2020/4/30
 * 进行启动程序
 */
public interface Server {

    //进行启动前面的准备
    void preStart();

    //启动项目
    void start();


}
