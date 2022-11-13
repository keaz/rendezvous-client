package com.kzone.message;

import lombok.extern.log4j.Log4j2;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Log4j2
public class MessageHolder {

    public static final Queue<Object> MESSAGES = new ConcurrentLinkedQueue<>();
    private final Lock lock = new ReentrantLock();
    private final Condition isEmpty = lock.newCondition();
    private final Condition hasElement = lock.newCondition();

    public void putMessage(Object clientEvent) {
        try {
            lock.lock();
            MESSAGES.add(clientEvent);
            isEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public Object readMessage() {
        try {
            lock.lock();
            while (MESSAGES.isEmpty()) {
                isEmpty.await();
            }
        } catch (InterruptedException e) {
            log.error("Thread interrupted ", e);
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }

        return MESSAGES.poll();
    }

}
