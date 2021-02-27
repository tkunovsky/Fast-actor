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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Messages can be sent via a router to efficiently route them to destination actors, known as its routees.
 * A Router can be used inside or outside of an actor. It acts as special actor.
 *
 * Different routing strategies can be used, according to your application’s needs.
 * It comes with several useful routing strategies right out of the box. But it is also possible to create your own.
 *
 * @param <RouteeMessageType> base type of routee messages
 * @param <RoutingType> type of messages which you need to route
 */
public final class Router<RouteeMessageType, RoutingType extends RouteeMessageType> {
    private RoutingLogic<RouteeMessageType, RoutingType> routingLogic;
    private List<Routee<RouteeMessageType>> routees;
    private List<Actor<RouteeMessageType>> actors;
    private InternalRouter internalRouter;

    private static final Logger logger = LogManager.getLogger(Router.class);

    /**
     * Interface for implementation of custom routing logic.
     *
     * @param <RouteeMessageType> base type of routee messages
     * @param <RoutingMessageType> type of messages which you need to route
     */
    public interface RoutingLogic<RouteeMessageType, RoutingMessageType extends RouteeMessageType> {
        /**
         * Called when a message need to be routed.
         *
         * @param message message which will be routed
         * @param routees candidates for the message processing
         */
        void select(RoutingMessageType message, List<Routee<RouteeMessageType>> routees);
    }

    /**
     * A broadcast router forwards the message it receives to all its routees.
     *
     * @param <RouteeMessageType> base type of routee messages
     * @param <RoutingType> type of messages which you need to route
     */
    public static class BroadcastRoutingLogic<RouteeMessageType, RoutingType extends RouteeMessageType> implements RoutingLogic<RouteeMessageType, RoutingType> {

        @Override
        public void select(RoutingType message, List<Routee<RouteeMessageType>> routees) {
            for (Routee<RouteeMessageType> routee: routees) {
                routee.getActorRef().tell(message);
            }
        }
    }

    /**
     * Routes in a round-robin fashion to its routees
     *
     * @param <RouteeMessageType> base type of routee messages
     * @param <RoutingType> type of messages which you need to route
     */
    public static class RoundRobinRoutingLogic<RouteeMessageType, RoutingType extends RouteeMessageType> implements RoutingLogic<RouteeMessageType, RoutingType> {
        private int lastIndex = 0;

        @Override
        public void select(RoutingType message, List<Routee<RouteeMessageType>> routees) {
            int targetRoutee = lastIndex;

            if (targetRoutee >= routees.size()) {
                targetRoutee = 0;
            }

            routees.get(targetRoutee).getActorRef().tell(message);
            lastIndex++;
        }
    }

    /**
     * The ConsistentHashingRoutingLogic uses hashFunction to select a routee based on the sent message.
     *
     * @param <RouteeMessageType> base type of routee messages
     * @param <RoutingType> type of messages which you need to route
     */
    public static class ConsistentHashingRoutingLogic<RouteeMessageType, RoutingType extends RouteeMessageType> implements RoutingLogic<RouteeMessageType, RoutingType> {
        private final Function<RoutingType, Long> hashFunction;

        /**
         *
         * @param hashFunction hash function for message distribution
         */
        public ConsistentHashingRoutingLogic(Function<RoutingType, Long> hashFunction) {
            this.hashFunction = hashFunction;
        }

        @Override
        public void select(RoutingType message, List<Routee<RouteeMessageType>> routees) {
            int targetRoutee = (int) (hashFunction.apply(message) % routees.size());
            routees.get(targetRoutee).getActorRef().tell(message);
        }
    }

    /**
     *
     * @param routingLogic logic for target routee selection
     * @param actorFactory factory for routee
     * @param poolSize number of routee which will be created
     */
    public Router(RoutingLogic<RouteeMessageType, RoutingType> routingLogic, Supplier<Actor<RouteeMessageType>> actorFactory, int poolSize) {
        init(routingLogic, supplierToList(actorFactory, poolSize), null);
    }

    /**
     *
     * @param name name of router (which set it as name of his actor)
     * @param routingLogic logic for target routee selection
     * @param actorFactory factory for routee
     * @param poolSize number of routee which will be created
     */
    public Router(String name, RoutingLogic<RouteeMessageType, RoutingType> routingLogic,
                  Supplier<Actor<RouteeMessageType>> actorFactory, int poolSize) {
        init(routingLogic, supplierToList(actorFactory, poolSize), name);
    }

    /**
     *
     * @param routingLogic logic for target routee selection
     * @param actors list of actors which will be used as routee
     */
    public Router(RoutingLogic<RouteeMessageType, RoutingType> routingLogic, List<Actor<RouteeMessageType>> actors) {
        init(routingLogic, actors, null);
    }

    /**
     *
     * @param name name of router (which set it as name of his actor)
     * @param routingLogic logic for target routee selection
     * @param actors list of actors which will be used as routee
     */
    public Router(String name, RoutingLogic<RouteeMessageType, RoutingType> routingLogic, List<Actor<RouteeMessageType>> actors) {
        init(routingLogic, actors, name);
    }

    /**
     * Creates list of actors using factory
     *
     * @param actorFactory factory for routee
     * @param poolSize number of routee which will be created
     * @param <T> base type of routee messages
     * @return created list of actors
     */
    public static <T> List<Actor<T>> supplierToList(Supplier<Actor<T>> actorFactory, int poolSize) {
        List<Actor<T>> actors = new ArrayList<>(poolSize);
        for (int r = 0; r < poolSize; r++) {
            Actor<T> actor = actorFactory.get();
            actors.add(actor);
        }
        return actors;
    }

    private class InternalRouter extends Actor<RoutingType> {
        private InternalRouter(String name) {
            super(name != null ? name : "Router<" + generateUniqueString(Router.this) + ">");
        }

        @Override
        protected void onMessage(RoutingType message) {
            routingLogic.select(message, routees);
        }
    }

    void setActorSystem(ActorSystem actorSystem) {
        internalRouter.setActorSystem(actorSystem);
        for (Actor<RouteeMessageType> actor:actors) {
            actor.setActorSystem(actorSystem);
        }
    }

    ActorRef<RoutingType> getActorRef() {
        return internalRouter.getActorRef();
    }

    // TODO managemet messages - https://doc.akka.io/docs/akka/current/routing.html#router-usage ?
    // TODO implementation of more RoutingLogic - https://doc.akka.io/docs/akka/current/routing.html#router-usage

    private void init(RoutingLogic<RouteeMessageType, RoutingType> routingLogic, List<Actor<RouteeMessageType>> actors, String name) {
        this.routingLogic = routingLogic;
        this.routees = new ArrayList<>(actors.size());
        this.actors = new ArrayList<>(actors.size());
        this.internalRouter = new InternalRouter(name);
        for (int r = 0; r < actors.size(); r++) {
            Actor<RouteeMessageType> actor = actors.get(r);
            routees.add(actor.getRoutee());
            this.actors.add(actor);
        }
    }
}
