package com.boot.cleanhub.common.hazelcast;

import java.io.Serializable;

/**
 * <pre>
 *   SessionNode 
 *   Hazelcast에서 사용하기 위한 객체
 * </pre>
 * @author In-seong Hwang
 * @since 2024.11.28
 * @version 1.0
 */
public class SessionNode implements Serializable{
    private String sessionId;
    private String msg;
    private String code;


    public String getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }
    
}
