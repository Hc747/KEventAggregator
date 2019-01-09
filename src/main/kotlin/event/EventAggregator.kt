package event

import java.lang.reflect.Modifier
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.function.Consumer
import kotlin.reflect.KClass

/**
 * The event aggregator is responsible for managing event handlers and dispatching messages to event handlers.
 *
 *
 * References / Inspirations:
 * - https://martinfowler.com/eaaDev/EventAggregator.html
 * - https://github.com/Discord4J/Discord4J/blob/master/src/main/java/sx/blah/discord/api/events/EventDispatcher.java
 */
enum class DispatchMode {
    PARALLEL,
    BLOCKING
}

//TODO: look into CompletableDeferred / Deferred
class EventAggregator {

    //    private val lookup = MethodHandles.lookup()!! //TODO: when method handles are stable use the proper API
    private val chain = ConcurrentHashMap.newKeySet<EventHandler>()!!

    @JvmOverloads
    fun dispatch(event: Event, executor: ExecutorService? = null, mode: DispatchMode = DispatchMode.PARALLEL): CompletableFuture<Void> {
        val handlers = chain.filter { handler -> handler.accepts(event) }

        return when (mode) {
            DispatchMode.PARALLEL -> {
                val tasks = handlers.map { handler -> if (executor == null) CompletableFuture.runAsync { handler.invoke(event) } else CompletableFuture.runAsync(Runnable { handler.invoke(event) }, executor) }
                CompletableFuture.allOf(*tasks.toTypedArray())
            }
            DispatchMode.BLOCKING -> {
                handlers.forEach { handler -> handler.invoke(event) }
                CompletableFuture.completedFuture(null)
            }
        }
    }

    @JvmOverloads
    fun dispatchAll(events: List<Event>, executor: ExecutorService? = null, mode: DispatchMode = DispatchMode.PARALLEL): CompletableFuture<Void> {
        return CompletableFuture.allOf(*events.map { event -> dispatch(event, executor, mode) }.toTypedArray())
    }

    fun <T : Event> onEvent(event: KClass<T>, action: (T) -> Unit) {
        onEvent(event.java, action)
    }

    fun <T : Event> onEvent(event: Class<T>, action: (T) -> Unit) {
        chain.add(EventConsumingHandler(event, Consumer(action)))
    }

    fun register(listenerClass: KClass<*>) {
        register(listenerClass.java)
    }

    fun register(listenerClass: Class<*>) {
        register(listenerClass, null)
    }

    fun register(listener: Any) {
        register(listener::class.java, listener)
    }

    private fun register(`class`: Class<*>, listener: Any?) {
        val methods = `class`.methods
                .filter { (if (listener == null) Modifier.isStatic(it.modifiers) else !Modifier.isStatic(it.modifiers)) && it.isAnnotationPresent(event.EventHandler::class.java) }

        val handlers = methods.map { method ->

            if (method.parameterCount != 1) {
                throw IllegalArgumentException("EventHandler methods must accept only one argument. Invalid method $method")
            }

            val type = method.parameterTypes[0]

            if (!Event::class.java.isAssignableFrom(type)) {
                throw IllegalArgumentException("Argument type is not an Event nor a subclass of it. Invalid method $method")
            }

            method.isAccessible = true

//            val handle = if (listener == null) {
//                lookup.unreflect(method)
//            } else {
//                lookup.unreflect(method).bindTo(listener)
//            }
//
//            EventConsumingHandler(type, Consumer { event -> handle.invoke(event) })
            //TODO: when method handles are stable use the proper API

            EventConsumingHandler(type, Consumer { event -> method.invoke(listener, event) })
        }

        chain.addAll(handlers)

//        logger.info(String.format("Registered %d event(s) %s %s.", handlers.size, if (listener == null) "to the class" else "to an instance of the class", class.simpleName))
    }

    //TODO: builder implementation / DSL allowing for:
    // - removal from aggregator chain
    // - exception handling
    private interface EventHandler {

        fun accepts(event: Event): Boolean

        operator fun invoke(event: Event)

    }

    /**
     * @param adapter
     * The type of events to handle
     * @param action
     * The action to invoke in response to the event
     */
    private class EventConsumingHandler<T> internal constructor(
            private val adapter: Class<T>,
            private val action: Consumer<T>) : EventHandler {

        override fun accepts(event: Event): Boolean = adapter.isInstance(event)

        override fun invoke(event: Event) = action.accept(event as T)

    }

}