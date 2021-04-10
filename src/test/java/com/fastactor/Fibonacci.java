package com.fastactor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;

public class Fibonacci extends Actor<Fibonacci.Command> {
    interface Command {}

    static class Compute implements Command {
        final int n;
        final BlockingQueue<Long> results;

        public Compute(int n, BlockingQueue<Long> results) {
            this.n = n;
            this.results = results;
        }
    }

    private static class InternalCompute implements Command {
        final int n;
        final ActorRef<Fibonacci.Command> sender;

        public InternalCompute(int n, ActorRef<Command> sender) {
            this.n = n;
            this.sender = sender;
        }
    }

    private static class InternalComputedResult implements Command {
        final long result;

        public InternalComputedResult(long result) {
            this.result = result;
        }
    }

    private static final Logger logger = LogManager.getLogger(Fibonacci.class);

    private Long firstResult;
    private ActorRef<Fibonacci.Command> sender;
    private BlockingQueue<Long> results;

    public Fibonacci(String id) {
        super(id);
        firstResult = null;
    }

    @Override
    protected void onMessage(Command message) {
        if (message instanceof Compute) {
            processMessage((Compute) message);
        } else if (message instanceof InternalCompute) {
            processMessage((InternalCompute) message);
        } else if (message instanceof InternalComputedResult) {
            processMessage((InternalComputedResult) message);
        }
    }

    protected void processMessage(Compute compute) {
        int n = compute.n;
        actorOf(new Fibonacci("Fibonacci-" + n + "-1")).tell(new InternalCompute(n - 1, getSelf()));
        actorOf(new Fibonacci("Fibonacci-" + n + "-2")).tell(new InternalCompute(n - 2, getSelf()));
        this.results = compute.results;
    }

    protected void processMessage(InternalCompute compute) {
        int n = compute.n;
        sender = compute.sender;
        if (n == 0 || n == 1) {
            compute.sender.tell(new InternalComputedResult(n));
        } else {
            actorOf(new Fibonacci("Fibonacci-" + n + "-1")).tell(new InternalCompute(n - 1, getSelf()));
            actorOf(new Fibonacci("Fibonacci-" + n + "-2")).tell(new InternalCompute(n - 2, getSelf()));
        }
    }

    protected void processMessage(InternalComputedResult computedResult) {
        if (firstResult == null) {
            firstResult = computedResult.result;
        } else if (results != null) {
            results.offer(firstResult + computedResult.result);
        } else {
            sender.tell(new InternalComputedResult(firstResult + computedResult.result));
        }
    }
}
