package com.ddd.servlets;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.ddd.catalina.Context;
import com.ddd.classloader.JspClassLoader;
import com.ddd.http.Request;
import com.ddd.http.Response;
import com.ddd.util.Constant;
import com.ddd.util.JspUtil;
import com.ddd.util.WebXMLUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

//一个专门处理jsp文件的Servlet
public class JspServlet extends HttpServlet{
    private static final long serialVersionUID = 1L;
    private static JspServlet jspServlet;

    //单例模式
    private JspServlet(){}

    public static synchronized JspServlet getInstance(){
        if(jspServlet==null)
            jspServlet = new JspServlet();
        return jspServlet;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            Request request = (Request)req;
            Response response = (Response)resp;

            String uri = request.getRequestURI();

            //如果是访问根目录的话，就进入欢饮界面
            if("/".equals(uri))
                uri = WebXMLUtil.getWeblcomeFile(request.getContext());

            //反之，去除uri前面的/，然后得到最终的资源定位符
            String fileName = StrUtil.removePrefix(uri,"/");
            //并得到该文件在服务器的准确地址
            //即传入的docBase+后面的path生产的的一条URL
            File jspFile = FileUtil.file(request.getRealPath(fileName));

            if(jspFile.exists()){
                //获得文件的文件名
                String extName = FileUtil.extName(jspFile);
                //得到Mime的名字，即文件的类型
                String mimeType = WebXMLUtil.getMineType(extName);
                //设置mime的格式
                response.setContentType(mimeType);

                //得到context
                Context context = request.getContext();

                //从context得到path，即项目目录文件的名称
                String path =context.getPath();
                String subFolder;

                //定义subFolder的位置
                if("/".equals(path))
                    subFolder = "_";
                else
                    subFolder = StrUtil.subAfter(path,'/',false);

                //传进一般项目目录的位置，同时，也把项目目录的文件作为参数放进去
                String servletClassPath = JspUtil.getServletClassPath(uri,subFolder);

                //得到目录的位置后，就用该位置获取文件
                File jspServletClassFile = new File(servletClassPath);

                //这里就将jsp文件，转换到响应的work目录下
                //再用jspSerrvletClassFile和jsp文件的时间进行比较。
                //如果jspServletClassFile文件比jsp文件老
                // 就说明jsp文件之后进行了更新，所以就要重新生成class文件
                if(!jspServletClassFile.exists())
                    JspUtil.compileJsp(context,jspFile);
                else if(jspFile.lastModified() > jspServletClassFile.lastModified()){
                    JspUtil.compileJsp(context,jspFile);

                    //删除旧的classloader和jsp的关联

                    //因为生成classloader放到了统一处理。而生成Classloader的时候，jsp就会和ClassLoader产生关联
                    //所以，这里要去掉老的，以防一个jsp对应两个classLoader
                    JspClassLoader.invalidJspClassLoader(uri,context);
                }

                //得到一个classLoader
                JspClassLoader jspClassLoader = JspClassLoader.getJspClassLoader(uri,context);

                //得到一个servletName，即传入jsp的uri和subFoler文件位置，然后得到响应的class文件所在的位置
                String jspServletClassName = JspUtil.getJspServletClassName(uri,subFolder);

                //用加载器加载该文件，即用jsp对应的加载器加载该jsp文件
                Class jspServletClass = jspClassLoader.loadClass(jspServletClassName);

                //然后实例化该类对象
                HttpServlet servlet = context.getServlet(jspServletClass);

                //启动servlet的service服务，然后之后的过程就是servlet自己去处理了，因为jsp已经转成了servlet了
                servlet.service(request,response);

                if(response.getRedirectPath()==null){
                    //设置200的状态码，表示OK
                    response.setStatus(Constant.CODE_200);
                }else {
                    response.setStatus(Constant.CODE_302);
                }

            }else {
                //反之则是没有找到JSP文件
                response.setStatus(Constant.CODE_404);
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
