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
 * Immutable handle to an actor. An ActorRef
 * can be obtained from an {@link com.fastactor.ActorSystem}. This means
 * actors can be created in the ActorSystem or by existing actor.
 * <p>
 * ActorRefs can be freely shared among actors by message passing. Message
 * passing conversely is their only purpose, as demonstrated in the following
 *
 * @param <MessageType> base type of actor messages
 */
public interface ActorRef<MessageType> {

    /**
     * Sends the specified message to this ActorRef, i.e. fire-and-forget semantics.
     *
     * @param message message which is sent
     */
    void tell(MessageType message);
}
