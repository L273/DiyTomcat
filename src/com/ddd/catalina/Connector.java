package com.ddd.catalina;

import cn.hutool.log.LogFactory;
import com.ddd.http.Request;
import com.ddd.http.Response;
import com.ddd.util.ThreadPoolUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Connector implements Runnable{
    //定义一个服务引擎
    private Service service;

    //定义使用的端口
    private int port;

    //四个压缩的参数，存储在xml文件中
    private String compression;
    private int compressionMinSize;
    private String noCompressionUserAgents;
    private String compressableMimeType;


    public Connector(Service service){
        this.service = service;
    }

    public Service getService() {
        return service;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void init(){
        LogFactory.get().info("Initializing ProtocolHandler [http-bio-{}]",port);
    }

    public void start(){
        LogFactory.get().info("Starting ProtocolHandler [http-bio-{}]",port);

        //启动本线程
        new Thread(this).start();
    }

    @Override
    public void run() {
        try{
        /*
        如果产生了连接，则将数据以write的方式反馈给浏览器
         */
            ServerSocket serverSocket = new ServerSocket(port);

            while(true) {
                //得到连接过来的套接字
                Socket socket = serverSocket.accept();

                Runnable runnable = new Runnable(){
                    @Override
                    public void run() {
                        try {
                            //用一个Request来处理请求的头部
                            //Connector.this。表示在这个包含这个Runnable接口的类
                            Request request = new Request(socket,Connector.this);

                            //使用封装的response进行返回
                            Response response = new Response();

                            //将Request和Response丢到HttpProcessor中处理
                            HttpProcessor httpProcessor = new HttpProcessor();

                            httpProcessor.execute(socket,request,response);

                        }catch (IOException e){
                            //发生于接受的sokcet生成的时候
                            e.printStackTrace();
                        }finally {
                            if(!socket.isConnected())
                                try{
                                    socket.close();
                                }catch (IOException e1){
                                    e1.printStackTrace();
                                }
                        }
                    }
                };
                ThreadPoolUtil.run(runnable);
            }
        }catch (IOException e){
            //发生于服务器Socket生成的时候

            //日志记录错误信息
            LogFactory.get().error(e);

            //控制台输出错误信息
            e.printStackTrace();
        }
    }


    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public int getCompressionMinSize() {
        return compressionMinSize;
    }

    public void setCompressionMinSize(int compressionMinSize) {
        this.compressionMinSize = compressionMinSize;
    }

    public String getNoCompressionUserAgents() {
        return noCompressionUserAgents;
    }

    public void setNoCompressionUserAgents(String noCompressionUserAgents) {
        this.noCompressionUserAgents = noCompressionUserAgents;
    }

    public String getCompressableMimeType() {
        return compressableMimeType;
    }

    public void setCompressableMimeType(String compressableMimeType) {
        this.compressableMimeType = compressableMimeType;
    }
}
