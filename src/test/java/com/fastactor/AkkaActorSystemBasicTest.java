package com.fastactor;

import org.junit.jupiter.api.Test;
import akka.actor.typed.ActorSystem;

import java.util.concurrent.CountDownLatch;

public class AkkaActorSystemBasicTest {
    final Integer messageCount = 1_000_000;

    @Test
    void pingPong() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final ActorSystem<AkkaPing.Message> system = ActorSystem.create(AkkaPing.create(countDownLatch,
                messageCount, 1, "pong", null), "ping");
        countDownLatch.await();
    }
}
