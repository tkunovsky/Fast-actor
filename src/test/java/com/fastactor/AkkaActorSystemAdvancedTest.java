package com.fastactor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

import akka.actor.typed.ActorSystem;

public class AkkaActorSystemAdvancedTest {

    public Behavior<Void> create(int messageCount, int actorPingCount, int pongPerPing, CountDownLatch countDownLatch, Integer sleepTime) {
        return Behaviors.setup(
                context -> {
                    for (int a = 0; a < actorPingCount; a++) {
                        context.spawn(AkkaPing.create(countDownLatch, messageCount, pongPerPing, "Pong-" + a, sleepTime), "Ping-" + a);
                    }

                    return Behaviors.receive(Void.class).build();
                });
    }

    @Test
    void manyActorsFewMessages() throws InterruptedException {
        int messageCount = 10000;
        int actorPingCount = 1000000;
        CountDownLatch countDownLatch = new CountDownLatch(actorPingCount);
        ActorSystem.create(create(messageCount, actorPingCount, 1, countDownLatch, null), "manyActorsFewMessagesTest");
        countDownLatch.await();
    }

    @Test
    void fewActorsManyMessages() throws InterruptedException {
        int messageCount = 10000000;
        int actorPingCount = 1000;
        CountDownLatch countDownLatch = new CountDownLatch(actorPingCount);
        ActorSystem.create(create(messageCount, actorPingCount, 1, countDownLatch, null), "manyActorsFewMessagesTest");
        countDownLatch.await();
    }

    @Test
    void manyMessagesFromPong() throws InterruptedException {
        int messageCount = 1000;
        int actorPingCount = 1000;
        int pongPerPing = 1000;

        CountDownLatch countDownLatch = new CountDownLatch(actorPingCount * pongPerPing);
        ActorSystem.create(create(messageCount, actorPingCount, pongPerPing, countDownLatch, null), "manyActorsFewMessagesTest");
        countDownLatch.await();
    }

    @Test
    void sleepingPingActors() throws InterruptedException {
        int messageCount = 10;
        int actorPingCount = 1000;
        int sleepTime = 100;

        CountDownLatch countDownLatch = new CountDownLatch(actorPingCount);
        ActorSystem.create(create(messageCount, actorPingCount, 1, countDownLatch, sleepTime), "manyActorsFewMessagesTest");
        countDownLatch.await();
    }

    @Test
    void fibonacci() throws InterruptedException {
        int n = 30;
        long expected = 832040;

        BlockingQueue<Long> results = new LinkedBlockingDeque<>();
        ActorSystem.create(AkkaFibonacci.create(), "Fibonacci-" + n).tell(new AkkaFibonacci.Compute(n, results));
        Long result = results.take();
        Assertions.assertEquals(expected, result);
    }

}
