package com.fastactor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.ActorRef;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AkkaPing extends AbstractBehavior<AkkaPing.Message> {
    public static Behavior<AkkaPing.Message> create(CountDownLatch countDownLatch, int initValue, int pongPerPing, String pongId, Integer sleepTime) {
        return Behaviors.setup(context -> new AkkaPing(context, countDownLatch, initValue, pongId, pongPerPing, sleepTime));
    }

    private static final Logger logger = LogManager.getLogger(AkkaPing.class);
    private final Map<ActorRef<AkkaPing.Message>, Integer> lastValues;
    private ActorRef<AkkaPing.Message>[] pongs;
    private final CountDownLatch countDownLatch;
    private final Integer sleepTime;

    static public class Message {
        final int data;
        final ActorRef<AkkaPing.Message> sender;

        public Message(int data, ActorRef<Message> sender) {
            this.data = data;
            this.sender = sender;
        }
    }


    private AkkaPing(ActorContext<AkkaPing.Message> context, CountDownLatch countDownLatch, int initValue,
                     String pongId, int pongCount, Integer sleepTime) {
        super(context);
        this.lastValues = new HashMap<>(pongCount);
        this.countDownLatch = countDownLatch;
        this.sleepTime = sleepTime;
        pongs = new ActorRef[pongCount];
        for (int m = 0; m < pongCount; m++) {
            pongs[m] = context.spawn(AkkaPong.create(context.getSelf()), pongId  + "." + m);
            pongs[m].tell(new Message(initValue, getContext().getSelf()));
            lastValues.put(pongs[m], initValue);
        }
    }

    @Override
    public Receive<AkkaPing.Message> createReceive() {

        return newReceiveBuilder().onMessage(AkkaPing.Message.class, this::onMessage).build();
    }

    private Behavior<AkkaPing.Message> onMessage(AkkaPing.Message message) {
        Integer previousValue = lastValues.get(message.sender);
        assertEquals(previousValue - 1, message.data);
        int newData = message.data - 1;
        if (sleepTime != null) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        lastValues.put(message.sender, newData);
        if (newData > 0) {
            message.sender.tell(new Message(newData, getContext().getSelf()));
        } else {
            countDownLatch.countDown();
        }

        return this;
    }
}
