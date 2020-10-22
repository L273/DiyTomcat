package com.ddd.catalina;

import com.ddd.http.StandardServletConfig;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class StandardFilterConfig implements FilterConfig {
    private ServletContext servletContext;
    private Map<String,String> initParameters;
    private String filterName;

    public StandardFilterConfig(ServletContext servletContext
            ,String filterName,Map<String,String> initParameters){
        this.servletContext = servletContext;
        this.filterName = filterName;
        this.initParameters = initParameters;

        if(this.initParameters ==null)
            this.initParameters = new HashMap<>();
    }

    @Override
    public String getFilterName() {
        return this.filterName;
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
        return Collections.enumeration(initParameters.keySet());
    }
}
