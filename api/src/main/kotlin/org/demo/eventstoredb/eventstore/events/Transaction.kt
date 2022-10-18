package org.demo.eventstoredb.eventstore.events

interface Transaction {
    val description: String
    val amount: Long
}