package eventstoredb.workshop.eventstore

import eventstoredb.workshop.eventstore.events.Created
import eventstoredb.workshop.eventstore.events.Deposit
import eventstoredb.workshop.eventstore.events.Event
import eventstoredb.workshop.eventstore.events.Withdrawal
import eventstoredb.workshop.model.Account
import eventstoredb.workshop.eventstore.events.Transaction

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
    private fun toTransaction(event: Transaction): eventstoredb.workshop.model.Transaction = eventstoredb.workshop.model.Transaction(event.description, event.amount)
}
