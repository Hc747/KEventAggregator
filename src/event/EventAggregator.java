package event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The event aggregator is responsible for managing event handlers and dispatching messages to event handlers.
 *
 * References / Inspirations:
 * - https://martinfowler.com/eaaDev/EventAggregator.html
 * - https://github.com/Discord4J/Discord4J/blob/master/src/main/java/sx/blah/discord/api/events/EventDispatcher.java
 */
public class EventAggregator {

	private static final Logger logger = Logger.getLogger(EventAggregator.class.getSimpleName());

	private final MethodHandles.Lookup lookup = MethodHandles.lookup();
	private final Set<EventHandler> chain = ConcurrentHashMap.newKeySet();

	/**
	 * Dispatches an event to all handlers registered within the chain.
	 *
	 * Caveats:
	 * - The calling thread is responsible for executing each event handler, thus, this method is potentially blocking.
	 */
	public void dispatch(Event event) {
		chain.stream().filter(handler -> handler.accepts(event)).forEach(handler -> {
			try {
				handler.invoke(event);
			} catch (Throwable e) {
				logger.log(Level.WARNING, String.format("An exception occurred whilst processing an event of type: %s - data: %s", event.getClass().getSimpleName(), event), e);
			}
		});
	}

	/**
	 * Registers event handler methods belonging to a class instance.
	 */
	public void register(Class<?> listenerClass) {
		register(listenerClass, null);
	}

	/**
	 * Registers event handler methods belonging to an object instance.
	 */
	public void register(Object listener) {
		register(listener.getClass(), listener);
	}

	/**
	 * Registers event handlers annotated with the EventHandler method annotation.
	 *
	 * Caveats:
	 * - Only publicly accessible methods are considered.
	 * - EventHandler methods must accept one argument of type Event
	 */
	private void register(Class<?> listenerClass, Object listener) {
		Stream<Method> eventHandlerMethods = Arrays.stream(listenerClass.getMethods()).filter(m -> m.isAnnotationPresent(event.EventHandler.class));

		if (listener == null)
			eventHandlerMethods = eventHandlerMethods.filter(m -> Modifier.isStatic(m.getModifiers()));
		else
			eventHandlerMethods = eventHandlerMethods.filter(m -> !Modifier.isStatic(m.getModifiers()));

		List<EventHandler> handlers = eventHandlerMethods.map(method -> {

			if (method.getParameterCount() != 1)
				throw new IllegalArgumentException("EventHandler methods must accept only one argument. Invalid method " + method);

			Class<?> eventClass = method.getParameterTypes()[0];
			if (!Event.class.isAssignableFrom(eventClass))
				throw new IllegalArgumentException("Argument type is not an Event nor a subclass of it. Invalid method " + method);

			method.setAccessible(true);
			try {
				MethodHandle methodHandle = lookup.unreflect(method);
				if (listener != null)
					methodHandle = methodHandle.bindTo(listener);
				return new MethodEventHandler(eventClass, methodHandle, listener);
			} catch (IllegalAccessException ex) {
				throw new IllegalStateException("Method " + method + " is not accessible", ex);
			}

		}).collect(Collectors.toList());

		chain.addAll(handlers);

		logger.info(String.format("Registered %d event(s) %s %s.", handlers.size(), listener == null ? "to the class" : "to an instance of the class", listenerClass.getSimpleName()));
	}

	/**
	 * Represents a predicate and consumer for event handling;
	 * if an event is accepted by a handler, the handler shall be invoked in response to the event.
	 *
	 * Package private as implementation is internal.
	 */
	private interface EventHandler {

		boolean accepts(Event event);

		void invoke(Event event) throws Throwable;

	}

	private static final class MethodEventHandler implements EventHandler {

		/**
		 * The class of events to handle.
		 */
		final Class<?> adapter;

		/**
		 * The method to invoke in response to the event.
		 */
		final MethodHandle handle;

		/**
		 * This object may be null.
		 * A reference to the object (if any) which is to invoke the event handling method in order to prevent GC.
		 */
		final Object instance;

		MethodEventHandler(Class<?> adapter, MethodHandle handle, Object instance) {
			this.adapter = adapter;
			this.handle = handle;
			this.instance = instance;
		}

		@Override
		public boolean accepts(Event event) {
			return adapter.isInstance(event);
		}

		@Override
		public void invoke(Event event) throws Throwable {
			handle.invoke(event);
		}

	}

}
