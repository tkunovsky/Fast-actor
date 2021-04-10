package com.fastactor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

public class ActorSystemAdvancedTest {
    final int threadPoolSize = Runtime.getRuntime().availableProcessors();

    @Test
    void manyActorsFewMessages() throws InterruptedException {
        int messageCount = 1000;
        int actorPingCount = 100000;

        ActorSystem actorSystem = new ActorSystem(threadPoolSize);

        CountDownLatch countDownLatch = new CountDownLatch(actorPingCount);
        for (int a = 0; a < actorPingCount; a++) {
            actorSystem.actorOf(new Ping(messageCount, countDownLatch, "Ping-" + a, "Pong-" + a));
        }

        countDownLatch.await();
    }

    @Test
    void fewActorsManyMessages() throws InterruptedException {
        int messageCount = 1000000;
        int actorPingCount = 100;

        ActorSystem actorSystem = new ActorSystem(threadPoolSize);

        CountDownLatch countDownLatch = new CountDownLatch(actorPingCount);
        for (int a = 0; a < actorPingCount; a++) {
            actorSystem.actorOf(new Ping(messageCount, countDownLatch, "Ping-" + a, "Pong-" + a));
        }

        countDownLatch.await();
    }

    @Test
    void manyMessagesFromPong() throws InterruptedException {
        int messageCount = 100;
        int actorPingCount = 100;
        int pongPerPing = 100;

        ActorSystem actorSystem = new ActorSystem(threadPoolSize);

        CountDownLatch countDownLatch = new CountDownLatch(actorPingCount * pongPerPing);
        for (int a = 0; a < actorPingCount; a++) {
            actorSystem.actorOf(new Ping(messageCount, countDownLatch, "Ping-" + a, "Pong-" + a, pongPerPing));
        }

        countDownLatch.await();
    }

    @Test
    void sleepingPingActors() throws InterruptedException {
        int messageCount = 10;
        int actorPingCount = 100;
        int sleepTime = 10;

        ActorSystem actorSystem = new ActorSystem(threadPoolSize);

        CountDownLatch countDownLatch = new CountDownLatch(actorPingCount);
        for (int a = 0; a < actorPingCount; a++) {
            actorSystem.actorOf(new Ping(messageCount, countDownLatch, sleepTime, "Ping-" + a, "Pong-" + a));
        }

        countDownLatch.await();
    }

    @Test
    void fibonacci() throws InterruptedException {
        int n = 30;
        long expected = 832040;

        BlockingQueue<Long> results = new LinkedBlockingDeque<>();
        ActorSystem actorSystem = new ActorSystem();
        actorSystem.actorOf(new Fibonacci("Fibonacci-" + n)).tell(new Fibonacci.Compute(n, results));
        Long result = results.take();
        Assertions.assertEquals(expected, result);
    }
}
