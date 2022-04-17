package com.kzone.p2p.message;

import com.kzone.p2p.event.Notification;
import lombok.extern.log4j.Log4j2;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Log4j2
public class MessageHolder {

    public static final Queue<Notification> MESSAGES = new ConcurrentLinkedQueue<>();
    private final Lock lock = new ReentrantLock();
    private final Condition isEmpty = lock.newCondition();
    private final Condition hasElement = lock.newCondition();

    public void putMessage(Notification notification) {
        try{
            lock.lock();
            MESSAGES.add(notification);
            isEmpty.signalAll();
        }finally {
            lock.unlock();
        }
    }

    public Notification readMessage() {
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
