package org.example;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
/*
방 ID를 thread safe하게 생성해주는 클래스
 */
public class IdGenerator {
    private AtomicInteger currentId = new AtomicInteger(0);
    private ConcurrentLinkedQueue<Integer> unusedIds = new ConcurrentLinkedQueue<>();

    public Integer getNewId() {
        Integer id = unusedIds.poll();
        if (id == null) {
            id = currentId.incrementAndGet();
        }
        return id;
    }

    public void releaseId(Integer id) {unusedIds.add(id);
    }
}