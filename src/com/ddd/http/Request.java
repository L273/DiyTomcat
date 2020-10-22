package com.ddd.http;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.ddd.catalina.Connector;
import com.ddd.catalina.Context;
import com.ddd.catalina.Engine;
import com.ddd.catalina.Service;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

public class Request extends BaseRequest{
    private String requestString;
    private String uri;
    private Socket socket;

    private Context context;

    private Connector connector;

    //method参数，用于之后调用InvokerServlet内的Service进行判断
    private String method;

    //准备查询的字符串和参数Map
    private String queryString;
    private Map<String,String[]> parameterMap;

    //头部的Map
    private Map<String,String> hreaderMap;

    //请求中的Cookie列表
    private Cookie[] cookies;

    //创建session的属性
    private HttpSession session;

    //加上一个判断forwarded的参数，以便之后的判断是否进行跳转
    private boolean forwared;

    //生命attributesMap属性，用于存放参数
    private Map<String,Object> attributesMap;

    public Request(Socket socket,Connector connector)
            throws IOException{
        this.socket = socket;
        this.connector = connector;
        this.parameterMap = new HashMap<>();
        this.hreaderMap = new HashMap<>();
        this.attributesMap = new HashMap<>();

        //初始化请求头
        parseHttpRequest();
        if(StrUtil.isEmpty(requestString))
            return;

        //初始化Uri
        parseUri();

        //遍历目录内容
        parseContext();

        //得到请求头的方法
        parseMethod();

        //如果Context的路径不是"/"，则要对uri进行修正
        //即，uri保存为该目录下的资源相对位置
        if(!"/".equals(context.getPath())){
            //去除uri前面的path，即访问资源的相对目录
            //最后的uri就是去掉了项目目录的一个在项目目录内的相对文件定位符
            uri = StrUtil.removePrefix(uri,context.getPath());

            //没/会进行补充/，因为这个同理访问根目录的操作
            if(StrUtil.isEmpty(uri))
                uri = "/";
        }

        parseParameters();
        parseHeaders();
        parseCookies();
    }

    private void parseHttpRequest()
            throws IOException{
        InputStream inputStream = this.socket.getInputStream();
        byte[] bytes = readBytes(inputStream);
        requestString = new String(bytes,"utf-8");
    }

    private void parseUri(){
        String temp;

        //即get后面一部分
        temp = StrUtil.subBetween(requestString," "," ");

        if(!StrUtil.contains(temp,'?')){
            //如果URL里面没有?
            //就说明没有使用?进行传参
            //直接将temp输出即可
            uri = temp;
            return;
        }

        //截取字符串中，以?作为分隔符，截取后面的字符串，并非最后一个
        //即，从第一个?，开始向前截取
        temp = StrUtil.subBefore(temp,'?',false);
        uri = temp;
    }

    private void parseContext(){
        //从服务中单独提取出引擎
        Service service = connector.getService();
        Engine engine = service.getEngine();

        //拿到uri的映射的context
        //就说明uri的内容是目录（仅限一层的情况）
        context = engine.getDefaultHost().getContext(uri);
        if(context!=null)
            return;

        //截取第一个放在/ /中的字符串
        String path = StrUtil.subBetween(uri,"/","/");

        if(path==null)
            path = "/";
        else
            path = "/" + path;

        //从服务器存储的映射路径得到数据
        context = engine.getDefaultHost().getContext(path);

        if(context==null){
            //说明该path映射下无文件存在
            //自动调用根目录下的文件
            context = engine.getDefaultHost().getContext("/");
        }

    }

    private void parseMethod(){
        //截取第一个空格前的String，即头部的GET或者POST
        //所以，传递参数false,表明不是最后一个" "
        this.method = StrUtil.subBefore(requestString," ",false);
    }

    private void parseParameters(){
        //分GET和POST两种方式传递参数

        if("GET".equals(this.getMethod())){
            //去掉空格先
            String url = StrUtil.subBetween(requestString," "," ");

            if(StrUtil.contains(url,'?')){
                //从第一个?开始往后面取参数
                queryString = StrUtil.subAfter(url,'?',false);
            }
        }

        if("POST".equals(this.getMethod())){
            //得到内容后的全部数据
            //即POST查询，相关的参数，是放在请求头的后面
            queryString = StrUtil.subAfter(requestString,"\r\n\r\n",false);
        }

        //如果得到的参数字符串是空的话，就结束本函数，即没有传递来的参数
        if(queryString==null)
            return;

        //对传来的字符串参数进行解析
        queryString = URLUtil.decode(queryString);

        //得到&分割好的各个数据
        String[] parameterValues = queryString.split("&");

        //如果参数为空，则结束本函数，则没有传递来的参数
        if(parameterValues == null)
            return;

        for(String parameterValue : parameterValues){
            //得到=前后的两个字符串。
            // 因为之前用split进行了分割
            String[] nameValues = parameterValue.split("=");

            String name = nameValues[0];
            String value = nameValues[1];

            //看一看一是不是首次得到这个参数
            String values[] = parameterMap.get(name);
            if (values==null){
                //如果为空的话，就说明没有这个参数
                values = new String[]{value};

                parameterMap.put(name,values);
            }else {
                //用Util工具给values赋值，并在后面加上一个value值
                values = ArrayUtil.append(values,value);

                //将数据推入map映射表
                parameterMap.put(name,values);
            }

        }
    }


    private void parseHeaders(){
        StringReader stringReader = new StringReader(requestString);
        List<String> lines = new ArrayList<>();

        //从stringReader中，按行读取数据
        //将读取的结果放到lines中
        IoUtil.readLines(stringReader,lines);

        //应为头部除去一开始的HTTP请求
        //后面的数据都是键值对的形式
        //所以，绕开第一个数据，一一往后遍历
        //并一次推入headerMap中，以键值的方式
        for(int i=1;i<lines.size();i++){
            String line = lines.get(i);
            if(line.length()==0)
                break;

            String[] segs =line.split(":");
            String headerName = segs[0].toLowerCase();
            String headerValue = segs[1];

            this.hreaderMap.put(headerName,headerValue);
        }
    }

    //遍历请求的cookie头
    private void parseCookies(){
        List<Cookie> cookieList = new ArrayList<>();

        //从头部列中得到cookie 的值
        String cookies = this.hreaderMap.get("cookie");

        //如果存在cookie
        if(cookies!=null){
            //得到cookies存在的每一项
            String[] pairs = StrUtil.split(cookies,";");

            for(String pair:pairs){
                if(StrUtil.isBlank(pair))
                    continue;

                //再分离cookie的键值对，即=前后的数据
                String[] segs = StrUtil.split(pair,"=");

                //第一个数据是键，第二个数据是值
                String name =segs[0].trim();
                String value = segs[1].trim();

                //将处理好的数据推入Cookie，然后生成新的Cookie
                Cookie cookie = new Cookie(name,value);

                //将生成的cookie放到储存Cookie的列表中
                cookieList.add(cookie);
            }
        }

        //cookies的列表，其中是将集合框架转化成一般的数组
        this.cookies = ArrayUtil.toArray(cookieList,Cookie.class);
    }

    @Override
    public String getParameter(String name){
        String values[] = parameterMap.get(name);

        //如果有值，就返回值
        if(values!=null && values.length!=0)
            return values[0];

        //反之没有值的情况
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return this.parameterMap;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(this.parameterMap.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }


    @Override
    public String getHeader(String name) {
        //如果传入的参数有问题，返回的结果就是Null
        if(name==null)
            return null;

        //转换小写的形式
        name = name.toLowerCase();
        return this.hreaderMap.get(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set keys = this.hreaderMap.keySet();
        return Collections.enumeration(keys);
    }

    @Override
    public int getIntHeader(String name) {
        String value = this.hreaderMap.get(name);

        //将得到的头数据转换成整数，如果转化失败，就使用默认值0
        return Convert.toInt(value,0);
    }

    public String getUri(){
        return this.uri;
    }

    public String getRequestString(){
        return this.requestString;
    }

    private byte[] readBytes(InputStream is) throws IOException {
        int buffer_size = 1024;
        byte buffer[] = new byte[buffer_size];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while(true) {
            int length = is.read(buffer);
            if(-1==length)
                break;
            baos.write(buffer, 0, length);
            if(length!=buffer_size)
                break;
        }
        byte[] result =baos.toByteArray();
        return result;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    //得到Request的访问目录
    public Context getContext() {
        return context;
    }

    @Override
    public String getMethod() {
        return this.method;
    }

    @Override
    public ServletContext getServletContext() {
        return context.getServletContext();
    }

    @Override
    public String getRealPath(String path) {
        return context.getServletContext().getRealPath(path);
    }

    /*
    一些常用的方法
     */

    //得到服务器的地址
    public String getLocalAddr(){
        return socket.getLocalAddress().getHostAddress();
    }

    //得到服务器的名字
    public String getLocalName(){
        return socket.getLocalAddress().getHostName();
    }

    //得到服务器的端口
    public int getLocalPort(){
        return socket.getLocalPort();
    }

    @Override
    public String getProtocol() {
        return "HTTP:/1.1";
    }

    //得到访问浏览器的地址
    @Override
    public String getRemoteAddr() {
        InetSocketAddress inetSocketAddress = (InetSocketAddress)socket.getRemoteSocketAddress();

        String temp = inetSocketAddress.toString();

        return StrUtil.subAfter(temp,"/",false);
    }

    //得到访问浏览器的名字
    @Override
    public String getRemoteHost() {
        InetSocketAddress inetSocketAddress = (InetSocketAddress)socket.getRemoteSocketAddress();
        return inetSocketAddress.getHostName();
    }

    //得到访问浏览器的端口

    @Override
    public int getRemotePort() {
        return socket.getPort();
    }

    //得到协议的名称
    @Override
    public String getScheme() {
        return "http";
    }

    //得到服务器的名字
    @Override
    public String getServerName() {
        return getHeader("host").trim();
    }

    //得到服务器的端口
    @Override
    public int getServerPort() {
        return socket.getLocalPort();
    }

    //得到服务器的目录的相对位置
    @Override
    public String getContextPath() {
        String result = this.context.getPath();
        if("/".equals(result))
            return "";
        return result;
    }

    @Override
    public String getRequestURI() {
        return this.uri;
    }

    //得到URL的地址
    @Override
    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();

        if(port<0){
            port = 80;
        }
        url.append(scheme);
        url.append("://");
        url.append(getServerName());

        if((scheme.equals("http")&&(port!=80)||(scheme.equals("https")&&(port!=443)))){
            //80是http的默认端口，没有80的话，就要显示端口
            //443是https的默认端口，没有443的话，就要显示端口
            url.append(':');
            url.append(port);
        }

        //在最后面还要加上资源定位符
        url.append(getRequestURI());

        return url;
    }

    public String getJSessionIdFromCookie(){
        //如果cookies没有值，就说明
        if(cookies==null)
            return null;

        //如果cookie中，有含JESSIONID健的
        // 就返回其值作为Session的结果
        for(Cookie cookie:cookies){
            if("JSESSIONID".equals(cookie.getName())){
                return cookie.getValue();
            }
        }
        return null;
    }

    //得到servlet的路劲
    @Override
    public String getServletPath() {
        return uri;
    }

    //得到请求的cookies列表
    @Override
    public Cookie[] getCookies() {
        return cookies;
    }

    @Override
    public HttpSession getSession() {
        return session;
    }

    public void setSession(HttpSession session) {
        this.session = session;
    }

    public Connector getConnector() {
        return connector;
    }

    public boolean isForwared() {
        return forwared;
    }

    public void setForwared(boolean forwared) {
        this.forwared = forwared;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void removeAttribute(String name) {
        this.attributesMap.remove(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        this.attributesMap.put(name,value);
    }

    @Override
    public Object getAttribute(String name) {
        return attributesMap.get(name);
    }

    //得到所有的变量名
    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributesMap.keySet());
    }

    //返回控制跳转的类
    @Override
    public RequestDispatcher getRequestDispatcher(String uri) {
        return new ApplicationRequestDispatcher(uri);
    }
}
