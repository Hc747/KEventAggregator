#JEventAggregator
A simple Event Aggregator written in Java. 

An event aggregator is used to pass messages (events) to interested parties in a loosely coupled manner and is closely related to the Observer pattern.

More information on the Event Aggregator pattern is provided by Martin Fowler in his blog [here.](https://martinfowler.com/eaaDev/EventAggregator.html)

#Limitations
- Concurrency: this implementation is thread-safe, however, the calling thread is responsible for the execution of event handling code. In the future, asynchronous / non-blocking event handling should be introduced.
- Event Deregistration: this implementation at present date only supports the (dynamic) registration of event handlers. In the future, a mechanism for dynamically deregistering events should be implemented.

#Usage
- Create Event Handlers
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
EventAggregator aggregator = ...;
```

- Register Event Handlers with the Event Aggregator
```Java
aggregator.register(MyEventHandler.class); //register static methods
...
aggregator.register(new MyEventHandler()); //register instance methods
```

- Dispatch events to interested parties (event handlers)
```Java
aggregator.dispatch(new MyEvent());
```

##A reference implementation is provided in the 'example' package.