package com.ddd.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import com.ddd.http.Request;
import com.ddd.http.Response;
import com.ddd.http.StandardSession;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//一个管理Session的类
public class SessionManager {
    //保管所有的Session
    private static Map<String ,StandardSession> sessionMap = new ConcurrentHashMap<>();

    //设置默认的失效时间
    private static int defaultTimeout = getTimeout();

    static {
        startSessionOutdateCheckThread();
    }

    //每30s一次，检查线程是否失效
    private static void startSessionOutdateCheckThread(){
        new Thread(){
            @Override
            public void run() {
                while(true){
                    checkOutDateSession();
                    ThreadUtil.sleep(1000*30);
                }
            }
        }.start();
    }

    //检查Session列表里所有的session，是否超过了存活时间
    private static void checkOutDateSession(){
        Set<String> jsessionids = sessionMap.keySet();

        //储存没有通过检查的session
        List<String> outdateJessionIds = new ArrayList<>();

        //遍历sessionid
        //然后根据SessionID在SessionMap里取值
        //接着再逐一比较当前时间和最后访问时间的差值
        //如果结果比最长的生存时间要长，就说明超时了
        //反之通过检查
        for(String jsessionid : jsessionids){
            StandardSession standardSession = sessionMap.get(jsessionid);
            long interval = System.currentTimeMillis() - standardSession.getLastAccessedTime();
            if(interval > standardSession.getMaxInactiveInterval()*1000)
                outdateJessionIds.add(jsessionid);
        }

        //对于没有通过检查的session，进行remove操作
        for (String jsessionid : outdateJessionIds){
            sessionMap.remove(jsessionid);
        }
    }

    //从conf目录下的web.xml里读取数据
    //如果读取到数据，就说明
    private static int getTimeout(){
        int defaultResult = 30;
        try {
            Document document = Jsoup.parse(Constant.webXmlFile,"utf-8");
            Elements elements = document.select("session-config session-timeout");

            //如果在文件里没有找到相关数据的话，就返回默认的值
            if(elements.isEmpty())
                return defaultResult;

            //如果在文件里找到相关的数据的话，就取第一个session-timeout标签内的值进行返回
            return Convert.toInt(elements.get(0).text());
        }catch (IOException e){
            return defaultResult;
        }
    }

    //生成SessionID值的一个过程
    public static synchronized String generateSessionId(){
        String result = null;

        //先随机生成一个数列
        byte[] bytes = RandomUtil.randomBytes(16);

        //将生成的16字节的随机数列进行md5的计算
        //之后再将数列生成的字符串全部转化成小写的字母
        result = new String(bytes);
        result = SecureUtil.md5(result);
        result = result.toUpperCase();

        //返回最后的结果
        return result;
    }

    //得到session的值
    //如果传入的sessionID没有值，那么，就返回一个新Session
    //如果没有再map根据sessionID找到session,，也返回一个新session
    //最后，如果找到了，那么，就重新设置最后的访问时间，然后生成一个cookie
    //返回得到的Session值
    public static HttpSession getSession(String jsessionid, Request request, Response response){
        if(jsessionid==null){
            return newSession(request,response);
        }else {
            StandardSession currentSession = sessionMap.get(jsessionid);

            if(currentSession==null){
                return newSession(request,response);
            }else {
                currentSession.setLastAccessedTime(System.currentTimeMillis());

                //设置一个cookie，以供方会给浏览器存储
                createCookieBySession(currentSession,request,response);
                return currentSession;
            }

        }

    }

    //创建session
    private static HttpSession newSession(Request request,Response response){
        //得到访问的context，即记录本身
        ServletContext servletContext = request.getServletContext();

        //得到sessionID
        String sid = generateSessionId();

        //生成一个标准的session，利用sessionID，还有context
        StandardSession session = new StandardSession(sid,servletContext);

        //将生成的session放入sessionMap中
        sessionMap.put(sid,session);

        //通过session来生成cookie，以便返回给浏览器
        createCookieBySession(session,request,response);

        return session;
    }

    private static void createCookieBySession(HttpSession session,Request request,Response response){
        //利用JESSIONID作为cookie的名字
        //再将SessionId的值作为改key的value
        Cookie cookie = new Cookie("JSESSIONID",session.getId());

        //设置cookie的最长生存时间
        cookie.setMaxAge(session.getMaxInactiveInterval());

        //设置cookie的path值
        cookie.setPath(request.getContext().getPath());

        //将cookie 放到response中
        response.addCookie(cookie);
    }

}
