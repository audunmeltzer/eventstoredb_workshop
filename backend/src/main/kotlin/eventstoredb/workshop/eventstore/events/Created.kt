package eventstoredb.workshop.eventstore.events


data class Created(val id: String, val name: String) : Event