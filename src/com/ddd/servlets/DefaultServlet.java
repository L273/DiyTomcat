package com.ddd.servlets;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.ddd.catalina.Context;
import com.ddd.http.Request;
import com.ddd.http.Response;
import com.ddd.util.Constant;
import com.ddd.util.WebXMLUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

//处理静态文件
//如直接访问jsp，或者html文件的uri映射处理
public class DefaultServlet extends HttpServlet{

    private static DefaultServlet defaultServlet;

    private DefaultServlet(){}

    public static synchronized DefaultServlet getInstance(){
        if(defaultServlet==null)
            defaultServlet = new DefaultServlet();
        return defaultServlet;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Request request = (Request) req;
        Response response = (Response) resp;

        String uri = request.getUri();
        Context context = request.getContext();

        //handle500的测试
        if("/500.html".equals(uri)){
            throw new RuntimeException("this is a deliberately created exception");
        }

        //这里分两种情况
        //1、根目录     2、一般目录
        if ("/".equals(uri)) {
            uri = WebXMLUtil.getWeblcomeFile(request.getContext());
        }

        //由于JSP文件有专门的JspServlet进行处理
        //所以如果找到JSP文件，就选择退出
        if(uri.endsWith(".jsp")){
            JspServlet.getInstance().service(request,response);
            return;
        }

        //删除最前面的 /
        String fileName = StrUtil.removePrefix(uri, "/");

        //用类中处理好的本项目的webapps目录
        //然后再在webapps目录内创建该目录下的文件

        File file = new File(context.getDocBase(), fileName);

        //利用一般的文件位置进行uri定位
        //如果存在该文件，就说明URL定位成功
        if (file.exists()) {
            //得到文件的后缀名
            String extName = FileUtil.extName(file);

            //利用后缀名得到mimeType对应的参数
            String mimeType = WebXMLUtil.getMineType(extName);

            //设置返回文件的格式
            //用于之后的handle200
            response.setContentType(mimeType);

            //改成操作二进制文件，字节流文件
            byte[] body = FileUtil.readBytes(file);

            response.setBody(body);

            //默认为200处理成功
            response.setStatus(Constant.CODE_200);

            //几种特殊情况
            if (fileName.equals("timeConsume.html")) {
                ThreadUtil.sleep(1000);
            }
        }else {
            //反之不存在就是没找到
            response.setStatus(Constant.CODE_404);
        }
    }
}
