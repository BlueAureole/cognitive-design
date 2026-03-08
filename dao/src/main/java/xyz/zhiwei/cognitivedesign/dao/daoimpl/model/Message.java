
package xyz.zhiwei.cognitivedesign.dao.daoimpl.model;

/**
 * @param <T>
 * @author Peace
 */
public class Message<T> {
    private int code = 200;//返回标识码
    private String msg = "";//错误信息
    private T result;//返回结果值

    public Message() {
    }
    
    public Message(T result) {
        this.result = result;
    }

    public Message(int code, String msg, T result) {
        this.msg = msg;
        this.code = code;
        this.result = result;
    }
    
    
    public static <T> Message<T> success(T result) {
    	return new Message<>(200,"成功",result);
    }
    public static <T> Message<T> success(String msg,T result) {
    	return new Message<>(200,msg,result);
    }
    
    
    
    
    public static <T> Message<T> failure(T result) {
    	return new Message<>(401,"失败",result);
    }
    public static <T> Message<T> failure(int code,String msg) {
    	return new Message<>(code,msg,null);
    }
    public static <T> Message<T> failure(String msg,T result) {
    	return new Message<>(401,msg,result);
    }
    public static <T> Message<T> failure(int code,String msg,T result) {
    	return new Message<>(code,msg,result);
    }
    
    
    
    
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }



}

