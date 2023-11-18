package org.example;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class ThreadPool {
    private TaskQueue taskQueue;
    private List<ThreadPoolRunnable> runnableList = new ArrayList<>();

    private volatile boolean running = true;

    public ThreadPool(int maxThread, int maxCount) {
        taskQueue = TaskQueue.makeQueue(maxCount);
        for (int i = 0; i < maxThread; i++) {    //
            runnableList.add(new ThreadPoolRunnable(taskQueue));
        }
        for (ThreadPoolRunnable task: runnableList) {
            new Thread(task).start();
        }
    }
    public synchronized void execute(BinaryMessage binaryMessage) throws Exception{
        if( !running ){
            throw new RuntimeException("Thread Pool is running");
        }
        ThreadPoolRunnable runnable = new ThreadPoolRunnable(binaryMessage, taskQueue);
        SocketChannel socketChannel = binaryMessage.getSocketChannel();
        taskQueue.put(runnable, socketChannel);
    }
    public synchronized void execute(JSONObjectWrapper jsonObjectWrapper) throws Exception{
        if( !running ){
            throw new RuntimeException("Thread Pool is running");
        }
        ThreadPoolRunnable runnable = new ThreadPoolRunnable(jsonObjectWrapper, taskQueue);
        SocketChannel socketChannel = jsonObjectWrapper.getSocketChannel();
        taskQueue.put(runnable, socketChannel);
    }

    public synchronized void stop() throws InterruptedException{
        running = false;
        for( ThreadPoolRunnable r : runnableList){
            r.stop();
        }
    }
}
