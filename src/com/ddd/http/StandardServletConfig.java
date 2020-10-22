package com.ddd.http;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


//在Servlet初始化的时候，传进去的参数对象
public class StandardServletConfig implements ServletConfig{
    //servlet容器
    private ServletContext servletContext;
    //放在web.xml中初始化的参数
    private Map<String,String> initParameters;
    //servlet的名字
    private String servletName;

    public StandardServletConfig(ServletContext servletContext,String servletName
            ,Map<String,String> initParameters){
        this.servletContext = servletContext;
        this.servletName = servletName;
        this.initParameters = initParameters;

        //当没有初始的参数的时候
        //默认生成一个hash表
        if(this.initParameters == null)
            this.initParameters = new HashMap<>();
    }


    @Override
    public String getServletName() {
        return this.servletName;
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public String getInitParameter(String name) {
        return this.initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(this.initParameters.keySet());
    }
}
