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
Commonly we create variables and allow any thread to modify them-in a controlled fashion. It's called shared mutability. Programming with shared mutability is simply the way of life for most of us Java programmers, but this leads to the undesirable synchronize and suffer model. We have to ensure that code crosses the memory barrier at the appropriate time and have good visibility on the variables. With shared mutability, we must also ensure that no two threads modify a field at the same time and that changes to multiple fields are consistent. We get no support from the compiler or the runtime to determine correctness; we have to analyze the code to ensure we did the right thing. The minute we touch the code, we have to reanalyze for correctness, because synchronization is too easy to get wrong.

An alternate middle ground to deal with state is isolated mutability, where variables are mutable but are never seen by more than one thread, ever. We ensure that anything that’s shared between threads is immutable. Java programmers find this fairly easy to design, and so the isolated mutability may be a reasonable approach. This approach can be reached by Actors.

Moreover isolated mutability also works more effectively with modern processor architecture [NUMA](https://en.wikipedia.org/wiki/Non-uniform_memory_access). Modern GC as [ZGC](https://wiki.openjdk.java.net/display/zgc/Main) can store isolated state in memory owned by processor on which its thread runs and [affinity](https://en.wikipedia.org/wiki/Processor_affinity) then can map this thread on the same processor repeatedly.

## Design
There are four points which improve performance of Actors significantly: mapping of Actors to threads, batching, GC optimalization and fast mailbox.
### Mapping of actors
Fast Actor library uses advanced features of [ForkJoinPool](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ForkJoinPool.html) to map groups of Actors which comunicate frequently together on the same thread. Example is figured on the following picture:

![animation (2)](https://user-images.githubusercontent.com/9279768/109640375-b3425100-7b50-11eb-8ca0-63c3ec152ae5.gif)

It's without limitation of comunication amoung Actors, offers decreased overhead from parallel synchronisation primitives and is also very cache friendly.

### Batching
For maximization of throughput messages sent to Actors are proccesed in batches. It reduces overhead of Actors scheduling and improves cache locality. On the other hand it has negative impact on fairness because some Actors can be blocked by other with higher number of messages. As workaroud more Actor systems can be created for Actors with different CPU utilization requirements. The Fast Actor library supports comunication of Actors from different its Actor System implicitly.

### GC optimalization
Garbage Collertor is invoked when a memory allocation request fails or reach a level, which happens at a frequency proportional to the rate memory is allocated. And GC runs for a time proportional to the number of live objects. These two metrics determine total GC time and both cases are optimalized in Fast Actor library. Amount of memory used and newly allocated is minimal compared to other Actor libraries.

### Extremely fast wait-free mailbox
[Non-intrusive MPSC node-based queue from D. Vyukov](http://www.1024cores.net/home/lock-free-algorithms/queues/non-intrusive-mpsc-node-based-queue) is used for implementaion of mailbox.

### Messages and Actor state
Visibility of all fields defined inside of Actors are guaranted by [Happens-Before](https://javarevisited.blogspot.com/2020/01/what-is-happens-before-in-java-concurrency.html#axzz6nysLoMrT) rules. It's thread-safe only in case when these fields are used only by Actor which own them (by their methods `preStart` and `onMessage`). Message doesn't need to be immutable but after it is sent to Actor, its modification can cause a race condition.   

### API
Simplified API from [Akka Actor](https://doc.akka.io/docs/akka/current/actors.html) framework was used for Fast Actor library. It should make learning curve more favorable for people which know it. The API is also more friendly than API of Akka Actor framework for Java applications.   

## Build
This library uses Maven, you can build it and get desired jar file with `mvn package`.

## How to use it
### Defining an Actor class
Actors are implemented by extending the Actor class and implementing the `onMessage` method. The `onMessage` method should define a series of case/if statements that defines which messages your Actor can handle.

Here is an example:

```java
import com.fastactor.Actor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyActor extends Actor<MyActor.Message> {
    private static final Logger logger = LogManager.getLogger(MyActor.class);

    interface Message {}

    static public class TextMessages implements Message {
        private final String text;

        public TextMessages(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    @Override
    protected void onMessage(Message m) {
        if (m instanceof TextMessages) {
            processMessage((TextMessages) m);
        } else {
            logger.error("received unknown message");
        }
    }

    private void processMessage(TextMessages textMessages) {
        logger.info("Received Text message: {}", textMessages.getText());
    }
}
```

### Send messages
Messages are sent to an Actor through method `ActorRef.tell`. `tell` means "fire-and-forget", e.g. send a message asynchronously and return immediately. Message ordering is guaranteed on a per-sender basis.

### Tell: Fire-forget
This is the only way of sending messages. No blocking waiting for a message. This gives the best concurrency and scalability characteristics:

```java
ref.tell(new MyActor.TextMessages("Hello World!"));
```

To acquire an ActorRef you have to assign your Actor to `ActorSystem`.

```java
ActorSystem actorSystem = new ActorSystem();
ActorRef<MyActor.Message> ref = actorSystem.actorOf(new MyActor());
```

### Start Hook
Right after assigning the Actor to an instance of `ActorSystem`, its `preStart` method is invoked.

```java
@Override
protected void preStart() {
  // e.g. send an init message
}
```

### API
It offers:
- `getSelf()` reference to the ActorRef of the actor
- `getName()` each actor has a name which is set by constructor or generated

### Router
In some cases it is useful to distribute messages of the same type over a set of actors, so that messages can be processed in parallel - a single (or group) actor(s) will only process one message at a time.

The router itself forwards any message sent to it to one (or a few) final recipient(s) out of the set of routees.

```java
List<Actor<MyActor.Message>> myActors = Router.supplierToList(new MyActorFactory(), 100);
router = new Router<>("Broadcast-to-100-actors", new Router.BroadcastRoutingLogic<>(), myActors);
ActorRef<> routerRef = actorSystem.actorOf(router);
routerRef.tell(new MyActor.TextMessages("Hello 100 times!"));
```

## Performance
For performance measure [Akka Actor](https://doc.akka.io/docs/akka/current/typed/actors.html) framework (v2.6.10_2.13) was used for comparison. Version of Fast Actor was 0.1. It was tested on HW [AMD RYZEN 5 3600](https://www.amd.com/en/products/cpu/amd-ryzen-5-3600) and [HyperX 16GB KIT DDR4 3200MHz CL16 Predator Series](https://www.amazon.com/Kingston-Technology-HyperX-HX432C16PB3K2-16/dp/B01GCWQ8VO). There were three simple test. 

### Simple ping-pong test
First it's simple ping-pong test where there are groups which contain 2 actors (ping and pong). In graph (lower value is better) you can see Fast Actor library is more than twice faster:

![image](https://user-images.githubusercontent.com/9279768/110137879-a2ecd900-7dd1-11eb-9ccd-6de3e9bda48f.png)

You can find source code of tests in [AkkaActorSystemAdvancedTest](https://github.com/tkunovsky/Fast-actor/blob/main/src/test/java/com/fastactor/AkkaActorSystemAdvancedTest.java)::manyActorsFewMessages and [ActorSystemAdvancedTest](https://github.com/tkunovsky/Fast-actor/blob/main/src/test/java/com/fastactor/ActorSystemAdvancedTest.java)::manyActorsFewMessages.

## License
Fast Actor is Open Source and available under the Apache 2 License.
