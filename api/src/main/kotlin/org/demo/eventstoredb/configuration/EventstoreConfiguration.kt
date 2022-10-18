package org.demo.eventstoredb.configuration

import com.eventstore.dbclient.Endpoint
import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBClientSettings
import com.eventstore.dbclient.SubscribeToStreamOptions
import org.demo.eventstoredb.eventstore.AccountProjection
import org.demo.eventstoredb.eventstore.BY_CATEGORY_STREAM_NAME
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EventstoreConfiguration {

    @Bean
    fun eventStoreDBClient() =
        EventStoreDBClient.create(
            EventStoreDBClientSettings.builder().addHost(
                Endpoint("eventstore", 2113)
            ).tls(false).tlsVerifyCert(false).buildConnectionSettings())

    @Bean
    fun AccountProjection(@Autowired eventStoreDBClient: EventStoreDBClient) =
        AccountProjection().also {
            eventStoreDBClient.subscribeToStream(BY_CATEGORY_STREAM_NAME, it, SubscribeToStreamOptions.get().resolveLinkTos().fromStart())
        }


}