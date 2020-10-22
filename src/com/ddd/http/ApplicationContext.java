package com.ddd.http;

import com.ddd.catalina.Context;

import java.io.File;
import java.util.*;

public class ApplicationContext extends BaseServletContext{

    //用于存属性值的Map
    private Map<String,Object> attributesMap;

    //内置一个context，以便之后的调用，ApplicationContext中很多方法，其实就是在调用它
    private Context context;

    public ApplicationContext(Context context){
        this.context = context;
        attributesMap = new HashMap<>();
    }

    @Override
    public void removeAttribute(String s) {
        this.attributesMap.remove(s);
    }

    @Override
    public void setAttribute(String s, Object o) {
        this.attributesMap.put(s,o);
    }

    @Override
    public Object getAttribute(String s) {
        return this.attributesMap.get(s);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        Set<String> names = this.attributesMap.keySet();
        //将集合names置于一个容器中，enueration的用法类似于一般的迭代器
        //可调用.hasMoreElements()来遍历下一个数据
        return Collections.enumeration(names);
    }

    @Override
    public String getRealPath(String path) {
        //传入的path为资源定位符,通常为uri
        //而docBase为本App所在的目录位置（通常在webapps的某个目录下，或者javaweb项目的web目录下）
        return new File(context.getDocBase(),path).getAbsolutePath();
    }
}
