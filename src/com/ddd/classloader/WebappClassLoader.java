package com.ddd.classloader;

import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

//当然，一个path，可以存放一个web应用
//所以，一个Context的path，就要一个webapp单独的类加载器
//即，这个加载器，是给webapps目录下的子目录，即App应用使用的
//而这个加载器是加载当前web应用的lib的jar包
public class WebappClassLoader extends URLClassLoader{
    public WebappClassLoader(String docBase, ClassLoader commonClassLoader){
        super(new URL[] {});

        //WEB-INF内lib中的jar包
        //WEB-INF内classes中的class文件
        try {
            File webinFolder = new File(docBase,"WEB-INF");

            //生成class目标文件的目录
            File classesFloder = new File(webinFolder,"classes");
            //将classes下的全部文件加入URL，进行载入
            URL url = new URL("file:"+classesFloder.getAbsolutePath()+"/");
            this.addURL(url);

            //这个lib 的包的位置
            //是在web项目中webapp下的WEB-INF中的lib子目录的位置
            //所以web项目里的lib，要放到WEN-INF的lib中，这样就可以在Tomcat中自动加载

            File libFolder = new File(webinFolder,"lib");
            //遍历当前lib目录，以及lib目录下的子目录
            //即将lib目录中的jar全部取出
            List<File> jarFiles = FileUtil.loopFiles(libFolder);

            for(File file:jarFiles){
                url = new URL("file:" + file.getAbsolutePath());
                this.addURL(url);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void stop(){
        try{
            this.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
