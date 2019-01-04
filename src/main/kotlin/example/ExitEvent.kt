package example

import event.Event

class ExitEvent internal constructor(internal val code: Int = 0) : Event()
