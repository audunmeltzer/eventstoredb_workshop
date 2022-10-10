package eventstoredb.workshop.services

import eventstoredb.workshop.events.Created
import eventstoredb.workshop.events.Deposit
import eventstoredb.workshop.events.Event
import eventstoredb.workshop.events.Withdrawal
import eventstoredb.workshop.model.Account

class AccountAggregate {
    var id: String? = null
    var amount: Long = 0L

    fun handle(event: Event) = apply {
        when(event){
            is Created -> id = event.id
            is Withdrawal -> amount -= event.amount
            is Deposit -> amount += event.amount
        }
    }

    fun build() = Account(this.id ?: throw RuntimeException("Account not propperly initialized"), this.amount)
}