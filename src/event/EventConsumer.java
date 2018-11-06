package event;

import java.util.function.Consumer;

/**
 * A consumer that may throw an unchecked RuntimeException in the case of any exception arising.
 */
@FunctionalInterface
public interface EventConsumer<T> extends Consumer<T> {

	@Override
	default void accept(T element) {
		try {
			acceptOrThrow(element);
		} catch (Throwable exception) {
			throw new RuntimeException(exception);
		}
	}

	void acceptOrThrow(T element) throws Throwable;

}
