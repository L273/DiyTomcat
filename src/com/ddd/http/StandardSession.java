package com.ddd.http;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class StandardSession implements HttpSession{
    //用来存放属性值，即放在session里的属性值
    private Map<String,Object> attributesMap;

    //SessionID的值
    private String id;

    //Session的创建时间
    private long creationTime;

    //最后一次访问的时间
    private long lastAccessedTime;

    //ServletContext
    private ServletContext servletContext;

    //最大的生命周期
    private int maxInactiveInterval;

    public StandardSession(String jsessionid,ServletContext servletContext){
        this.attributesMap = new HashMap<>();
        this.id = jsessionid;
        this.creationTime = System.currentTimeMillis();
        this.servletContext = servletContext;
    }

    @Override
    public void removeAttribute(String name) {
        attributesMap.remove(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        //给Session的集合框架赋值
        attributesMap.put(name,value);
    }

    @Override
    public Object getAttribute(String name) {
        return attributesMap.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributesMap.keySet());
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    @Override
    public int getMaxInactiveInterval() {
        return this.maxInactiveInterval;
    }

    @Override
    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public HttpSessionContext getSessionContext() {
        return null;
    }


    @Override
    public void removeValue(String name) {

    }

    @Override
    public Object getValue(String s) {
        return null;
    }

    @Override
    public String[] getValueNames() {
        return new String[0];
    }

    @Override
    public void putValue(String s, Object o) {

    }

    //清空session参数列表里的值
    @Override
    public void invalidate() {
        attributesMap.clear();
    }

    //检查是否是最新的Session
    @Override
    public boolean isNew() {
        return creationTime == lastAccessedTime;
    }

}
