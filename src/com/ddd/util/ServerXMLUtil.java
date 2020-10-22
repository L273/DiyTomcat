package com.ddd.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import com.ddd.catalina.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class ServerXMLUtil {

    public static List<Connector> getConnectors(Service service){
        List<Connector> result = new ArrayList<>();

        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);

        Document document = Jsoup.parse(xml);

        Elements elements = document.select("Connector");

        //遍历全部的Connector标签内的内容
        for(Element element:elements){
            //解析Connector标签中端口的值
            int port = Convert.toInt(element.attr("port"));

            //剖析Connector中gzip的四个参数
            String compression = element.attr("compression");
            int compressionMinSize = Convert.toInt(element.attr("compressionMinSize"),0);
            String noCompressionUserAgents = element.attr("noCompressionUserAgents");
            String compressableMimeType = element.attr("compressionMimeType");

            Connector connector = new Connector(service);

            connector.setPort(port);

            //设置gzip的四个参数
            connector.setCompression(compression);
            connector.setCompressableMimeType(compressableMimeType);
            connector.setNoCompressionUserAgents(noCompressionUserAgents);
            connector.setCompressionMinSize(compressionMinSize);

            result.add(connector);
        }
        return result;
    }


    //目前只是单一Server，单一Service，单一Engine，单一Host
    // 所以，Context的获取暂时不做改动
    public static List<Context> getContexts(Host host){
        List<Context> result = new ArrayList<>();

        //得到相关xml的String
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);

        //使用Jsoup进行xml文件字符串的解析
        Document document = Jsoup.parse(xml);

        //用Jsoup在解析完的xml里找出的全部Context标签
        //Select的语法就有点像是JQuery了
        Elements elements = document.select("Context");

        for (Element element:elements){
            //得到path属性对应的值
            String path = element.attr("path");
            //得到docBase属性对应的值
            String docBase = element.attr("docBase");

            //是否热加载
            boolean reloadable = Convert.toBool(element.attr("reloadable"),true);

            //初始化Context
            Context context = new Context(path,docBase,host,reloadable);

            //将初始化的Context放入result中
            result.add(context);
        }

        //返回结果
        return result;
    }

    public static String getEngineName(){

        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);

        Document document = Jsoup.parse(xml);

        Element element = document.selectFirst("Engine");

        //返回引擎的默认名字
        return element.attr("name");
    }

    public static String getServiceName(){
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);

        Document document = Jsoup.parse(xml);

        Element host = document.selectFirst("Server");
        return host.attr("name");
    }


    //得到一个Host列表
    //即将xml文件里的host标签全部取出
    //获取所有的 Host. server.xml 的 host 理论上可以有多个，但是常常是只有一个。
    public static List<Host> getHosts(Engine engine){
        List<Host> result = new ArrayList<>();

        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);

        Document document = Jsoup.parse(xml);

        Elements elements = document.select("Engine");

        for (Element element:elements){
            //选取标签中引擎名字一样的进行下一次遍历
            if(element.attr("name").equals(engine.getEngineName())){
                for(Element element1: element.select("Host")){
                    String name = element1.attr("name");
                    Host host = new Host(name,engine);
                    result.add(host);
                }
            }
        }
        return result;
    }

}
