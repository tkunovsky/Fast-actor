# Fast-actor
## Description
Fast Actor is simple library for parallel programming with [Actors](http://en.wikipedia.org/wiki/Actor_model). It offers very low overhead which make it significantly faster than other library.

### Actor
Actor is the foundation on which you build the structure of your application, it has internal state invisible to the outer world and interacts with other actors through asynchronous messages.

Every actor is identified with a unique address by which you send messages to it. When a message is processed, it is matched against the current behavior of the actor; which is nothing more than a function that defines the actions to be taken in reaction to the message. In response to a message, an actor may:

- Create more actors.
- Send messages to other actors.
- Designate internal state to handle the next message.

### Motivation
Commonly we create variables and allow any thread to modify them—in a controlled fashion. It's called shared mutability. Programming with shared mutability is simply the way of life for most of us Java programmers, but this leads to the undesirable synchronize and suffer model. We have to ensure that code crosses the memory barrier at the appropriate time and have good visibility on the variables. With shared mutability, we must also ensure that no two threads modify a field at the same time and that changes to multiple fields are consistent. We get no support from the compiler or the runtime to determine correctness; we have to analyze the code to ensure we did the right thing. The minute we touch the code, we have to reanalyze for correctness, because synchronization is too easy to get wrong.

An alternate middle ground to deal with state is isolated mutability, where variables are mutable but are never seen by more than one thread, ever. We ensure that anything that’s shared between threads is immutable. Java programmers find this fairly easy to design, and so the isolated mutability may be a reasonable approach. This approach can be reached by Actors.

## Design
There are three points which improve performance of Actors significantly: mapping of Actors to threads, batching and GC optimalization.
### Mapping of actors
Fast Actor library uses advanced features of [ForkJoinPool](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ForkJoinPool.html) to map groups of Actors which comunicate frequently together on the same thread. Example is figured on the following picture:

![animation (2)](https://user-images.githubusercontent.com/9279768/109640375-b3425100-7b50-11eb-8ca0-63c3ec152ae5.gif)

It's without limitation of comunication amoung Actors, offers decreased overhead from parallel synchronisation primitives and is also very cache friendly.

### Batching
TODO

### GC optimalization
TODO

## Build
This library uses Maven, you can build it and get desired jar file with `mvn package`.

## License
Fast Actor is Open Source and available under the Apache 2 License.
