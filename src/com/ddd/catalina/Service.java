package com.ddd.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;
import com.ddd.util.ServerXMLUtil;

import java.util.List;

public class Service {
    private String name;
    //嵌套定义
    private Engine engine;
    //一个Service内可有多个Connector
    private List<Connector> connectors;

    //上级标签，一对多的关系，保持xml内部的嵌套关系
    private Server server;

    public Service(Server server){
        this.server = server;
        this.name = ServerXMLUtil.getServiceName();
        this.engine = new Engine(this);
        this.connectors = ServerXMLUtil.getConnectors(this);
    }

    public Engine getEngine() {
        return engine;
    }

    public void start(){
        this.init();
    }

    private void init(){
        //获得一个定时器
        TimeInterval timeInterval = DateUtil.timer();

        for(Connector connector:connectors){
            connector.init();
        }

        LogFactory.get().info("Initialization processed in {} ms",timeInterval.intervalMs());

        for(Connector connector:connectors)
            connector.start();
    }
}
