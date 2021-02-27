package com.fastactor;

import org.jctools.queues.MpscLinkedQueue;
import org.jctools.queues.MpscUnboundedArrayQueue;
import org.junit.jupiter.api.Test;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

class ActorSystemBasicTest {
    final Integer messageCount = 1_000_000;
    final int threadPoolSize = Runtime.getRuntime().availableProcessors();

    @Test
    void clq() throws InterruptedException {
        ActorSystem actorSystem = new ActorSystem(threadPoolSize);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        actorSystem.actorOf(new Ping(ConcurrentLinkedQueue::new, messageCount,
                countDownLatch,"Ping", "Pong"));
        countDownLatch.await();
    }

    @Test
    void mpscLQ() throws InterruptedException {
        ActorSystem actorSystem = new ActorSystem(threadPoolSize);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        actorSystem.actorOf(new Ping(MpscLinkedQueue::new, messageCount,
                countDownLatch,"Ping", "Pong"));
        countDownLatch.await();
    }

    @Test
    void mpscUAQ() throws InterruptedException {
        ActorSystem actorSystem = new ActorSystem(threadPoolSize);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        actorSystem.actorOf(new Ping(() -> new MpscUnboundedArrayQueue(1024), messageCount,
                countDownLatch,"Ping", "Pong"));
        countDownLatch.await();
    }
}