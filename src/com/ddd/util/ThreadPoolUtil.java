package com.ddd.util;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

//自定义的一个线程池的工具类
public class ThreadPoolUtil {
    //五个参数的意思分别为
    //初始化的线程池有20条线程
    //线程池最大可有60条线程
    //线程池空闲的线程最长等待时间为60秒，超过60秒即回收
    //定义一个任务列表，用于缓存任务。其中当缓存的任务超过10条的时候，即20条线程启动，然后缓存队列满的时候，后面才会开启新的线程
    private static ThreadPoolExecutor threadPoolExecutor =
            new ThreadPoolExecutor(20,60,60, TimeUnit.SECONDS,
                    new LinkedBlockingDeque<Runnable>(10));

    public static void run(Runnable runnable){
        //开放一个类，可以让外面丢入任务列表，然后启动线程池
        threadPoolExecutor.execute(runnable);
    }
}
