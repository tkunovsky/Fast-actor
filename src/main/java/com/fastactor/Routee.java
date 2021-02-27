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

/**
 * Routee is destination actor of Router. This interface is for implementation of Router strategy.
 * @param <T> base message type of routee (actor).
 */
public interface Routee<T> {

    /**
     * Use for implementation of Router strategy which need info about current size of mailbox.
     *
     * @return number of messages waiting in mailbox
     */

    int getMailboxSize();

    /**
     * Use for sending message to routee.
     *
     * @return ActorRef of target actor (routee)
     */
    ActorRef<T> getActorRef();
}
