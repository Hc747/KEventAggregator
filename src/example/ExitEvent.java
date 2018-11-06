package example;

import event.Event;

public class ExitEvent extends Event {

	final int code;

	ExitEvent(int code) {
		this.code = code;
	}

}
