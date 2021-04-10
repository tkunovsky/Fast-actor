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

import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.*;

/**
 * An actor system is a flat group of actors which share thread pool. It is also the entry point for creating actors.
 * <p>
 * There are several possibilities for creating actors it specific system:
 * <p>
 * {@code
 * system.actorOf(actor)
 * actor.actorOf(actor)
 * }
 */
public final class ActorSystem {
    private static final Logger logger = LogManager.getLogger(ActorSystem.class);
    private final ForkJoinPool userThreadPool;

    /**
     * Creates a new ActorSystem with thread pool with the specific number of threads.
     *
     * @param maximumThreadsNumber number of threads which are shared by assigned actors
     */
    public ActorSystem(int maximumThreadsNumber) {
        logger.debug("Maximum number of thread is {}", maximumThreadsNumber);

        final ForkJoinWorkerThreadFactory usTFactory = new ForkJoinWorkerThreadFactory() {
            @Override
            public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
                final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                worker.setName("Actor-Thread-User-" + worker.getPoolIndex());
                return worker;
            }
        };

        userThreadPool = new ForkJoinPool(maximumThreadsNumber, usTFactory,
                null, false,
                maximumThreadsNumber,
                maximumThreadsNumber,
                1, null, 60, TimeUnit.SECONDS);
    }

    /**
     * Creates a new ActorSystem with thread pool with number of threads according number of processors.
     */
    public ActorSystem() {
        this(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Create an actor in the system.
     *
     * @param actor actor assigned to the system
     */
    public <T> ActorRef<T> actorOf(Actor<T> actor) {
        actor.setActorSystem(this);
        actor.markAsScheduled();
        userThreadPool.execute(actor.getPreStartRunnable());
        return actor.getActorRef();
    }

    /**
     * Create a router in the system.
     *
     * @param router router assigned to the system
     */
    public <RouteeMessageType, RoutingType extends RouteeMessageType> ActorRef<RoutingType> actorOf(Router<RouteeMessageType, RoutingType> router) {
        router.setActorSystem(this);
        return router.getActorRef();
    }

    /**
     * Blocks until all actors in the system are idle.
     *
     * @param timeout max blocking time
     * @param unit    unit for timeout
     */
    public boolean waitOnIdle(long timeout, TimeUnit unit) {
        return userThreadPool.awaitQuiescence(timeout, unit);
    }

    Executor getUserThreadPool() {
        return userThreadPool;
    }
}
