package eventstoredb.workshop.events.producer.events

import java.util.*

class Deposit(val id: UUID, val user: String, val amount: Long)