package com.ddd.catalina;

import com.ddd.util.ServerXMLUtil;

import java.util.List;

public class Engine {
    private String name;

    //嵌套定义
    private List<Host> hosts;

    //上级标签
    private Service service;

    public Engine(Service service){
        this.service = service;
        this.name = ServerXMLUtil.getEngineName();
        this.hosts = ServerXMLUtil.getHosts(this);

        checkDefault();
    }

    public Service getService() {
        return service;
    }

    private void checkDefault(){
        //如果没有查到该引擎，则想上级丢出一个异常

        if(getDefaultHost()==null)
            throw new RuntimeException("the default "+name+" does not exist");
    }

    public String getEngineName() {
        return name;
    }

    public Host getDefaultHost() {
        for(Host host:hosts){
            //由于是单一的Host，目前，所以，这里从Engine获取Host的方式，暂时不用改
            if(host.getName().equals(name))
                return host;
        }
        return null;
    }

}
