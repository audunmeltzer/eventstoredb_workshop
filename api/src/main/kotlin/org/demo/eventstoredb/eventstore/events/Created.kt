package org.demo.eventstoredb.eventstore.events

data class Created(val id: String, val name: String) : Event