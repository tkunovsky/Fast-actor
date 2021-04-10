package com.fastactor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.ActorRef;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AkkaPong extends AbstractBehavior<AkkaPing.Message> {
    public static Behavior<AkkaPing.Message> create(ActorRef<AkkaPing.Message> ping) {
        return Behaviors.setup(context -> new AkkaPong(context, ping));
    }

    private static final Logger logger = LogManager.getLogger(AkkaPong.class);
    private final ActorRef<AkkaPing.Message> ping;
    private Integer lastValue;

    private AkkaPong(ActorContext<AkkaPing.Message> context, ActorRef<AkkaPing.Message> ping) {
        super(context);
        this.ping = ping;
    }

    @Override
    public Receive<AkkaPing.Message> createReceive() {
        return newReceiveBuilder().onMessage(AkkaPing.Message.class, this::onMessage).build();
    }

    private Behavior<AkkaPing.Message> onMessage(AkkaPing.Message message) {
        if (lastValue != null) {
            assertEquals(lastValue - 1, message.data);
        }

        lastValue = message.data - 1;
        ping.tell(new AkkaPing.Message(lastValue, getContext().getSelf()));

        return this;
    }
}
