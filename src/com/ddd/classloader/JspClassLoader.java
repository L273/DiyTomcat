package com.ddd.classloader;

import cn.hutool.core.util.StrUtil;
import com.ddd.catalina.Context;
import com.ddd.util.Constant;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;


//一个文件对应一个JspClassLoader
//如果这个jsp修改了，就要换一个新的JspClassLoader
//JspClassLoader是扫描加载已经从jsp转成class的文件
public class JspClassLoader extends URLClassLoader{
    private static Map<String,JspClassLoader> map = new HashMap<>();

    private JspClassLoader(Context context){
        //继承自WebClassLoader
        super(new URL[]{},context.getWebappClassLoader());

        try {
            //subFolder，即将要扫描的work目录下的子文件夹的名字
            String subFolder;
            String path = context.getPath();

            //如果是根目录，就转换成_
            if("/".equals(path))
                subFolder = "_";
            else
                subFolder = StrUtil.subAfter(path,"/",false);

            //扫描该子文件夹
            File classesFolder = new File(Constant.workFolder,subFolder);

            //将扫描到的文件定义成一个URL
            URL url = new URL("file:"+classesFolder.getAbsolutePath() +"/");

            //将得到的URL放到集合中
            this.addURL(url);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    //删除某一个jsp和loader的关联
    public static void invalidJspClassLoader(String uri, Context context){
        String key = context.getPath() + "/" +uri;
        map.remove(key);
    }

    //从单例化的JspClassLoader中，取出一个JspClassLoader。同时对应一个jsp文件
    public static JspClassLoader getJspClassLoader(String uri,Context context){
        //利用path+uri作为映射，即就是在工程文件（%TOMCAT_HOME%）下的相对目录，即jsp的位置
        String key = context.getPath() + "/" + uri;

        //然后用这个健去找JspClassLoader
        JspClassLoader loader = map.get(key);

        //如果没有找到，则生成一个新的JspClassLoader
        //然后放进map中
        if(loader==null){
            loader = new JspClassLoader(context);
            map.put(key,loader);
        }

        //两种情况，有的话返回的是map.get(key)得到的loader。反之就是new JspClassLoader得到的loader
        return loader;
    }

}
