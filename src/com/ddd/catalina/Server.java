package com.ddd.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.NetUtil;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Server {
    private Service service;

    public Server(){
        //初始化引擎
        this.service = new Service(this);
    }

    public void start(){
         /*
        第一步，查看端口有没有被占用
         */
        int port = 18080;

        if(!NetUtil.isUsableLocalPort(port)){
            //若端口被占用
            System.out.println("端口："+port+"被占用！");
            return;
        }


        /*
        第二步，打印日志信息，并启动端口线程
         */
        TimeInterval timeInterval = DateUtil.timer();
        logJVM();
        init();
        LogFactory.get().info("Server startup in {} ms",timeInterval.intervalMs());
    }

    private void init(){
        service.start();
    }
    //处理日志信息
    private void logJVM(){
        Map<String,String> infos = new LinkedHashMap<>();
        infos.put("Server version","How2J DiyTomcat/1.0.1");
        infos.put("Server built","2020-04-08 10:20:22");
        infos.put("Server number","1.0.1");

        //系统的名称
        infos.put("OS Name\t", SystemUtil.get("os.name"));

        //系统的版本
        infos.put("OS Version",SystemUtil.get("os.version"));
        infos.put("Architecture",SystemUtil.get("os.arch"));

        //java bin的地址，还有java的版本，java的规范
        infos.put("Java Home",SystemUtil.get("java.home"));
        infos.put("JVM Version",SystemUtil.get("java.runtime.version"));
        infos.put("JVM Vendor",SystemUtil.get("java.vm.specification.vendor"));


        Set<String> keys = infos.keySet();
        for (String key:keys){
            //调用hutool写好的log4j生成工具，然后传递信息答应即可。
            LogFactory.get().info(key+":\t\t"+infos.get(key));
        }
    }



}
