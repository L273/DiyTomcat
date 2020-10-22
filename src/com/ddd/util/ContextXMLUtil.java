package com.ddd.util;

import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ContextXMLUtil {
    //返回WEB-INF里的web.xml的位置所在
    public static String getWatchedResource(){
        try{
            String xml = FileUtil.readUtf8String(Constant.contextXmlFile);

            Document document = Jsoup.parse(xml);

            Element element = document.selectFirst("WatchedResource");

            //返回标签内的内容
            return element.text();
        }catch (Exception e){
            e.printStackTrace();
            return "WEB-INF/web.xml";
        }
    }
}
