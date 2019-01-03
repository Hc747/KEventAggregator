package example

import event.Event

class GreetingEvent internal constructor(internal val who: String) : Event()
