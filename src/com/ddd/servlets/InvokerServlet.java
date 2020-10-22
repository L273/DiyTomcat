package com.ddd.servlets;

import cn.hutool.core.util.ReflectUtil;
import com.ddd.catalina.Context;
import com.ddd.http.Request;
import com.ddd.http.Response;
import com.ddd.util.Constant;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


//一个单例用于处理Servlet的请求
//之所以设计单例类，是为了防止之后的开销
//因为目标 servlet 实现了 HttpServlet ,所以一定提供了 service 方法。
//这个 service 方法实会根据 request 的 Method ，访问对应的 doGet 或者 doPost。
public class InvokerServlet extends HttpServlet {

    private  static InvokerServlet invokerServlet;

    private InvokerServlet(){ }

    //防止多个线程同时操作该方法的时候
    //造成错误
    public synchronized static InvokerServlet getInstance(){
        if(invokerServlet==null)
            invokerServlet = new InvokerServlet();

        return invokerServlet;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        //super.service(req, resp);

        Response response = (Response) resp;
        Request request = (Request) req;

        String uri = request.getUri();
        Context context = request.getContext();

        //得到Serlvet类的位置，用于类加载器来加载使用类
        //参数和反射一致
        String servletClassName = context.getServletClass(uri);

        try{
            //先从扫描器的classes中找到该类
            Class servletClass = context.getWebappClassLoader().loadClass(servletClassName);

            System.out.println("servletClass:" + servletClassName);
            System.out.println("servletClass'classLoader:"+servletClass.getClassLoader());

            //再用反射将该类实例化
            //利用getServlet是用单例模式，因为servlet一旦生成一次
            //就会存放在Context的ServletPool中，之后再调用的时候，就会调用之前的servlet
            Object servletObject = context.getServlet(servletClass);
            ReflectUtil.invoke(servletObject,"service",request,response);

            //如果代码内设置了redirectPath的话，这里就要将值设置成302
            if(response.getRedirectPath()!=null)
                response.setStatus(Constant.CODE_302);
            else
                response.setStatus(Constant.CODE_200);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
