package com.ddd.http;

import com.ddd.catalina.HttpProcessor;
import com.sun.org.apache.regexp.internal.RE;
import org.apache.tools.ant.taskdefs.condition.Http;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Arrays;

//用于服务器跳转的一个类
public class ApplicationRequestDispatcher implements RequestDispatcher{

    private String uri;
    public ApplicationRequestDispatcher(String uri){
        if(!uri.startsWith("/"))
            uri = "/" + uri;

        this.uri = uri;
    }

    @Override
    public void forward(ServletRequest servletRequest, ServletResponse servletResponse)
            throws ServletException, IOException {
        Request request = (Request) servletRequest;
        Response response = (Response) servletResponse;

        //设置跳转的位置
        request.setUri(uri);

        //本质上是服务器在不重新建立socket的情况下，对资源进行访问
        HttpProcessor processor = new HttpProcessor();

        response.resetBuffer();

        processor.execute(request.getSocket(),request,response);

        //设置跳转，然后用于其他的判断条件
        request.setForwared(true);
    }

    @Override
    public void include(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {

    }
}
