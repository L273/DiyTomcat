package com.ddd.catalina;

import cn.hutool.core.util.ArrayUtil;

import javax.servlet.*;
import java.io.IOException;
import java.util.List;

public class ApplicationFilterChain implements FilterChain {

    private Filter[] filters;
    private Servlet servlet;
    int pos;

    public ApplicationFilterChain(List<Filter> filterList,Servlet servlet){
        //将以Filter构成的集合框架，转换成数组
        this.filters = ArrayUtil.toArray(filterList,Filter.class);
        this.servlet = servlet;
    }

    //于Web项目中的Filter调用
    //其中，doFilter可调用的次数，和filters的长度是一致的
    //即，doFilter可调用的次数，等于符合匹配url模式的filter个数
    @Override
    public void doFilter(ServletRequest request, ServletResponse response)
            throws IOException, ServletException {

        //System.out.println("=============================");
        //System.out.println(filters.length);
        //System.out.println(pos);
        //System.out.println("=============================");

        if(pos<filters.length){
            //用一次就往后遍历
            Filter filter = filters[pos++];

            //调用第一个filter，即，第一个符合条件的filter
            //但是，另一个doFilter的控制，就放在第一个符合条件的filter中
            //如果哪一个符合条件的filter放行，即后面的filter也就有机会执行doFilter
            //这样，pos的数量就可以慢慢增加到filters的数量
            // 即当pos的数量等于符合条件的filter的个数的时候，if不成立，执行else中的service
            filter.doFilter(request,response,this);
        }else {
            servlet.service(request,response);
        }

    }


}
