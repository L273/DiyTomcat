package com.ddd.util;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MiniBrowsbr {
    public static void main(String[] args) throws Exception{
        String url = "http://static.how2j.cn/diytomcat.html";
//        String contentString= getContentString(url,false);
//        System.out.println(contentString);
        String httpString= getHttpString(url,false);
        System.out.println(httpString);
    }

    /*
            以下四个getHttp是得到http响应的全部报文
     */

    public static String getHttpString(String url,boolean gzip){
        byte[] bytes = getHttpBytes(url,gzip);
        return new String(bytes).trim();
    }

    public static String getHttpString(String url){
        return getHttpString(url,false);
    }

    public static byte[] getHttpBytes(String url,boolean gzip){
        byte[] result = null;

        try{
            URL u = new URL(url);

            //建立一个客户端的Socket，之后用这个Socket去连接URL配置的服务器
            Socket client = new Socket();

            //得到URL的端口
            int port = u.getPort();
            //如果没有找到端口，就将端口设置为80端口
            if(port==-1)
                port=80;

            //传入主机名，还有端口
            //该类的作用，仅仅只是封装主机名和端口
            InetSocketAddress inetSocketAddress = new InetSocketAddress(u.getHost(),port);

            //用客户端的socket去连接这个新地址
            //用1秒的时间建立连接，如果一秒以内产生异常，则抛出该异常
            client.connect(inetSocketAddress,1000);

            //定义请求头的Map
            Map<String,String> requestHeaders = new HashMap<>();

            //载入头部的数据
            requestHeaders.put("Host",u.getHost()+":"+port);
            requestHeaders.put("Accept","text/html");
            requestHeaders.put("Connection","close");
            requestHeaders.put("User-Agent","how2j mini brower / java1.8");

            //如果有gzip的压缩要求，那么，这一项也压入
            if(gzip)
                requestHeaders.put("Accept-Encoding","gzip");

            //得到URL的
            String path = u.getPath();
            //如果路径为空，则默认进入根目录
            if(path.length()==0)
                path="/";

            //这里的GET后，以及HTTP前都要有空格
            String firstLine = "GET " + path + " HTTP/1.1\r\n";

            //请求头的缓存池
            StringBuffer httpRequestString = new StringBuffer();

            //压入请求头的首行
            httpRequestString.append(firstLine);

            //得到头的主键，并从相关头内获取信息，并将其加入http的缓冲池内
            Set<String> headers = requestHeaders.keySet();
            for (String header:headers){
                String headerLine = header + ":" + requestHeaders.get(header) + "\r\n";
                httpRequestString.append(headerLine);
            }

            //一个流的操作器，可以引出一系列有关流的操作的函数，并将流的对象定义为客户端Socket的Stream，同时自动刷新
            PrintWriter printWriter = new PrintWriter(client.getOutputStream(),true);

            //讲请求头写入客户端的发送流内
            //并发送
            printWriter.println(httpRequestString);


            /*
            发送完后，服务器就会返回数据，这里就要利用另一个接受流来处理得到的数据
             */


            //接受流，从客户端的Socket收到的数据
            InputStream inputStream = client.getInputStream();

            //调用函数，直接将流内的数据转换成byte数组
            result = readBytes(inputStream);

            client.close();

        }catch (Exception e){
            e.printStackTrace();
            try{
                //将得到的异常的字符串，以utf-8的方式，进行处理编码为字符数组
                result = e.toString().getBytes("utf-8");
            }catch (UnsupportedEncodingException e1){
                e1.printStackTrace();
            }
        }

        //将得到的客户端的字符数组返回
        return result;
    }


    /*
    下面四个，就是获取HTTP响应内容的函数
     */

    public static byte[] getContentBytes(String url){
        return getContentBytes(url,false);
    }

    public static byte[] getContentBytes(String url,boolean gzip){
        //先得到整体的HTTP响应报文
        byte[] response = getHttpBytes(url,gzip);

        byte[] doubleReturn = "\r\n\r\n".getBytes();

        int pos = -1;

        for(int i=0;i<response.length - doubleReturn.length;i++){
            //将数组response中i到i+doubleReturn的数据放到temp内
            byte[] temp = Arrays.copyOfRange(response,i,i+doubleReturn.length);

            if(Arrays.equals(temp,doubleReturn)){
                //如果匹配到两行换行符，则退出
                //并记录当下的pos，即指针的位置
                pos = i;
                break;
            }
        }

        if(pos==-1){
            //如果指针没有变化
            return null;
        }

        //将指针移动两个换行符的单位
        pos += doubleReturn.length;

        byte[] result =  Arrays.copyOfRange(response,pos,response.length);
        return  result;
    }


    public static String getContentString(String url){
        return getContentString(url,false);
    }

    public static String getContentString(String url,boolean gzip){
        byte[] result = getHttpBytes(url,gzip);

        if(result==null)
            return null;
        try {
            return new String(result,"utf-8");
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
            return null;
        }

    }


    /*
    一些常用的方法↓↓↓↓↓↓↓↓↓
     */

    private static byte[] readBytes(InputStream inputStream)
            throws IOException{
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int length = -1;
        while(true){
            length = inputStream.read(buffer);

            if(length==-1){
                //如果取不到数据，则退出循环
                break;
            }

            //将数据写入一个处理数组的输出流中
            byteArrayOutputStream.write(buffer,0,length);
            if(length!=bufferSize){
                //如果有空余，就说明到了最后一组
                break;
            }
        }
        byte[] result = byteArrayOutputStream.toByteArray();
        return result;
    }

    /*
    一些常用的方法↑↑↑↑↑↑↑↑↑
     */
}