package eventstoredb.workshop.backend.events

import kotlinx.serialization.Serializable

@Serializable
data class Created(var id: String, var timestamp: Long) : Event