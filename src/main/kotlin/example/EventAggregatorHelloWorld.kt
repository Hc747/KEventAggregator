package example

import event.DispatchMode
import event.Event
import event.EventAggregator
import event.EventHandler
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import kotlin.system.exitProcess

object EventAggregatorHelloWorld {

    @JvmStatic
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    fun main(args: Array<String>) {
        val aggregator = EventAggregator()

        //register static methods belonging to the class 'EventAggregatorHelloWorld'
        aggregator.register(EventAggregatorHelloWorld::class)

        //register instance methods belonging to the 'GoodbyeGreeter' object
        aggregator.register(GoodbyeGreeter())

        //register a standalone event handler
        aggregator.onEvent(ExitEvent::class) { event ->
            println("Exit requested; received with code: ${event.code}.")
            exitProcess(event.code)
        }

        val events = listOf(
                GreetingEvent("World"),
                GreetingEvent("Harrison")
        )

        //dispatch events in a thread blocking manner (synchronously); execution time: ~10 seconds
        aggregator.dispatchAll(events, mode = DispatchMode.BLOCKING)
        //dispatch events in a non-blocking manner (asynchronously); execution time: ~5 seconds
        aggregator.dispatchAll(events, mode = DispatchMode.PARALLEL).whenComplete { _, _ -> aggregator.dispatch(ExitEvent()) }

        while (true) {
            println("${Thread.currentThread().name} thread suspended")
            Thread.currentThread().suspend()//in a loop to illustrate that it's only called once
        }
    }

    @JvmStatic
    @EventHandler
    fun Logger(event: Event) {
        println("[LOGGER] Received event: $event at ${Date().toInstant()} on thread ${Thread.currentThread().name}")
    }

    @JvmStatic
    @EventHandler
    @Throws(InterruptedException::class)
    fun BlockingGreeter(event: GreetingEvent) {
        println("Starting blocking for 5 seconds, ${event.who}")
        Thread.sleep(5000)
        println("Done blocking for 5 seconds, ${event.who}")
    }

    @JvmStatic
    @EventHandler
    fun FrenchGreeter(event: GreetingEvent) {
        println("Bonjour ${event.who}")
    }

    @JvmStatic
    @EventHandler
    fun EnglishGreeter(event: GreetingEvent) {
        println("Hello ${event.who}")
    }

    @JvmStatic
    @EventHandler
    fun GermanGreeter(event: GreetingEvent) {
        println("Hallo ${event.who}")
    }

    class GoodbyeGreeter {

        @EventHandler
        fun sayGoodbye(event: GreetingEvent) {
            println("Goodbye ${event.who}")
        }

    }

}
