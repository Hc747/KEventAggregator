# KEventAggregator
A simple Event Aggregator written in Kotlin.

An event aggregator is used to pass messages (events) to interested parties in a loosely coupled manner and is closely related to the Observer pattern.

More information on the Event Aggregator pattern is provided by Martin Fowler in his blog [here.](https://martinfowler.com/eaaDev/EventAggregator.html)

# Limitations
- Event Deregistration: this implementation at present date only supports the (dynamic) registration of event handlers. In the future, a mechanism for dynamically deregistering events should be implemented.
- Coroutines / Green threads: this implementation at present date only supports the JVM's native thread implementation. 

# Usage
- Create Event Handlers
## Java
```Java
class MyEventHandler {
    @EventHandler
    public /*static*/ void myEventHandlingMethod(MyEvent event) {
        ...
    }
}
```

- Create an Event Aggregator instance
```Java
EventAggregator aggregator = new EventAggregator();
```

- Register Event Handlers with the Event Aggregator
```Java
aggregator.register(MyEventHandler.class); //register static methods
...
aggregator.register(new MyEventHandler()); //register instance methods
...
aggregator.onEvent(MyEvent.class, event -> {...}); //register a standalone event handler 
```

- Dispatch events to interested parties (event handlers)
```Java
aggregator.dispatch(event, executor_service, execution_mode);
...
aggregator.dispatchAll(events, executor_service, execution_mode);
```

## Kotlin
```Kotlin
class MyEventHandler {
    @EventHandler
    /*@JvmStatic*/
    fun myEventHandlingFun(event: MyEvent) {
        ...
    }
}
```

- Create an Event Aggregator instance
```Kotlin
val aggregator = EventAggregator()
```

- Register Event Handlers with the Event Aggregator
```Kotlin
aggregator.register(MyEventHandler::class/*.java*/) //register static methods
...
aggregator.register(MyEventHandler()) //register instance methods
...
aggregator.onEvent(MyEvent::class/*.java*/) { event -> ... } //register a standalone event handler 
```

- Dispatch events to interested parties (event handlers)
```Kotlin
aggregator.dispatch(event, executor_service, execution_mode)
...
aggregator.dispatchAll(events, executor_service, execution_mode)
```

# A reference implementation is provided in the 'example' package.
