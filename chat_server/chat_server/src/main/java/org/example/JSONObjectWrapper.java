package org.example;

import org.json.JSONObject;

import java.nio.channels.SocketChannel;

public class JSONObjectWrapper {
    private JSONObject jsonObject;

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public JSONObjectWrapper(JSONObject jsonObject, SocketChannel socketChannel) {
        this.jsonObject = jsonObject;
        this.socketChannel = socketChannel;
    }

    private SocketChannel socketChannel;
    public JSONObject getJsonObject() {
        return jsonObject;
    }

}
