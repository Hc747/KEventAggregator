package example;

import event.Event;
import event.EventAggregator;
import event.EventHandler;

import java.util.Arrays;
import java.util.List;

public final class EventAggregatorHelloWorld {

	public static void main(String[] args) {

		final EventAggregator aggregator = new EventAggregator();

		aggregator.register(EventAggregatorHelloWorld.class); //register static methods belonging to the class 'EventAggregatorHelloWorld'
		aggregator.register(new GoodbyeGreeter()); //register instance methods belonging to the 'GoodbyeGreeter' object

		List<Event> events = Arrays.asList(new GreetingEvent("World"), new GreetingEvent("Harrison"));

		events.forEach(aggregator::dispatch);
	}

	@EventHandler
	public static void FrenchGreeter(GreetingEvent event) {
		System.out.println(String.format("Bonjour %s", event.who));
	}

	@EventHandler
	public static void EnglishGreeter(GreetingEvent event) {
		System.out.println(String.format("Hello %s", event.who));
	}

	@EventHandler
	public static void GermanGreeter(GreetingEvent event) {
		System.out.println(String.format("Hallo %s", event.who));
	}

	public static class GoodbyeGreeter {

		@EventHandler
		public void sayGoodbye(GreetingEvent event) {
			System.out.println(String.format("Goodbye %s", event.who));
		}

	}

}
