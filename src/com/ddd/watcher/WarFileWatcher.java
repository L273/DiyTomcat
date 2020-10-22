package com.ddd.watcher;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import com.ddd.catalina.Host;
import com.ddd.util.Constant;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

public class WarFileWatcher {
    private WatchMonitor monitor;
    public WarFileWatcher(Host host){
        //监听所有事件，监听的深度最大为1，然后建立一个新的监听器
        //监听的目录为Tomcat目录下的webapps
        this.monitor = WatchUtil.createAll(Constant.webappsFolder, 1, new Watcher() {

            @Override
            public void onCreate(WatchEvent<?> watchEvent, Path path) {
                dealEvent(watchEvent,path);
            }

            @Override
            public void onModify(WatchEvent<?> watchEvent, Path path) {
                dealEvent(watchEvent,path);
            }

            @Override
            public void onDelete(WatchEvent<?> watchEvent, Path path) {
                dealEvent(watchEvent,path);
            }

            @Override
            public void onOverflow(WatchEvent<?> watchEvent, Path path) {
                dealEvent(watchEvent,path);
            }

            private void dealEvent(WatchEvent<?> watchEvent, Path path){
                //将本身这个类进行同步处理
                synchronized (WarFileWatcher.class){
                    //得到监控的文件名称
                    String fileName = watchEvent.context().toString();

                    //如果检测到是.war文件的话
                    if(fileName.toLowerCase().endsWith(".war") && ENTRY_CREATE.equals(watchEvent.kind())){
                        //重新加载war文件
                        File warFile = FileUtil.file(Constant.webappsFolder,fileName);
                        host.loadWar(warFile);
                    }
                }
            }
        });
    }

    public void start(){
        monitor.start();
    }

    public void stop(){
        monitor.interrupt();
    }
}
