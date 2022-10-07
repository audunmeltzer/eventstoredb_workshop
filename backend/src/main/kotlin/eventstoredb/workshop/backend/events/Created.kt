package eventstoredb.workshop.backend.events

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

@Serializable
data class Created(var id: String?, var timestamp: Long?)