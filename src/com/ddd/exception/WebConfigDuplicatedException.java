package com.ddd.exception;

//用于Servlet发生重复配置的时候，抛出处理
public class WebConfigDuplicatedException extends Exception {
    public WebConfigDuplicatedException(String msg){
        super(msg);
    }
}
