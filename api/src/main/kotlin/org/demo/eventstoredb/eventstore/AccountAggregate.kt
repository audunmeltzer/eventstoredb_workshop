package org.demo.eventstoredb.eventstore

import org.demo.eventstoredb.eventstore.events.Created
import org.demo.eventstoredb.eventstore.events.Deposit
import org.demo.eventstoredb.eventstore.events.Event
import org.demo.eventstoredb.eventstore.events.Withdrawal
import org.demo.eventstoredb.eventstore.events.Transaction
import org.demo.eventstoredb.api.model.Account

class AccountAggregate {

    var id: String? = null
    var amount: Long = 0L
    val transactions = mutableListOf<Transaction>()

    fun handle(event: Event) = apply {
        when(event){
            is Created -> id = event.id
            is Withdrawal -> {
                amount -= event.amount
                transactions.add(event)
            }
            is Deposit -> {
                amount += event.amount
                transactions.add(event)
            }
        }
    }

    fun build() = Account(this.id ?: throw RuntimeException("Account not propperly initialized"), this.amount, this.transactions.map { event -> toTransaction(event) }.toList())
    private fun toTransaction(event: Transaction): org.demo.eventstoredb.api.model.Transaction = org.demo.eventstoredb.api.model.Transaction(event.description, event.amount)
}
