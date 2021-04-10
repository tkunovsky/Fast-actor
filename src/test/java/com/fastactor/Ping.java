package com.fastactor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jctools.queues.MpscLinkedQueue;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Ping extends Actor<Ping.Message> {
    private static final Logger logger = LogManager.getLogger(Ping.class);

    private final Integer initData;
    private ActorRef<Ping.Message>[] pongs;
    private final CountDownLatch countDownLatch;
    private String pongId;
    private final Supplier<Queue<Ping.Message>> queueFactory;
    private final int pongCount;
    private final Map<ActorRef<Ping.Message>, Integer> lastValues;
    private final Integer sleepTime;

    static public class Message {
        final int data;
        final ActorRef<Ping.Message> sender;

        public Message(int data, ActorRef<Ping.Message> sender) {
            this.data = data;
            this.sender = sender;
        }
    }

    public Ping(Supplier<Queue<Ping.Message>> queueFactory, int initValue, CountDownLatch countDownLatch,
                String id, String pongId, int pongCount, Integer sleepTime) {
        super(id, queueFactory);
        initData = initValue;
        this.countDownLatch = countDownLatch;
        this.pongId = pongId;
        this.queueFactory = queueFactory;
        this.pongCount = pongCount;
        this.lastValues = new HashMap<>(pongCount);
        this.sleepTime = sleepTime;
    }

    public Ping(Supplier<Queue<Ping.Message>> queueFactory, int initValue, CountDownLatch countDownLatch,
                String id, String pongId) {
        this(queueFactory, initValue, countDownLatch, id, pongId, 1, null);
    }

    public Ping(int initValue, CountDownLatch countDownLatch, String id, String pongId) {
        this(MpscLinkedQueue::new, initValue, countDownLatch, id, pongId, 1, null);
    }

    public Ping(int initValue, CountDownLatch countDownLatch, String id, String pongId, int pongCount) {
        this(MpscLinkedQueue::new, initValue, countDownLatch, id, pongId, pongCount, null);
    }

    public Ping(int initValue, CountDownLatch countDownLatch, Integer sleepTime, String id, String pongId) {
        this(MpscLinkedQueue::new, initValue, countDownLatch, id, pongId, 1, sleepTime);
    }

    @Override
    protected void preStart() {
        pongs = new ActorRef[pongCount];
        for (int m = 0; m < pongCount; m++) {
            pongs[m] = actorOf(new Pong(queueFactory, pongId + "." + m));
            pongs[m].tell(new Message(initData, getSelf()));
            lastValues.put(pongs[m], initData);
        }
    }

    @Override
    protected void onMessage(Ping.Message message) {
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
            message.sender.tell(new Message(newData, getSelf()));
        } else {
            countDownLatch.countDown();
        }
    }
}
