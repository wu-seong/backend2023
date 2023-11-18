package org.example;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.example.Server.mode;

public class TaskQueue {
    final private Lock lock = new ReentrantLock();
    final private Condition notFull = lock.newCondition();
    final private Condition notEmpty = lock.newCondition();
    final private Condition notContain = lock.newCondition();
    private Queue<Object> messageQueue = new LinkedList<>();

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public static TaskQueue makeQueue(int maxCount) { // 인스턴스 반환
        TaskQueue taskQueue = new TaskQueue();
        taskQueue.setMaxCount(maxCount);
        return taskQueue;
    }

    public
    int count;
    int maxCount;

    public void put(Object task, SocketChannel socketChannel) throws InterruptedException {
        System.out.println("put");
        lock.lock();
        try {
            while (count == maxCount) {
                notFull.await();
            }
            while( isContain(socketChannel)){ //contain하면 take하여 signal 받아서 not contain할 때 까지 대기
                notContain.await();
            }
            messageQueue.add(task);
            ++count;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public Object take() throws InterruptedException {
        lock.lock();
        System.out.println("take");
        try {
            while (count == 0) {
                notEmpty.await();
            }
            Object task = messageQueue.poll();
            --count;
            notContain.signal();    //take 한 뒤에 Contain한지 확인 하도록 signal보냄
            notFull.signal();
            return task;
        } finally {
            lock.unlock();
        }
    }

    public Object peek(){
        lock.lock();
        System.out.println("peek");
        try {
            Object task = messageQueue.peek();
            return task;
        } finally {
          lock.unlock();
        }
    }

    /*
    같은 유저가 보낸 task가 있는지 확인하는 메서드
     */
    public boolean isContain(SocketChannel socketChannel){
        lock.lock();
        try{
            for (Object task:messageQueue) {
                switch (mode){
                    case JSON:
                        JSONObjectWrapper jsonObjectWrapper = ((ThreadPoolRunnable) task).getJsonObjectWrapper();
                        SocketChannel insideSocketChannel1 = jsonObjectWrapper.getSocketChannel();
                        if( socketChannel.equals(insideSocketChannel1)){
                            return true;
                        }
                        break;
                    case PROTOBUF:
                        BinaryMessage binaryMessage = ((ThreadPoolRunnable) task).getBinaryMessage();
                        SocketChannel insideSocketChannel2 = binaryMessage.getSocketChannel();
                        if( socketChannel.equals(insideSocketChannel2)){
                            return true;
                        }
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
}
