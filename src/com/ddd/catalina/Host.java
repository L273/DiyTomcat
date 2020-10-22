package com.ddd.catalina;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import com.ddd.util.Constant;
import com.ddd.util.ServerXMLUtil;
import com.ddd.watcher.WarFileWatcher;

import javax.servlet.FilterChain;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Host {
    private String name;

    //一个 contextMap 用于存放路径和Context 的映射。
    private Map<String,Context> contextMap;

    private Engine engine;

    public Host(String name,Engine engine){
        //初始化变量
        this.contextMap = new HashMap<>();
        this.name = name;
        this.engine = engine;

        //扫描相关的文件
        // 同时扫描XML文件内的标签内容
        scanContextsOnWebAppsFolder();
        scanContextsInServerXML();
        scanWarOnWebAppsFolder();

        //启动war文件变化而动作的监听器
        new WarFileWatcher(this).start();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Context> getContextMap() {
        return contextMap;
    }

    public void setContextMap(Map<String, Context> contextMap) {
        this.contextMap = contextMap;
    }

    //遍历webapps下的全部目录
    //如果遇到目录，则继续往下遍历。
    //如果遇到文件，则使用函数进行加载。
    private void scanContextsOnWebAppsFolder(){
        //从Constant的目录中，即
        File[] files = Constant.webappsFolder.listFiles();
        for(File file : files){
            if(!file.isDirectory()) {
                //如果不是目录，则重新循环计算
                continue;
            }

            //如果是目录，则载入目录
            loadContext(file);
        }
    }

    //载入目录的函数
    //并对相关的目录进行封装
    //即看这个过程也能明白，path就是webapps目录下的子目录
    //然后ROOT目录默认为/，其他都是/+目录名
    //当然，一个path，可以存放一个web应用
    //所以，一个Context的path，就要一个webapp单独的类加载器
    private void loadContext(File folder){
        String path = folder.getName();

        //区别是根目录，还是一般的目录
        if("ROOT".equals(path))
            path = "/";
        else
            path = "/" + path;

        //得到文件路劲在服务器的目录位置
        String docBase = folder.getAbsolutePath();

        //初始化Context类的路径，还有该路径对应在服务器的文件路径
        //应用默认是支持热加载的
        Context context = new Context(path,docBase,this,true);

        //一个 contextMap 用于存放路径和Context 的映射。
        contextMap.put(context.getPath(),context);
    }


    //即可以得到在Server.xml配置表里写好的配置文件的所在地
    private void scanContextsInServerXML(){
        //得到xml文件里配置的配置信息
        List<Context> contexts = ServerXMLUtil.getContexts(this);

        for (Context context : contexts){
            //初始化contextMap的列表
            contextMap.put(context.getPath(),context);
        }
    }

    //本质上是根据传入的context，重新生成一个context
    public void reload(Context context){
        LogFactory.get().info("Reloading Context with name [{}] has started",context.getPath());

        String path = context.getPath();
        String docBase = context.getDocBase();
        boolean reloadable = context.isReloadable();

        //首先停掉之前的应用
        context.stop();

        //再移掉之前的应用的path
        contextMap.remove(path);

        //创建一个新的项目，应为还是在本Host上的，所以Host参数传自己就可以了
        //而后重载，就会在context的构造器中，遍历相关的xml文件。同时在加载器中，重新扫描classes以及一些lib文件
        Context newContext = new Context(path,docBase,this,reloadable);

        //将新生成的context加入contextMap
        contextMap.put(newContext.getPath(),newContext);

        //打印消耗的时间
        LogFactory.get().info("Reloading Context with name[{}] has completed",context.getPath());
    }

    public Context getContext(String path){
        //通过contextMap得到context的内容
        return contextMap.get(path);
    }

    //从webapps目录下，扫描war文件，并用loadWar进行处理
    public void scanWarOnWebAppsFolder(){
        File folder = FileUtil.file(Constant.webappsFolder);
        File[] files = folder.listFiles();
        for(File file:files){
            //如果找到了以war结尾的文件，则进行相关的操作
            if(file.getName().toLowerCase().endsWith(".war"))
                loadWar(file);
        }
    }

    //解压war文件
    public void loadWar(File warFile){
        //首先得到war文件的文件名
        String fileName = warFile.getName();

        //对文件的名字进行处理，得到.之前的名字
        //即文件的名字，并非文件的尾缀名字
        String folderName = StrUtil.subBefore(fileName,".",true);

        //先尝试看能不能从这个名字，即path对应得到一个Context
        Context context = getContext("/"+folderName);

        //如果得到了Context，说明该war文件有对应的web工程文件，即该war不需要再解压
        if(context!=null)
            return;

        //同理如果在webapps目录下找到相关的目录
        //即，也进行相关的跳转处理
        File folder = new File(Constant.webappsFolder,folderName);
        if(folder.exists())
            return;

        //如果前面的检查都通过了
        //就说明war文件对应的项目目录并不存在。所以这时候就要解压文件
        //首先一步，就是建立相应的文件夹
        File tempWarFile = FileUtil.file(Constant.webappsFolder,folderName,fileName);
        File contextFolder = tempWarFile.getParentFile();

        //创建相关文件夹
        contextFolder.mkdirs();

        //将war文件放到contextFolder的文件夹下
        // 即tempWarFile指向的文件
        FileUtil.copyFile(warFile,tempWarFile);

        //对contextFolder里的war文件进行解压
        String command = "jar -xvf " + fileName;
        Process process = RuntimeUtil.exec(null,contextFolder,command);

        //等待程序执行完成
        try{
            process.waitFor();
        }catch (InterruptedException e){
            e.printStackTrace();
        }

        //解压之后删除临时war
        tempWarFile.delete();

        //然后创建新的Context
        load(contextFolder);
    }

    //加载文件
    //即加载war文件解压生成的项目目录所在的位置的工程文件
    //然后put到Map里
    public void load(File folder){
        String path = folder.getName();
        if("ROOT".equals(path))
            path = "/";
        else
            path = "/" + path;

        String docBase = folder.getAbsolutePath();
        Context context = new Context(path,docBase,this,false);
        contextMap.put(context.getPath(),context);
    }


}
