package eventstoredb.workshop.eventstore

import com.eventstore.dbclient.ResolvedEvent
import com.eventstore.dbclient.Subscription
import com.eventstore.dbclient.SubscriptionListener
import eventstoredb.workshop.eventstore.events.Created
import eventstoredb.workshop.eventstore.events.Deposit
import eventstoredb.workshop.eventstore.events.Withdrawal
import eventstoredb.workshop.model.Account

val BY_CATEGORY_STREAM_NAME = "\$ce-account"

class AccountProjection : SubscriptionListener() {

    val accounts = mutableMapOf<String, Account>()

    @Override
    override fun onEvent(subscription: Subscription, event: ResolvedEvent) {
        println("Received event"
                + event.originalEvent.streamRevision.valueUnsigned
                + "@" + event.originalEvent.streamId
        );
        handleEvent(event);
    }

    private fun handleEvent(resolvedEvent: ResolvedEvent) {
        val aggregateId = resolvedEvent.event.streamId.removePrefix("$STREAM_NAME_PREFIX-")
        val event = resolvedEvent.event.toEvent()
        println("Handle event ${resolvedEvent.event.eventId} from projection on aggregate $aggregateId")
        when(event){
            is Created -> accounts[aggregateId] = AccountAggregate().handle(event).build()
            is Deposit -> {
                accounts[aggregateId]?.let { account ->
                    accounts.put(aggregateId, account.copy(amount = account.amount + event.amount))
                }
            }
            is Withdrawal -> accounts[aggregateId]?.let { account ->
                accounts.put(aggregateId, account.copy(amount = account.amount - event.amount))
            }
        }
    }

}
