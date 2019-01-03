package example

import event.EventAggregator
import event.EventHandler
import event.ExecutionMode
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
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
        aggregator.dispatchAll(events, mode = ExecutionMode.BLOCKING)

        //dispatch events in a non-blocking manner (asynchronously); execution time: ~5 seconds
        val job = aggregator.dispatchAll(events, mode = ExecutionMode.CONCURRENT).thenRunAsync { aggregator.dispatch(ExitEvent(0)) }

        job.get(5500, TimeUnit.MILLISECONDS)
    }

    @JvmStatic
    @EventHandler
    @Throws(InterruptedException::class)
    fun BlockingGreeter(event: GreetingEvent) {
        Thread.sleep(5000)
        println(String.format("Blocked for 5 seconds, %s", event.who))
    }

    @JvmStatic
    @EventHandler
    fun FrenchGreeter(event: GreetingEvent) {
        println(String.format("Bonjour %s", event.who))
    }

    @JvmStatic
    @EventHandler
    fun EnglishGreeter(event: GreetingEvent) {
        println(String.format("Hello %s", event.who))
    }

    @JvmStatic
    @EventHandler
    fun GermanGreeter(event: GreetingEvent) {
        println(String.format("Hallo %s", event.who))
    }

    class GoodbyeGreeter {

        @EventHandler
        fun sayGoodbye(event: GreetingEvent) {
            println(String.format("Goodbye %s", event.who))
        }

    }

}
