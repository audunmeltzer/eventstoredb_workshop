package eventstoredb.workshop.events.producer.events

import java.util.*

class Withdrawal(val id: UUID, val user: String, val amount: Long)