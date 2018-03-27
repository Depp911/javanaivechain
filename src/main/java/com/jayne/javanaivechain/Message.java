package com.jayne.javanaivechain;

import java.io.Serializable;

/**
 * 节点之间通信-消息
 *
 * Created by jayne on 2018/3/27.
 */
public class Message implements Serializable {
    private int    type;
    //type为RESPONSE_BLOCKCHAIN，date必须为List<Block>
    private String data;

    public Message() {
    }

    public Message(int type) {
        this.type = type;
    }

    public Message(int type, String data) {
        this.type = type;
        this.data = data;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
