package com.fastactor;

import java.util.Queue;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Pong extends Actor<Ping.Message> {

    private Integer lastValue;

    public Pong(Supplier<Queue<Ping.Message>> queueFactory, String id) {
        super(id, queueFactory);
    }

    @Override
    protected void onMessage(Ping.Message message) {
        if (lastValue != null) {
            assertEquals(lastValue - 1, message.data);
        }

        lastValue = message.data - 1;
        message.sender.tell(new Ping.Message(lastValue, getSelf()));
    }
}
