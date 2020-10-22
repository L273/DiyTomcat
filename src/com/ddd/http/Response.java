package com.ddd.http;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Response extends BaseResponse{
    private StringWriter stringWriter;
    private PrintWriter printWriter;
    private String contentType;

    private byte[] body;

    //状态码，用于限定返回的结果
    private int status;

    //设置存放cookies的列表
    private List<Cookie> cookies;

    //用于客户端重定位的变量
    private String redirectPath;

    public Response(){
        this.stringWriter = new StringWriter();
        this.printWriter = new PrintWriter(stringWriter);

        //设置html的文件格式
        this.contentType = "text/html";

        this.cookies = new ArrayList<>();
    }


    @Override
    public PrintWriter getWriter() throws IOException {
        return this.printWriter;
    }

    public byte[] getBody()
            throws UnsupportedEncodingException{

        //body只初始化一次，并保证set后的body可用
        if(body==null){

            //返回包含迄今为止写入到当前 StringWriter 中的所有的字符串
            //即，操作后的String数据
            String content = stringWriter.toString();

            //以utf-8的格式，进行解码字符串
            //即，由printWriter操作的StringWriter的数据
            //也就是在服务器内调用getPrintWriter后，操作完的数据
            body = content.getBytes("utf-8");
        }

        return body;
    }

    //把Cookie集合转化成cookieHeader
    public String getCookiesHeader(){
        if(cookies==null)
            return "";

        String pattern = "EEE,d MMM yyyy:mm:ss 'GMT'";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern,Locale.ENGLISH);

        //建立一个buffer来存数据
        StringBuffer stringBuffer = new StringBuffer();

        //从cookies中查询cookie
        //其中cookies会在servlet中调用addCookie来生成
        //所以生成的Cookie是放在Cookies列表中的
        for(Cookie cookie : this.cookies){
            //添加Coookie
            stringBuffer.append("\r\n");
            stringBuffer.append("Set-Cookie:");

            //得到Cookie的Name和Value
            stringBuffer.append(cookie.getName() + "=" + cookie.getValue() + ";");

            //如果设置了时限的话进行检查
            if(cookie.getMaxAge() != -1){
                stringBuffer.append("Expires=");
                Date now = new Date();
                Date expire = DateUtil.offset(now, DateField.MINUTE,cookie.getMaxAge());

                stringBuffer.append(simpleDateFormat.format(expire));
                stringBuffer.append(";");
            }

            if(cookie.getPath() != null){
                //如果有path的话，就将path加入流中
                stringBuffer.append("Path="+cookie.getPath());
            }
        }
        return stringBuffer.toString();
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public String getContentType(){
        return this.contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public int getStatus() {
        return this.status;
    }

    public List<Cookie> getCookies() {
        return this.cookies;
    }

    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public String getRedirectPath() {
        return redirectPath;
    }

    //赋值redirect，方便于HttpProcess中使用
    @Override
    public void sendRedirect(String redirect) throws IOException {
        this.redirectPath = redirect;
    }

    //用于清空缓存区
    //即服务器跳转的时候，要重新设定body内的值，防止上一页面的内容影响
    @Override
    public void resetBuffer() {
        //处理底层buffer，设置长度为0
        this.stringWriter.getBuffer().setLength(0);
    }
}
