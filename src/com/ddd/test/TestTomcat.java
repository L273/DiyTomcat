package com.ddd.test;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import com.ddd.catalina.Engine;
import com.ddd.catalina.Server;
import com.ddd.catalina.Service;
import com.ddd.util.Constant;
import com.ddd.util.MiniBrowsbr;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestTomcat {

    //两个默认参数
    private static int port = 18080;
    private static String ip = "127.0.0.1";


    //测试前执行，主要是检查端口
    //如果占用了端口，就说明启动了MyTomcat
    @BeforeClass
    public static void beforeClass(){
        //测试前看diy tomcat是否已经启动了
        if(NetUtil.isUsableLocalPort(port)){
            System.err.println("请先启动 位于端口："+port+"的diy tomcat，否则无法进行单元测试");
//            System.exit(1);
        }
        else {
            System.out.println("检测到 diy tomcat已经启动，开始进行单元测试");
        }
    }


    @Test
    public void testHelloTomcat(){
        String html = getContentString("/");
        System.out.println(html);
    }

    @Test
    public void testHtml(){
        String html = getContentString("/a.html");
        System.out.println(html);
    }

    @Test
    public void testTimeConsumeHtml() throws InterruptedException{
        //线程的基本个数、最大数量、空闲回收的最大等待时间、时间单位、任务的列表
        ThreadPoolExecutor poolExecutor =
                new ThreadPoolExecutor(20,20,60, TimeUnit.SECONDS,
                        new LinkedBlockingDeque<Runnable>(10));


        //通过DateUtil得到一个计时器
        TimeInterval interval = DateUtil.timer();

        for(int i = 0 ; i<3 ; i++){

            poolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    getContentString("/timeConsume.html");
                }
            });
        }
        //停止接受外部丢进来的任务，内部正在跑的线程，依旧会执行完
        poolExecutor.shutdown();

        //当前线程阻塞（main）
        // 直到线程池中所有提交的任务执行完，或者超过限定的时间
        // 或者线程被中断，抛出InterruptedException
        poolExecutor.awaitTermination(1,TimeUnit.HOURS);

        //Ms为打印的毫秒数
        long duration = interval.intervalMs();

        System.out.println(duration);
    }

    @Test
    public void testAIndex(){
        String  html;
//        html = getContentString("/a/index.html");
//        System.out.println(html);
//        System.out.println("===========================");
//
//        html = getContentString("/b/index.html");
//        System.out.println(html);
//        System.out.println("===========================");

//        html = getContentString("/test.pdf");
//        System.out.println(html.substring(0,100));
//        System.out.println("===========================");


//        html = getContentString("/j2ee/hello");
//        System.out.println(html);
//
//        System.out.println("===========================");
        html = getContentString("/javaweb/hello");
        System.out.println(html);
    }


    private String getContentString(String uri){
        //通过工具StrUtil来格式化字符串，然后赋值到url上
        String url = StrUtil.format("http://{}:{}{}",ip,port,uri);

        //得到http的响应报文
        String content = MiniBrowsbr.getContentString(url);

        return content;
    }

//    @Test
//    public void testSession()throws Exception{
//        String jsessionid = getContentString("/javaweb/hello");
//        if(null!=jsessionid)
//            jsessionid = jsessionid.trim();
//        String url = StrUtil.format("http://{}:{}{}", ip,port,"/javaweb/hello");
//        URL u = new URL(url);
//        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
//        conn.setRequestProperty("Cookie","JSESSIONID="+jsessionid);
//        conn.connect();
//        InputStream is = conn.getInputStream();
//        String html = IoUtil.read(is, "utf-8");
//    }

//    @Test
//    public void test(){
//        System.out.println(StrUtil.subBetween("/abc/123/456","/","/"));
//        System.out.println(StrUtil.removePrefix("/123/456","/"));
//    }

//    @Test
//    public void test1(){
//        String url = " http://www.baidu.com?id=2?id=3 ";
//        System.out.println(StrUtil.subBefore(url,'?',true));
//        System.out.println(StrUtil.subBetween(url," "," "));
//    }

//    @Test
//    public void test(){
//        {}为一个个赋值的意思
//        Engine[] engines = new Engine[]{new Engine(new Service(new Server()))};
//    }
}
