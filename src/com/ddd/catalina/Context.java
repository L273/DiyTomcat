package com.ddd.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import com.ddd.classloader.WebappClassLoader;
import com.ddd.exception.WebConfigDuplicatedException;
import com.ddd.http.ApplicationContext;
import com.ddd.http.StandardServletConfig;
import com.ddd.util.ContextXMLUtil;
import com.ddd.watcher.ContextFileChangeWatcher;
import org.apache.jasper.JspC;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import sun.reflect.misc.FieldUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.rmi.server.ServerCloneException;
import java.util.*;


//一个path对饮webapps下一个目录，即一个webapp
//所以，一个Context，就对应一个webapp应用
//所以，这里每生成一个Context，都要对应得到一个通用类加载器
//而这个通用类加载器，就是Webapp本身加载器的父类，在这里得到后赋值初始化（通用类加载器作为参数）。
public class Context {
    //访问的路径
    private String path;
    //文件再系统中的位置
    private String docBase;

    //导入context的xml文件
    private File contextWebXmlFile;

    //1. 由url映射类名
    //2. 由url映射servletName
    //3. Servlet 名字到类的映射
    //4. Servlet 类到名字的映射
    private Map<String,String> url_servletClassName;
    private Map<String,String> url_servletName;
    private Map<String,String> servletName_className;
    private Map<String,String> className_serlvetName;

    //根据servlet的name得到init的参数
    private Map<String ,Map<String ,String >> servletName_initParams;

    //每一个应用，都应该有自己独立的WebappClassLoader
    private WebappClassLoader webappClassLoader;

    //一个应用，应该有一个专门的文件监听器
    private Host host;
    private boolean reloadable;
    private ContextFileChangeWatcher contextFileChangeWatcher;

    //每一个应用，应该有一个专属的Servlet容器
    private ServletContext servletContext;

    //用一个池子来装Servlet
    private Map<Class<?>,HttpServlet> servletPool;

    //用一个列表来装需要自启动的类
    private List<String> loadOnStratupClassNames;

    //一些和Filter有关的参数
    private Map<String,List<String>> url_filterClassName;
    private Map<String,List<String>> url_filterNames;
    private Map<String,String> filterName_className;
    private Map<String,String> className_filterName;
    private Map<String,Map<String,String>> filterClassName_initParams;

    //存储filter的集合框架
    private Map<String,Filter> filterPool;

    //Listenters列表
    private List<ServletContextListener> listeners;

    public Context(String path,String docBase,Host host,boolean reloadable){
        TimeInterval timeInterval = new TimeInterval();

        this.path = path;
        this.docBase = docBase;

        //连个监听器的属性
        this.host = host;
        this.reloadable = reloadable;

        //即得到conf.xml配置文件中，映射的web.xml配置文件的路径信息
        this.contextWebXmlFile = new File(docBase, ContextXMLUtil.getWatchedResource());

        this.url_servletClassName = new HashMap<>();
        this.url_servletName = new HashMap<>();
        this.servletName_className = new HashMap<>();
        this.className_serlvetName = new HashMap<>();

        this.servletName_initParams = new HashMap<>();

        this.url_filterClassName= new HashMap<>();
        this.url_filterNames = new HashMap<>();
        this.filterName_className = new HashMap<>();
        this.className_filterName = new HashMap<>();
        this.filterClassName_initParams = new HashMap<>();

        this.filterPool = new HashMap<>();

        //利用自定义的Servlet容器初始化Servlet容器
        this.servletContext = new ApplicationContext(this);

        //初始化存放servlet的池子
        this.servletPool = new HashMap<>();

        //初始化自启动列表
        this.loadOnStratupClassNames = new ArrayList<>();

        //初始化listeners的表
        this.listeners = new ArrayList<ServletContextListener>();

        //得到在bootstrap中设置的commonClassLoader加载器
        ClassLoader commonClassLoader = Thread.currentThread().getContextClassLoader();
        //初始化该类的类加载器
        this.webappClassLoader = new WebappClassLoader(docBase,commonClassLoader);

        //打印加载的时间
        deploy();
    }

    private void deploy(){

        //加载监听器，目前这里只实现了Context的监听器
        loadListeners();

        TimeInterval timeInterval = DateUtil.timer();

        //得到web app的目录位置
        LogFactory.get().info("Deploying web application directory {}",this.docBase);

        init();

        if(reloadable){

            //根据本应用的docBase设置文件监听器的位置
            //定义本类的监听器，一个程序一个监听器
            //这个监听器里的重载文件只用一次，因为，重载的时候，Context会刷新
            //相应的监听器也会刷新，所以，监听器使用一次就刚刚好
            this.contextFileChangeWatcher = new ContextFileChangeWatcher(this);

            contextFileChangeWatcher.start();
        }

        //得到目录加载所使用的时间
        LogFactory.get().info("Deploying of web application directory {} has finished in {} ms",
                this.docBase,timeInterval.intervalMs());

        //对JspRuntimeContext初始化
        // 为了能够在jsp转化java文件里的javax.servlet.jsp.JspFactory.getDefaultFactory() 这行能够有返回值
        JspC jspC = new JspC();
        new JspRuntimeContext(servletContext,jspC);

    }

    //本函数的功能是初始化Servlet相关的xml配置文件
    private void init(){
        //如果没有
        if(!contextWebXmlFile.exists())
            return;

        try{
            //先查重
            checkDulicate();
        }catch (WebConfigDuplicatedException e){
            e.printStackTrace();
            return;
        }

        //得到context.xml所指向的文件
        String xml = FileUtil.readUtf8String(contextWebXmlFile);

        Document document = Jsoup.parse(xml);

        //遍历所有的Servlet标签
        parseServletMapping(document);

        //遍历初始的参数
        parseServletInitParams(document);

        //读取filter的设置
        parseFilterMapping(document);
        parseFilterInitParams(document);

        //初始化filter
        initFilter();

        //设置自启动列表
        parseLoadOnStartUp(document);
        handleLoadOnStartUp();

        //说明Contextinit了
        fireEvent("init");
    }

    //加载filter
    //使得className和Filter挂钩，于filterPool中
    //即初始化filterPool
    private void initFilter(){
        Set<String> classNames = className_filterName.keySet();
        for(String className : classNames){
            try{
                //加载相关的Filter类对象
                Class clazz = this.getWebappClassLoader().loadClass(className);

                //得到初始化的参数
                Map<String,String> initParameters = filterClassName_initParams.get(className);

                //并得到filer中的name标签的值
                String filterName = className_filterName.get(className);

                //将数据交给专门处理参数的FilterConfig
                FilterConfig filterConfig = new StandardFilterConfig(servletContext,filterName,initParameters);

                //从filterPool中得到filter
                //如果没有找到，就说明要重新声明相关的值，同时将新的filter推到pool中
                Filter filter = filterPool.get(clazz);
                if(filter==null){
                    filter = (Filter) ReflectUtil.newInstance(clazz);

                    //如果一开始没有这个filter的话，就要对其先初始化
                    filter.init(filterConfig);
                    filterPool.put(className,filter);
                }
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        }
    }

    //得到一系列符合匹配模式的filter
    //即，过滤得到url书写模式符合条件的filter
    public List<Filter> getMatchedFilters(String uri){
        //Filter的映射
        List<Filter> filters = new ArrayList<>();
        //其中patterns中存储的是url的集合
        Set<String> patterns = url_filterClassName.keySet();

        Set<String> matchedPatterns = new HashSet<>();

        //如果传入的url和uri匹配的话
        //那么，就将该url丢到匹配好的pattern中
        for(String pattern : patterns){
            if(match(pattern,uri))
                matchedPatterns.add(pattern);
        }

        //matchedFilterClassNames中存放names，即处理好的names
        Set<String> matchedFilterClassNames = new HashSet<>();

        //如果在该url下，找到相关的name集合，就一并放入该集合中
        for(String pattern:matchedPatterns){
            List<String> filterClassName = url_filterClassName.get(pattern);
            matchedFilterClassNames.addAll(filterClassName);
        }

        //将处理好的name遍历，然后往pool中添加相关的filter
        for(String filterClassName : matchedFilterClassNames){
            Filter filter = filterPool.get(filterClassName);
            filters.add(filter);
        }

        return filters;
    }

    private boolean match(String pattern,String uri){
        //完全匹配
        if(StrUtil.equals(pattern,uri))
            return true;

        // /*模式
        if(StrUtil.equals(pattern,"/*"))
            return true;

        // *.尾椎模式
        if(StrUtil.startWith(pattern,"/*.")){
            String patternExtName = StrUtil.subAfter(pattern,".",false);
            String uriExtName = StrUtil.subAfter(uri,".",false);
            if(StrUtil.equals(patternExtName,uriExtName))
                return true;
        }

        return false;
    }

    //检查是否由重复的，如果由重复的，则抛出异常处理
    private void checkDulicate()
        throws WebConfigDuplicatedException{
        String xml  = FileUtil.readUtf8String(contextWebXmlFile);
        Document document = Jsoup.parse(xml);

        checkDulicated(document,"servlet-mapping url-pattern","url-pattern重复，请保持其唯一性:{}");
        checkDulicated(document,"servlet servlet-name","servlet-name重复，请保持其唯一性:{}");
        checkDulicated(document,"servlet servlet-class","servlet-class重复，请保持其唯一性:{}");

        //查filter的唯一性
        checkDulicated(document,"filter filter-name","filter-name重复，请保持其唯一性:{}");
        checkDulicated(document,"filter filter-class","filter-class重复，请保持其唯一性:{}");
    }

    //检查标签的唯一性
    //如果发现错误，则向上抛出异常
    private void checkDulicated(Document document,String mapping,String desc)
        throws WebConfigDuplicatedException{

        //查询要检查唯一性的标签
        Elements elements = document.select(mapping);

        //存要检查标签的内容
        List<String> contexts = new ArrayList<>();

        //取内容
        for(Element element:elements){
            contexts.add(element.text());
        }

        //进行排序
        Collections.sort(contexts);

        //由于是有序的结果，那么，相同的字符串一定放在临近的位置
        for(int i = 0;i<contexts.size() - 1;i++){
            String contentPre = contexts.get(i);
            String contentNext = contexts.get(i+1);

            //若相邻存在相同，则说明存在重复的标签
            if(contentPre.equals(contentNext))
                throw new WebConfigDuplicatedException(StrUtil.format(desc,contentPre));
        }

    }

    private void parseServletMapping(Document document){
        //封装servlet里两个mapping映射
        Elements servletElements = document.select("servlet");
        if (!servletElements.isEmpty()){
            //遍历servlet标签下的数据
            for(Element servletElement : servletElements){
                //只有一个servlet-Name
                String servletName = servletElement.select("servlet-name").first().text();

                //只有一个servlet-Class
                String servletClass= servletElement.selectFirst("servlet-class").text();

                servletName_className.put(servletName,servletClass);
                className_serlvetName.put(servletClass,servletName);
            }
        }

        //封装servlet-mapping里的映射
        Elements mappingElements = document.select("servlet-mapping");

        if(!mappingElements.isEmpty()){
            for(Element mappingElement : mappingElements){
                //得到ServletName的值
                String servletName = mappingElement.select("servlet-name").first().text();

                //得到url-pattern的值
                String urlPattern = mappingElement.selectFirst("url-pattern").text();

                url_servletName.put(urlPattern,servletName);
                url_servletClassName.put(urlPattern,servletName_className.get(servletName));
            }
        }
    }

    //初始化servletName到initParams的映射
    private void parseServletInitParams(Document document){
        Elements servlets = document.select("servlet");

        if(servlets.isEmpty())
            return;

        for(Element servlet:servlets){

            Elements initParams = servlet.select("init-param");

            //如果没有值，则进行下一个Servlet的遍历
            if(initParams.isEmpty())
                continue;

             Map<String,String> Params = new HashMap<>();

            //但初始参数可能有多个，所以这里要一一遍历
            for(Element initParam : initParams){
                //参数名字
                String paramName = initParam.selectFirst("param-name").text();

                //参数值
                String paramValue = initParam.selectFirst("param-value").text();

                Params.put(paramName,paramValue);
            }

            //因为查重，所以这里只可能有一个servletName
            String servletName = servlet.select("servlet-name").first().text();

            this.servletName_initParams.put(servletName,Params);

        }
    }

    //初始化自启动的列表，即自启动的类的名字
    private void parseLoadOnStartUp(Document document){
        Elements elements = document.select("load-on-startup");
        for(Element element:elements){
            //从父类中得到class的名字
            String loadOnStartupServletClassNmae = element.parent().select("servlet-class").text();

            this.loadOnStratupClassNames.add(loadOnStartupServletClassNmae);
        }
    }

    //解析FilterMap
    public void parseFilterMapping(Document document){
        //初始化url_filterNames
        Elements elements = document.select("filter-mapping url-pattern");
        if(!elements.isEmpty()){
            for(Element element:elements){
                String urlPattern = element.text();
                String filterName = element.parent().select("filter-name").first().text();

                List<String> filterNames = url_filterNames.get(urlPattern);
                if(filterNames==null){
                    filterNames = new ArrayList<>();
                    url_filterNames.put(urlPattern,filterNames);
                }
                filterNames.add(filterName);
            }
        }

        //初始化filterName与filterClass之间的关系
        elements = document.select("filter");
        if(!elements.isEmpty()){
            for (Element element:elements) {
                String filterName = element.select("filter-name").first().text();
                String filterClass = element.select("filter-class").first().text();
                filterName_className.put(filterName, filterClass);
                className_filterName.put(filterClass, filterName);
            }
        }

        //初始化url和filterClassName
        //由于这里url和filterName已经有了相关的对应关系。
        //所以，只要用一种方式，将filterName改成filterClassName即可
        Set<String> urls = url_filterNames.keySet();

        if(urls.size()==0)
            return;

        for(String url:urls){
            List<String> filterNames = url_filterNames.get(url);
            if(filterNames == null){
                continue;
            }

            //这里的过程和赋值filterName类似
            for (String filterName:filterNames){
                //利用Name得到Class然后操作列表
                String filterClassName = filterName_className.get(filterName);
                List<String> filterClassNames = url_filterClassName.get(url);
                if(filterClassNames==null){
                    filterClassNames = new ArrayList<>();
                    url_filterClassName.put(url,filterClassNames);
                }
                filterClassNames.add(filterClassName);
            }
        }
        for(String a:url_filterClassName.keySet()){
            System.out.println(a);
        }

    }

    private void parseFilterInitParams(Document document){
        Elements elements = document.select("filter");

        if(elements.isEmpty())
            return;

        for (Element element:elements){
            Elements params = element.select("init-param");

            //如果为空的话，就返回继续执行
            if(params.isEmpty())
                continue;

            Map initParams = new HashMap();
            for (Element param:params){
                String paramName = param.select("param-name").first().text();
                String paramValue = param.select("param-value").first().text();
                initParams.put(paramName,paramValue);
            }

            String filterClassName = element.select("filter-class").first().text();

            filterClassName_initParams.put(filterClassName,initParams);
        }
    }


    //根据自加载列表里的类名来一一生成新类
    private void handleLoadOnStartUp(){
        //如果列表没有值的话，就不执行
        if(loadOnStratupClassNames.size()==0)
            return;
        for(String loadOnStartupServletClassName : loadOnStratupClassNames){

            try{
                Class<?> clazz = webappClassLoader.loadClass(loadOnStartupServletClassName);
                getServlet(clazz);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void reload(){
        //重载对象，因为目录文件有所修改，但是这个修改应该在host进行
        //因为host有扫描文件的函数

        host.reload(this);

    }

    //从Servlet池中得到Servlet
    public synchronized HttpServlet getServlet(Class<?> clazz)
            throws InstantiationException,IllegalAccessException,ServletException{

        HttpServlet servlet = servletPool.get(clazz);

        //如果没有根据该类对象找到Servlet
        //即首次，就要生成新生成一个servlet并放到SerlvetPool中
        if(servlet==null){
            servlet = (HttpServlet)clazz.newInstance();
            ServletContext servletContext = this.getServletContext();

            //拿到类的名字
            String className = clazz.getName();
            String servletName = className_serlvetName.get(className);


            //得到该servlet再web.xml中的init标签的值
            Map<String,String> initParameters = servletName_initParams.get(servletName);
            ServletConfig servletConfig = new StandardServletConfig(servletContext,servletName,initParameters);

            Set<String> s = servletName_initParams.keySet();
            for(String a : s){
                System.out.println(a);
            }

            //给servlet的init中传递init的参数
            servlet.init(servletConfig);
            servletPool.put(clazz,servlet);
        }
        return servlet;
    }

    //销毁servlet的池
    public void destroyServlets(){
        //将池子里的servlet全部取出，是value位置上的所有值
        Collection<HttpServlet> servlets = this.servletPool.values();

        //依次取出并destory
        for (HttpServlet servlet:servlets){
            servlet.destroy();
        }
    }

    //其中
    public void stop(){
        //关闭加载器和监听器
        webappClassLoader.stop();
        contextFileChangeWatcher.stop();

        //并清空servlet池子
        destroyServlets();

        fireEvent("destroy");
    }

    //添加Listenter监听器
    public void addListener(ServletContextListener listener){
        listeners.add(listener);
    }

    private void loadListeners(){
        try {
            //如果webapp下的xml不存在的话，返回
            if(!contextWebXmlFile.exists())
                return;

            String xml = FileUtil.readUtf8String(contextWebXmlFile);
            Document document = Jsoup.parse(xml);

            //遍历listenter下的class并用加载器生成相关的类
            Elements elements = document.select("listener listener-class");

            for (Element element:elements){
                String litenerClassName = element.text();

                Class<?> clazz = this.getWebappClassLoader().loadClass(litenerClassName);
                ServletContextListener listener = (ServletContextListener)clazz.newInstance();

                //将加载得到的listener放到listenter列表中
                addListener(listener);
            }
        }catch (Exception e){
            throw  new RuntimeException(e);
        }
    }

    private void fireEvent(String type){
        ServletContextEvent event = new ServletContextEvent(servletContext);
        //开始对时间进行监听
        for (ServletContextListener listener:this.listeners){
            if("init".endsWith(type))
                listener.contextInitialized(event);
            if("destroy".equals(type))
                listener.contextDestroyed(event);
        }
    }

    public String getServletClass(String uri){
        return url_servletClassName.get(uri);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDocBase() {
        return docBase;
    }

    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }

    public WebappClassLoader getWebappClassLoader() {
        return webappClassLoader;
    }

    public boolean isReloadable() {
        return reloadable;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    public ServletContext getServletContext() {
        return this.servletContext;
    }

    //返回WebClassLoader
    public WebappClassLoader getWebClassLoader(){
        return webappClassLoader;
    }
}
