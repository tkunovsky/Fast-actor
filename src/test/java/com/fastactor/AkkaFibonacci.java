package com.fastactor;

import akka.actor.typed.javadsl.AbstractBehavior;

import java.util.concurrent.BlockingQueue;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.ActorRef;

public class AkkaFibonacci extends AbstractBehavior<AkkaFibonacci.Command> {
    public static Behavior<AkkaFibonacci.Command> create() {
        return Behaviors.setup(context -> new AkkaFibonacci(context));
    }

    interface Command {}

    static class Compute implements AkkaFibonacci.Command {
        final int n;
        final BlockingQueue<Long> results;

        public Compute(int n, BlockingQueue<Long> results) {
            this.n = n;
            this.results = results;
        }
    }

    private static class InternalCompute implements AkkaFibonacci.Command {
        final int n;
        final ActorRef<AkkaFibonacci.Command> sender;

        public InternalCompute(int n, ActorRef<AkkaFibonacci.Command> sender) {
            this.n = n;
            this.sender = sender;
        }
    }

    private static class InternalComputedResult implements AkkaFibonacci.Command {
        final long result;

        public InternalComputedResult(long result) {
            this.result = result;
        }
    }

    private Long firstResult;
    private ActorRef<AkkaFibonacci.Command> sender;
    private BlockingQueue<Long> results;

    private AkkaFibonacci(ActorContext<AkkaFibonacci.Command> context) {
        super(context);
    }

    @Override
    public Receive<AkkaFibonacci.Command> createReceive() {
        return newReceiveBuilder().onMessage(AkkaFibonacci.Compute.class, this::onMessageCompute)
                .onMessage(AkkaFibonacci.InternalCompute.class, this::onMessageInternalCompute)
                .onMessage(AkkaFibonacci.InternalComputedResult.class, this::onMessageInternalComputedResult)
                .build();
    }

    private Behavior<AkkaFibonacci.Command> onMessageCompute(AkkaFibonacci.Compute message) {
        int n = message.n;
        getContext().spawn(AkkaFibonacci.create(), "Fibonacci-" + n + "-1").tell(new AkkaFibonacci.InternalCompute(n - 1, getContext().getSelf()));
        getContext().spawn(AkkaFibonacci.create(), "Fibonacci-" + n + "-2").tell(new AkkaFibonacci.InternalCompute(n - 2, getContext().getSelf()));
        this.results = message.results;

        return this;
    }

    private Behavior<AkkaFibonacci.Command> onMessageInternalCompute(AkkaFibonacci.InternalCompute message) {
        int n = message.n;
        sender = message.sender;
        if (n == 0 || n == 1) {
            message.sender.tell(new AkkaFibonacci.InternalComputedResult(n));
        } else {
            getContext().spawn(AkkaFibonacci.create(), "Fibonacci-" + n + "-1").tell(new AkkaFibonacci.InternalCompute(n - 1, getContext().getSelf()));
            getContext().spawn(AkkaFibonacci.create(), "Fibonacci-" + n + "-2").tell(new AkkaFibonacci.InternalCompute(n - 2, getContext().getSelf()));
        }

        return this;
    }

    private Behavior<AkkaFibonacci.Command> onMessageInternalComputedResult(AkkaFibonacci.InternalComputedResult message) {
        if (firstResult == null) {
            firstResult = message.result;
        } else if (results != null) {
            results.offer(firstResult + message.result);
        } else {
            sender.tell(new AkkaFibonacci.InternalComputedResult(firstResult + message.result));
        }

        return this;
    }
}
