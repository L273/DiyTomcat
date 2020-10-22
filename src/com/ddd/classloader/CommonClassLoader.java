package com.ddd.classloader;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;


//放到服务器的主线程中进行加载
//所以，之后的类加载，都是要经过这个加载器的
//所以这个是服务器的默认加载器
//即，这个加载器是加载服务器的lib中的jar包
public class CommonClassLoader extends URLClassLoader{
    public CommonClassLoader(){
        //无参构造，要取父类的URL来定义
        super(new URL[]{});

        //应为这里的类都是默认放在lib中。
        //所以也不选择传参定位
        //默认扫描该文件夹即可

        try{
            //得到项目的位置
            File workingFolder = new File(System.getProperty("user.dir"));
            File libFolder = new File(workingFolder,"lib");

            //列出所有的文件，只扫一层。
            //所以lib中的目录是没用的
            File[] jarFiles = libFolder.listFiles();

            for (File file:jarFiles){
                if(file.getName().endsWith(".jar")){
                    //如果找到符合条件的jar包，就丢进URL中
                    URL url = new URL("file:"+file.getAbsolutePath());
                    //可以看到lib导入的包
                    //System.out.println("URL:"+url);
                    this.addURL(url);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
