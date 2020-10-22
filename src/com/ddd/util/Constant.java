package com.ddd.util;

import cn.hutool.system.SystemUtil;
import org.junit.Test;

import java.io.File;

//定义一个常量类
public class Constant {

    //四个状态常量
    public static final int CODE_200 = 200;
    public static final int CODE_302 = 302;
    public static final int CODE_404 = 404;
    public static final int CODE_500 = 500;

    //一个202响应的模板字符串
    //其中有一个HTTP的响应头
    //以及一个文件格式的定义字符串
    //四个\r\n\r\n一定要写在一行，不然浏览器的头部会解析报错（会解析成undefine undefine undefine这种）
    public final static String response_head_202_gzip =
            "HTTP/1.1 200 OK\r\n"+
                    "Content-Type:{}{}\r\n" +
                    "Content-Encoding:gzip\r\n\r\n";

    public final static String response_head_202 =
            "HTTP/1.1 200 OK\r\n"+
                    "Content-Type:{}{}\r\n\r\n";

    //一个404响应的头信息
    public final static String response_head_404 =
            "HTTP/1.1 404 Not Found\r\n"+
                    "Content-Type:text/html\r\n\r\n";

    //一个404的响应的头的内容
    //其中两个{}用于之后的format填充数据
    public final static String textFormat_404 =
            "<html><head><title>DiyTomcat/1.0.1 - Error report</title><style>" +
            "<!--H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} " +
            "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} " +
            "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} " +
            "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} " +
            "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} " +
            "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}" +
            "A {color : black;}A.name {color : black;}HR {color : #525D76;}--></style> " +
            "</head><body><h1>HTTP Status 404 - {}</h1>" +
            "<HR size='1' noshade='noshade'><p><b>type</b> Status report</p><p><b>message</b> <u>{}</u></p><p><b>description</b> " +
            "<u>The requested resource is not available.</u></p><HR size='1' noshade='noshade'><h3>DiyTocmat 1.0.1</h3>" +
            "</body></html>";

    //定义一个500的头
    public final static String response_head_500 =
            "HTTP/1.1 500 Internal Server Error\r\n"+
                    "Content-Type:text/html\r\n\r\n";

    //定义一个500的内容
    //其中两个uri用来输入之后格式化format进入的数据
    public final static String TextFormat_500 =
            "<html><head><title>DIY Tomcat/1.0.1 - Error report</title><style>"
            + "<!--H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} "
            + "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} "
            + "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} "
            + "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} "
            + "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} "
            + "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}"
            + "A {color : black;}A.name {color : black;}HR {color : #525D76;}--></style> "
            + "</head><body><h1>HTTP Status 500 - An exception occurred processing {}</h1>"
            + "<HR size='1' noshade='noshade'><p><b>type</b> Exception report</p><p><b>message</b> <u>An exception occurred processing {}</u></p><p><b>description</b> "
            + "<u>The server encountered an internal error that prevented it from fulfilling this request.</u></p>"
            + "<p>Stacktrace:</p>" + "<pre>{}</pre>" + "<HR size='1' noshade='noshade'><h3>DiyTocmat 1.0.1</h3>"
            + "</body></html>";


    public final static String response_head_302 =
            "HTTP/1.1 302 Found\r\n" +
                    "Location:{}\r\n\r\n";

    //定义两个文本的操作对象
    //SystemUtil.get("user.dir") 会得到本项目的目录地址
    public final static File webappsFolder = new File(SystemUtil.get("user.dir"),"webapps");

    //在本目录的目录地址下的webapps的目录中，创建一个子目录，ROOT
    public final static File rootFolder = new File(webappsFolder,"ROOT");

    //得到配置文件的目录
    public static final File confFolder = new File(SystemUtil.get("user.dir"),"conf");

    //得到相关配置的xml的文件
    public static final File serverXmlFile = new File(confFolder,"server.xml");

    //conf目录下的web.xml文件
    public static final File webXmlFile = new File(confFolder,"web.xml");

    //得到conf目录下的context.xml文件，这个为记录Servlet的配置文件的xml
    public static final File contextXmlFile = new File(confFolder,"context.xml");


    //存放JSP的生成的.java文件的目录，即项目目录下的work目录内
    //File.separator为文件的分隔符
    public static final String workFolder = SystemUtil.get("user.dir") + File.separator + "work";

//    @Test
//    public void test(){
//        System.out.println(File.separator);
//    }
}