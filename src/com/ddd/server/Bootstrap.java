package com.ddd.server;

import com.ddd.catalina.Server;
import com.ddd.classloader.CommonClassLoader;

import java.lang.reflect.Method;

public class Bootstrap {
    public static void main(String[] args)
            throws Exception{
        //调用一个服务器
        //通过共用类加载器，因为lib是全部都要加载的

        CommonClassLoader commonClassLoader = new CommonClassLoader();

        //它表示后续加载的类，都会使用这个 CommonClassLoader
        Thread.currentThread().setContextClassLoader(commonClassLoader);

        //服务器类的位置字符串，也就是类加载器需要的参数
        String serverClass = "com.ddd.catalina.Server";

        Class<?> serverClazz = commonClassLoader.loadClass(serverClass);

        Object serverObject = serverClazz.newInstance();

        //反射调用方法
        Method method = serverClazz.getMethod("start");
        method.invoke(serverObject);

        System.out.println(serverClazz.getClassLoader());

        //Server server = new Server();
        //server.start();
    }
}
