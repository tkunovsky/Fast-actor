/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fastactor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Queue;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.jctools.queues.MpscLinkedQueue;

/**
 * Actor base class that should be extended to create an Actor with the semantics of the 'Actor Model':
 * <a href="http://en.wikipedia.org/wiki/Actor_model">http://en.wikipedia.org/wiki/Actor_model</a>
 * <p>
 * An actor has a well-defined life-cycle.
 * - ''RUNNING'' (created and started actor) - can receive messages
 * - ''NON-RUNNING'' (before it's assigned to an actor system) - can't do anything
 * <p>
 * The Actor's own {@link ActorRef} is available as {@link #getSelf()}.
 * The only abstract method is {@link #onMessage(MessageType message)} which shall implement message processing.
 *
 * @param <MessageType> base type of actor messages
 */
public abstract class Actor<MessageType> {
    private final OnMessageRun onMessageRun;
    private final ActorRef<MessageType> actorRef;
    private static final Logger logger = LogManager.getLogger(Actor.class);
    private ActorSystem actorSystem;
    private String name;
    private Queue<MessageType> mailbox;
    private final AtomicBoolean scheduled;

    /**
     * To be implemented by concrete Actor, this defines the behavior of the
     * actor.
     */
    abstract protected void onMessage(MessageType message);

    /**
     * Init a new Actor with the specific name and default mailbox queue {@link org.jctools.queues.MpscLinkedQueue}
     *
     * @param name name of the new actor
     */
    public Actor(String name) {
        this(name, MpscLinkedQueue::new);
    }

    /**
     * Init a new Actor with the specific name and mailbox queue
     *
     * @param name         name of the new actor
     * @param queueFactory factory for mailbox creation
     */
    public Actor(String name, Supplier<Queue<MessageType>> queueFactory) {
        mailbox = queueFactory.get();
        onMessageRun = new OnMessageRun();
        actorRef = new ActorRefImpl();
        if (name == null) {
            this.name = getDefaultUniqueActorName();
        } else {
            this.name = name;
        }
        this.scheduled = new AtomicBoolean(false);
    }

    /**
     * Init a new Actor with the generated name and default mailbox queue {@link org.jctools.queues.MpscLinkedQueue}
     */
    public Actor() {
        this(null);
    }

    /**
     * Each actor has a name which is set by constructor or generated.
     *
     * @return name of actor
     */
    public String getName() {
        return name;
    }

    /**
     * User overridable callback.
     * <p>
     * Is called when an Actor is started.
     * Actor are automatically started asynchronously when created.
     * Empty default implementation.
     */
    protected void preStart() {
    }

    /**
     * The ActorRef representing this actor
     * <p>
     * This method is thread-safe and can be called from other threads than the ordinary
     * actor message processing thread.
     */
    protected final ActorRef<MessageType> getSelf() {
        return getActorRef();
    }

    /**
     * Assigns actor to the same actor system.
     * <p>
     * This method is thread-safe and can be called from other threads than the ordinary
     * actor message processing thread,.
     *
     * @param actor actor which is assigned
     * @param <T>   base type of actor messages which is assigned
     * @return {@link ActorRef} of assigned actor
     */
    protected final <T> ActorRef<T> actorOf(Actor<T> actor) {
        try {
            return this.actorSystem.actorOf(actor);
        } catch (NullPointerException e) {
            throw new IllegalStateException("Method actorOf(Actor) cannot be called until" +
                    " this actor is assigned to an Actor System. It can be called for example in preStart() method", e);
        }
    }

    static String generateUniqueString(Object input) {
        return Integer.toHexString(System.identityHashCode(input));
    }

    private String getDefaultUniqueActorName() {
        return this.getClass().getSimpleName() + "<" + generateUniqueString(this) + ">";
    }

    void setActorSystem(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    ActorRef<MessageType> getActorRef() {
        return actorRef;
    }

    Routee<MessageType> getRoutee() {
        return new RouteeImpl();
    }

    Runnable getPreStartRunnable() {
        return new PreStartRun();
    }

    void markAsScheduled() {
        scheduled.set(true);
    }

    private class ActorRefImpl implements ActorRef<MessageType> {

        @Override
        public void tell(MessageType message) {
            mailbox.offer(message);
            scheduleIfNeeded();
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }
    }

    private class RouteeImpl implements Routee<MessageType> {

        @Override
        public int getMailboxSize() {
            return mailbox.size();
        }

        @Override
        public ActorRef<MessageType> getActorRef() {
            return Actor.this.getActorRef();
        }
    }

    private void scheduleIfNeeded() {
        if (!scheduled.get() && !mailbox.isEmpty() && !scheduled.getAndSet(true)) {
            if (ForkJoinTask.inForkJoinPool() && (ForkJoinTask.getPool() == actorSystem.getUserThreadPool())) {
                ForkJoinTask.adapt(onMessageRun).fork();
            } else {
                actorSystem.getUserThreadPool().execute(onMessageRun);
            }
        }
    }

    private class OnMessageRun implements Runnable {
        @Override
        public void run() {
            try {
                for (MessageType message = mailbox.poll(); message != null; message = mailbox.poll()) {
                    onMessage(message);
                }
            } catch (Throwable e) {
                logger.error("Unexpected exception from Actor '{}': ", getName(), e);
            } finally {
                scheduled.set(false);
                scheduleIfNeeded();
            }
        }
    }

    private class PreStartRun implements Runnable {
        @Override
        public void run() {
            try {
                preStart();
            } catch (Throwable e) {
                logger.error("Unexpected exception from Actor '{}': ", getName(), e);
            } finally {
                scheduled.set(false);
                scheduleIfNeeded();
            }
        }
    }
}
