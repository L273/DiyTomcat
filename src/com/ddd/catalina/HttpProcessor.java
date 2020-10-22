package com.ddd.catalina;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.log.LogFactory;
import com.ddd.http.Request;
import com.ddd.http.Response;
import com.ddd.servlets.DefaultServlet;
import com.ddd.servlets.InvokerServlet;
import com.ddd.servlets.JspServlet;
import com.ddd.util.Constant;
import com.ddd.util.SessionManager;
import com.sun.xml.internal.ws.api.ha.StickyFeature;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

//用来处理Socket请求
public class HttpProcessor {

    public void execute(Socket socket, Request request,Response response){
        try{

            //由于这里文件的位置是由URI来定义的
            String uri = request.getUri();

            //遍历session
            prepareSession(request,response);

            //所以URI为空的话，就说明无法进入该文件位置
            if (uri == null) {
                //没有就无法操作，于是就直接结束本线程
                return;
            } else {
                //反之则是存在URI

                //得到请求的报文
                //即从Request里处理好的String
                System.out.println("浏览器的请求报文：\r\n" + request.getRequestString());
                System.out.println("uri：" + request.getUri());

                //定义一个Context
                Context context = request.getContext();

                //用Servlet配置的xml的url进行定位
                String servletClassName = context.getServletClass(uri);

                //定义一个WorkServet
                //因为service都是放在doChain内，所以这样可以作为参数传入filter
                HttpServlet workServlet;


                if(servletClassName!=null){
                    //如果设置了该类的url映射
                    //则进入循环

                    //调用InvokerServlet来处理该Servlet的访问
                    workServlet = InvokerServlet.getInstance();

                }else if (uri.endsWith(".jsp")){
                    //处理JSP文件
                    workServlet = JspServlet.getInstance();
                }else {
                    //反之找不到Servlet
                    // 即文件的情况
                    workServlet = DefaultServlet.getInstance();
                }

                List<Filter> filters = request.getContext().getMatchedFilters(request.getRequestURI());

                ApplicationFilterChain filterChain = new ApplicationFilterChain(filters,workServlet);

                filterChain.doFilter(request,response);

            }




            //forward里访问HttpProcess结束
            if(request.isForwared())
                return;

            //根据response的状态，来进行handle返回
            if(response.getStatus()==Constant.CODE_200)
                handle200(socket,request,response);
            else if(response.getStatus()==Constant.CODE_404)
                handle404(socket,uri);
            else if (response.getStatus()==Constant.CODE_302) {
                handle302(socket,response);
            }else {
                    //其他情况，暂时没写
            }
        }catch (Exception e){
            LogFactory.get().error(e);
            handle500(socket,e);
        }
        finally {
            try {
                //一个访问结束
                //如果产生了连接，则关闭连接，释放资源
                if(!socket.isClosed())
                    socket.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    //处理成功的响应
    private void handle200(Socket socket,Request request, Response response)
            throws IOException {
        String contentType = response.getContentType();

        //Cookie头，绑定在头部的第二个{}中
        String cookiesHeader = response.getCookiesHeader();

        byte[] body = response.getBody();

        //判断是否进行gzip压缩
        boolean gzip = isGzip(request,body,contentType);

        //其中response_head_202中的响应格式的文档限制，已经用{}代替
        //所以格式只要使用接口李的数据response_head_202即可
        String headText;
        if(gzip)
            headText = Constant.response_head_202_gzip;
        else
            headText = Constant.response_head_202;

        headText = StrUtil.format(headText,contentType,cookiesHeader);

        //前面只是修饰响应头，以便浏览器反应并执行gzip操作
        //这里就是真正进行gzip的压缩过程（压缩body，即内容体）
        if(gzip)
            body = ZipUtil.gzip(body);

        byte[] head = headText.getBytes();

        //整体返回的字节流的定义
        byte[] responseBytes = new byte[head.length+body.length];

        //先将head复制到返回的字符串中
        ArrayUtil.copy(head,0,responseBytes,0,head.length);

        //再将后半部分的body复制到响应的字符串中,其中响应字符串的偏移量为头的长度
       ArrayUtil.copy(body,0,responseBytes,head.length,body.length);

        //得到返回的输出流
        OutputStream outputStream = socket.getOutputStream();

        //写入反馈的字节序列，即发送
        outputStream.write(responseBytes);

        //关闭流
        outputStream.close();
    }

    private void handle302(Socket socket,Response response)
            throws IOException{

        OutputStream outputStream = socket.getOutputStream();

        String redirectPath = response.getRedirectPath();

        String handText = Constant.response_head_302;

        handText = StrUtil.format(handText,redirectPath);

        byte[] body = handText.getBytes("utf-8");

        outputStream.write(body);

        outputStream.close();
    }

    //处理没有找到
    private void handle404(Socket socket,String uri)
            throws IOException{
        //得到返回的输出流
        OutputStream outputStream = socket.getOutputStream();

        //填充404错误页面中两个{}的内容
        String responseText = StrUtil.format(Constant.textFormat_404,uri,uri);

        //加上头部
        responseText = Constant.response_head_404 + responseText;

        //对文件流进行urf-8的字节解码
        byte[] responseByte = responseText.getBytes("utf-8");

        //将数据返回给请求的客户端
        outputStream.write(responseByte);

        outputStream.close();
    }

    //处理服务器发生异常的时候
    private void handle500(Socket socket,Exception e){
        OutputStream outputStream;
        try{
            outputStream = socket.getOutputStream();
            //将错误的信息压入堆栈，即全部的错误信息
            StackTraceElement[] stackTraceElements = e.getStackTrace();

            StringBuffer stringBuffer = new StringBuffer();

            stringBuffer.append(e.toString());
            stringBuffer.append("\r\n");

            //详情信息用异常栈中的数据来显示
            for(StackTraceElement stackTraceElement: stackTraceElements){
                stringBuffer.append("\t");
                stringBuffer.append(stackTraceElement.toString());
                stringBuffer.append("\r\n");
            }

            String msg = e.getMessage();
            if(msg != null && msg.length()>20){
                //如果消息的长度大于20，那么则要对其进行截取
                msg = msg.substring(0,19);
            }

            //分别将错误的消息、错误的概览、还有整体错误的信息
            //format进一串字符串中
            String text = StrUtil.format(Constant.TextFormat_500,msg,e.toString(),stringBuffer.toString());

            //加上头部
            text = Constant.response_head_500 + text;

            byte[] responseBytes = text.getBytes("utf-8");

            //将500的界面输出回浏览器
            outputStream.write(responseBytes);

            outputStream.close();
        }catch (IOException e1){
            e1.printStackTrace();
        }
    }

    public void prepareSession(Request request,Response response){
        //从request中得到jsessionID的值
        String jsessionid = request.getJSessionIdFromCookie();

        System.out.println("传递进来的sessionID的值："+jsessionid);

        //通过JessID，req，resp来获得Session的值
        HttpSession session = SessionManager.getSession(jsessionid,request,response);

        //将得到的Session的值放到request中
        request.setSession(session);
    }

    //判断是否进行gzip压缩
    private boolean isGzip(Request request,byte[] body,String mimeType){
        //得到头部的Accpt-Encoding的标识
        String acceptEncodings = request.getHeader("Accept-Encoding");

        //如果标识的内容里，没有gzip的选项，那么就直接返回false
        if(!StrUtil.containsAny(acceptEncodings,"gzip")){
            //System.out.println("没有标签内容Gzip");
            return false;
        }


        //得到request对应的连接
        Connector connector = request.getConnector();

        //看mimeType里面有没有;
        if(mimeType.contains(";"))
            mimeType = StrUtil.subBefore(mimeType,";",false);

        //如果，在getCompression中没有on的话，就返回false
        //即表示没有开启
        if(!"on".equals(connector.getCompression())){
           // System.out.println("没有开启gzip压缩的选项");
            return false;
        }


        //如果消息的长度小于消息的最小尺寸
        //那么，消息就选择不压缩
        if(body.length < connector.getCompressionMinSize())
            return false;


        //查找浏览器的代理头中是不是有在Connector中被排除的
        //如果有，那么就返回false，就说明不进行压缩
        String userAgents = connector.getNoCompressionUserAgents();
        String[] eachUserAgents = userAgents.split(";");
        for (String eachUserAgent : eachUserAgents){
            eachUserAgent = eachUserAgent.trim();
            String userAgent = request.getHeader("User-Agent");
            //System.out.print("标签内的浏览器头部："+eachUserAgent);
            //System.out.println("\t请求的头部："+userAgent);
            if(StrUtil.containsAny(userAgent,eachUserAgent)){
               // System.out.println("头部不符合");
                return false;
            }
        }

        //检查是否符合头部，如果符合，就说明要压缩，返回true
        String mimeTypes = connector.getCompressableMimeType();
        //System.out.println("请求的MIME："+mimeType);
        String[] eachMimeTypes = mimeTypes.split(",");
        for(String eachMimeType:eachMimeTypes){
            if(mimeType.equals(eachMimeType)){
                return true;
            }

        }

        //由于true的设置，仅仅存在之前的
        return false;
    }

}
