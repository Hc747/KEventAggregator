package event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
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
	 * Registers a standalone event handler that listens to events of type T.
	 * The consumer can be provided in the following forms:
	 * 1). A class implementing the EventConsumer<T> interface
	 * 2). A method reference adhering to Consumer<T>#accept's method signature, for instance, System.out::println
	 * 3). A lambda expression, for instance, event -> {}
	 *
	 * This method accepts calls with checked exceptions.
	 */
	public <T extends Event> void onEvent(Class<T> event, EventConsumer<T> action) {
		chain.add(new EventConsumingHandler<>(event, action));
	}

	/**
	 * Same documentation as the method above, however, this method does not accept calls with checked exceptions.
	 */
	public <T extends Event> void onEvent(Class<T> event, Consumer<T> action) {
		chain.add(new EventConsumingHandler<>(event, action::accept));
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
		Stream<Method> methods = Arrays.stream(listenerClass.getMethods()).filter(m -> m.isAnnotationPresent(event.EventHandler.class));

		if (listener == null)
			methods = methods.filter(m -> Modifier.isStatic(m.getModifiers()));
		else
			methods = methods.filter(m -> !Modifier.isStatic(m.getModifiers()));

		List<EventHandler> handlers = methods.map(method -> {

			if (method.getParameterCount() != 1) {
				throw new IllegalArgumentException("EventHandler methods must accept only one argument. Invalid method " + method);
			}

			Class<?> type = method.getParameterTypes()[0];

			if (!Event.class.isAssignableFrom(type)) {
				throw new IllegalArgumentException("Argument type is not an Event nor a subclass of it. Invalid method " + method);
			}

			try {
				method.setAccessible(true);

				MethodHandle handle = lookup.unreflect(method);

				if (listener != null)
					handle = handle.bindTo(listener);

				return new EventConsumingHandler<>(type, handle::invoke);
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
	 */
	private interface EventHandler {

		boolean accepts(Event event);

		void invoke(Event event) throws Throwable;

	}

	private static final class EventConsumingHandler<T> implements EventHandler {

		/**
		 * The type of events to handle
		 */
		private final Class<T> adapter;

		/**
		 * The action to invoke in response to the event
		 */
		private final EventConsumer<T> action;

		EventConsumingHandler(Class<T> adapter, EventConsumer<T> action) {
			this.adapter = adapter;
			this.action = action;
		}

		@Override
		public boolean accepts(Event event) {
			return adapter.isInstance(event);
		}

		@Override
		public void invoke(Event event) throws Throwable {
			action.acceptOrThrow((T) event);
		}

	}

}
