package org.demo.eventstoredb.eventstore.events

data class AccountCreated(val id: String, val name: String) : Event