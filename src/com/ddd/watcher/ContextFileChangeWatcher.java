package com.ddd.watcher;

import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.log.LogFactory;
import com.ddd.catalina.Context;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

public class ContextFileChangeWatcher {
    private WatchMonitor monitor;

    //标记是否已经暂停
    private boolean stop = false;

    public ContextFileChangeWatcher(Context context){
        //第一个参数为监听的文件夹
        //第二个参数代表监听的深入，如果是0、1，就表示只监听当前目录，而不监听子目录
        //第三个参数为当有文件发生变化，就会访问Watcher对应的方法
        this.monitor = WatchUtil.createAll(context.getDocBase(), Integer.MAX_VALUE, new Watcher() {
            @Override
            public void onCreate(WatchEvent<?> watchEvent, Path path) {
                //产生新文件的时候
                dealWith(watchEvent);
            }

            @Override
            public void onModify(WatchEvent<?> watchEvent, Path path) {
                //有文件修改的时候
                dealWith(watchEvent);
            }

            @Override
            public void onDelete(WatchEvent<?> watchEvent, Path path) {
                //有文件删除的时候
                dealWith(watchEvent);
            }

            @Override
            public void onOverflow(WatchEvent<?> watchEvent, Path path) {
                //有文件出错的时候
                dealWith(watchEvent);
            }

            //处理文件的函数
            private void dealWith(WatchEvent<?> event){
                //由于进行文件操作的时候，防止多条线程同时操作
                //所以这里要同步本类，防止产生脏数据
                synchronized (ContextFileChangeWatcher.class){
                    String fileName = event.context().toString();

                    if(stop)
                        return;

                    //只监听，jar包，class文件,xml文件。三种类型的文件
                    if(fileName.endsWith(".jar")||fileName.endsWith(".class")||fileName.endsWith(".xml")){

                        //告知后续的变动无需再修改
                        stop = true;
                        LogFactory.get().info(ContextFileChangeWatcher.this +"检测到了Web应用下的重要文件变化{}",fileName);

                        //如果发现相关文件，就要进行重载操作
                        context.reload();
                    }
                }
            }
        });

        //设置守护线程。
        this.monitor.setDaemon(true);
    }

    //开启文件的监听器
    public void start(){
        monitor.start();
    }

    //关闭文件的监听器
    public void stop(){
        monitor.close();
    }

}
