package example;

import event.Event;

class GreetingEvent extends Event {

	final String who;

	GreetingEvent(String who) {
		this.who = who;
	}

}
