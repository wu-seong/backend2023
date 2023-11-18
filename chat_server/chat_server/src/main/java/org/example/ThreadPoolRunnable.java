package org.example;

import com.google.protobuf.InvalidProtocolBufferException;
import mju.MessagePb;
import org.json.JSONObject;

import static org.example.Server.*;

public class ThreadPoolRunnable implements Runnable {
    private TaskQueue taskQueue;

    private BinaryMessage binaryMessage;

    private JSONObjectWrapper jsonObjectWrapper;

    public ThreadPoolRunnable(TaskQueue taskQueue) {
        this.taskQueue = taskQueue;
    }

    public ThreadPoolRunnable(BinaryMessage binaryMessage, TaskQueue taskQueue) {
        this.binaryMessage = binaryMessage;
        this.taskQueue = taskQueue;
    }

    public BinaryMessage getBinaryMessage() {
        return binaryMessage;
    }

    public JSONObjectWrapper getJsonObjectWrapper() {
        return jsonObjectWrapper;
    }

    public ThreadPoolRunnable(JSONObjectWrapper jsonObjectWrapper, TaskQueue taskQueue) {
        this.jsonObjectWrapper = jsonObjectWrapper;
        this.taskQueue = taskQueue;
    }
    private volatile boolean running = true;

    @Override
    public void run() {
        switch (mode){
            case PROTOBUF:
                while (running) {
                    Thread thread = Thread.currentThread();
                    System.out.println("thread.getId() = " + thread.getId());
                    try {
                        if (binaryMessage != null) {
                            System.out.println(binaryMessage);
                            MessagePb.Type messageType = MessagePb.Type.parseFrom(binaryMessage.getTypeMessage());
                            ProtoMessageWrapper protoMessageWrapper = new ProtoMessageWrapper(clients.get(binaryMessage.getSocketChannel()), binaryMessage.getRequestMessage());
                            messageHandler.protobufHandleMessage(messageType.getType(), protoMessageWrapper);
                        }
                        Runnable r = (Runnable) taskQueue.take();
                        r.run();
                    } catch (InterruptedException e) {
                        stop();
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException(e);
                    }
                }
                break;
            case JSON:
                while (running) {
                    Thread thread = Thread.currentThread();
                    System.out.println("thread.getId() = " + thread.getId());
                    try {
                        if(jsonObjectWrapper != null) {
                            String type = jsonObjectWrapper.getJsonObject().getString("type");
                            JSONMessageWrapper jsonMessageWrapper = new JSONMessageWrapper(clients.get(jsonObjectWrapper.getSocketChannel()), jsonObjectWrapper.getJsonObject());
                            messageHandler.JSONHandleMessage(type, jsonMessageWrapper);
                        }
                        Runnable r = (Runnable) taskQueue.take();
                        r.run();
                    } catch (InterruptedException e) {
                        stop();
                    }
                }
                break;
        }
    }

    public void stop() {
        running = false;
    }

    @Override
    public String toString() {
        return "ThreadPoolRunnable{" +
                "taskQueue=" + taskQueue +
                ", binaryMessage=" + binaryMessage +
                ", running=" + running +
                '}';
    }
}
