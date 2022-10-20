package org.demo.eventstoredb.eventstore

import com.eventstore.dbclient.ResolvedEvent
import com.eventstore.dbclient.Subscription
import com.eventstore.dbclient.SubscriptionListener
import org.demo.eventstoredb.eventstore.events.AccountCreated
import org.demo.eventstoredb.eventstore.events.Deposit
import org.demo.eventstoredb.eventstore.events.Withdrawal
import org.demo.eventstoredb.api.model.Account

const val BY_CATEGORY_STREAM_NAME = "\$ce-$STREAM_NAME"

class AccountProjection : SubscriptionListener() {

    val accounts = mutableMapOf<String, Account>()

    @Override
    override fun onEvent(subscription: Subscription, resolvedEvent: ResolvedEvent) {
        val aggregateId = extractAccountIdFromResolvedEvent(resolvedEvent)
        val event = resolvedEvent.event.toEvent()
        println("Handle event ${resolvedEvent.event.eventId} from projection on aggregate $aggregateId")
        when(event){
            is AccountCreated -> accounts[aggregateId] = AccountAggregate().handle(event).build()
            is Deposit -> {
                accounts[aggregateId]?.let { account ->
                    accounts.put(aggregateId, account.copy(balance = account.balance + event.amount))
                }
            }
            is Withdrawal -> accounts[aggregateId]?.let { account ->
                accounts.put(aggregateId, account.copy(balance = account.balance - event.amount))
            }
        }
    }

    private fun extractAccountIdFromResolvedEvent(resolvedEvent: ResolvedEvent) =
        resolvedEvent.event.streamId.removePrefix("$STREAM_NAME-")

}
