# Fast-actor
## Description
Fast Actor is simple library for parallel programming with [Actors](http://en.wikipedia.org/wiki/Actor_model). It offers very low overhead which make it significantly faster than other library.

### Actor
Actor is the foundation on which you build the structure of your application, it has internal state invisible to the outer world and interacts with other actors through asynchronous messages.

Every actor is identified with a unique address by which you send messages to it. When a message is processed, it is matched against the current behavior of the actor; which is nothing more than a function that defines the actions to be taken in reaction to the message. In response to a message, an actor may:

- Create more actors.
- Send messages to other actors.
- Designate internal state to handle the next message.

## Build
This library uses Maven, you can build it with `mvn package`.

## License
Fast Actor is Open Source and available under the Apache 2 License.
