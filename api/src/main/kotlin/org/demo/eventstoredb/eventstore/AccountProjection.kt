package org.demo.eventstoredb.eventstore

import com.eventstore.dbclient.ResolvedEvent
import com.eventstore.dbclient.Subscription
import com.eventstore.dbclient.SubscriptionListener
import org.demo.eventstoredb.api.model.Account

const val BY_CATEGORY_STREAM_NAME = "\$ce-$STREAM_NAME"

class AccountProjection : SubscriptionListener() {

    val accounts = mutableMapOf<String, Account>()

    @Override
    override fun onEvent(subscription: Subscription, resolvedEvent: ResolvedEvent) {
        val aggregateId = extractAccountIdFromResolvedEvent(resolvedEvent)
        val event = resolvedEvent.event.toEvent()
        //TODO update accounts with new account balance
    }

    private fun extractAccountIdFromResolvedEvent(resolvedEvent: ResolvedEvent) =
        resolvedEvent.event.streamId.removePrefix("$STREAM_NAME-")

}
