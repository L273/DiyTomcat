package com.ddd.util;

import cn.hutool.core.io.FileUtil;
import com.ddd.catalina.Context;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.ddd.util.Constant.webXmlFile;

public class WebXMLUtil {
    //装入MimeType的映射
    private static Map<String,String> mimeTypeMapping = new HashMap<>();

    private synchronized static void initMimeType(){
        //读取webXml
        String xml = FileUtil.readUtf8String(webXmlFile);

        Document document = Jsoup.parse(xml);

        Elements elements = document.select("mime-mapping");

        for(Element element:elements){
            //拿到mime-mapping中，第一个extension标签的内容
            String extName = element.select("extension").first().text();

            //拿到mime-mapping中，第一个mime-type标签的内容
            String mimeType = element.select("mime-type").first().text();

            //对mimeTypeMapping推入数据
            mimeTypeMapping.put(extName,mimeType);
        }
    }


    public static synchronized String getMineType(String extName){
        //如果mimeTypeMapping内没有数据，那么就要先初始化该Map
        if(mimeTypeMapping.isEmpty())
            initMimeType();

        //得到数据
        String mimeType = mimeTypeMapping.get(extName);

        //如果得到的数据为空。默认返回text/html
        if(mimeType == null)
            return "text/html";

        //如果有数据，发挥MimeType
        return mimeType;
    }


    public static String getWeblcomeFile(Context context){
        //读取xml文件
        String xml = FileUtil.readUtf8String(webXmlFile);

        Document document = Jsoup.parse(xml);

        Elements elements = document.select("welcome-file");

        for (Element element : elements){
            //得到标签内的text内容，和JQuery的用法相似
            String welcomeFileName = element.text();

            //得到目录下的默认欢迎文件
            File file = new File(context.getDocBase(),welcomeFileName);

            if(file.exists())
                return file.getName();
        }

        //默认返回index.html
        return "index.html";
    }
}
