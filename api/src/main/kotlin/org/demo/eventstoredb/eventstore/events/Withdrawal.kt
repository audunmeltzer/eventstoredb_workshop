package org.demo.eventstoredb.eventstore.events

class Withdrawal(override val amount: Long, override val description: String) : Event, Transaction