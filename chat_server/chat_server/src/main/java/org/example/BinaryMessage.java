package org.example;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.example.MessageUtility.getMessage;

public class BinaryMessage {
    private SocketChannel socketChannel;
    private byte[] typeMessage;
    private byte[] requestMessage;
    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public byte[] getTypeMessage() {
        return typeMessage;
    }

    public byte[] getRequestMessage() {
        return requestMessage;
    }

    @Override
    public String toString() {
        return "BinaryMessage{" +
                "socketChannel=" + socketChannel +
                ", typeMessage=" + Arrays.toString(typeMessage) +
                ", requestMessage=" + Arrays.toString(requestMessage) +
                '}';
    }

    public void setBinary(byte[] typeMessage, byte[] requestMessage){
        this.typeMessage = typeMessage;
        this.requestMessage = requestMessage;
    }

    public boolean isSet() {
        if(this.typeMessage != null && this.requestMessage != null &&
                (this.typeMessage.length == 2 && this.requestMessage.length > 0)){
            return true;
        }
        return false;
    }
}
