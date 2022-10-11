package eventstoredb.workshop.eventstore.events

class Deposit(override val amount: Long, override val description: String) : Event, Transaction