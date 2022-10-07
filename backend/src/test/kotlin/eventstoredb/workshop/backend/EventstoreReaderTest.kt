package eventstoredb.workshop.backend

import com.eventstore.dbclient.Endpoint
import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBClientSettings
import eventstoredb.workshop.backend.services.EventstoreService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.GenericContainer
import org.testcontainers.shaded.org.hamcrest.Matchers
import org.testcontainers.shaded.org.hamcrest.Matchers.hasProperty
import org.testcontainers.shaded.org.hamcrest.beans.HasPropertyWithValue
import java.lang.Thread.sleep
import java.util.*

class EventstoreReaderTest : StringSpec({

    val eventstoreEnv = mutableMapOf<String,String>().also {
        it["EVENTSTORE_CLUSTER_SIZE"] = "1"
        it["EVENTSTORE_RUN_PROJECTIONS"] = "All"
        it["EVENTSTORE_START_STANDARD_PROJECTIONS"] = "True"
        it["EVENTSTORE_DISCOVER_VIA_DNS"] = "True"
        it["EVENTSTORE_ENABLE_ATOM_PUB_OVER_HTTP"] = "True"
        it["EVENTSTORE_INSECURE"] = "True"
    }
    val eventstoreDb = GenericContainer("eventstore/eventstore:22.6.0-buster-slim").withAccessToHost(true).withExposedPorts(2113).withEnv(eventstoreEnv.also { it.forEach {pair -> println("${pair.key}:${pair.value}")} })


    beforeTest {
        println("Before every spec/test case")
    }

    beforeSpec {
        println("Before every test suite")
        eventstoreDb.start()

        var count = 0
        while(!eventstoreDb.isHealthy && count < 50){
            sleep(500L)
            count++
        }
        println("EventstoreDB started on host ${eventstoreDb.host}:${eventstoreDb.getMappedPort(2113)}, after $count retries")
    }

    afterTest {
        println("After every spec/test case")
    }

    afterSpec {
        println("After every test suite")
        eventstoreDb.stop()
    }

    "Test create and list accounts" {
        val eventstoreService = EventstoreService(
            EventStoreDBClient.create(
                EventStoreDBClientSettings.builder().addHost(Endpoint(eventstoreDb.host, eventstoreDb.getMappedPort(2113))).tls(false)
                    .tlsVerifyCert(false).buildConnectionSettings()
            )
        )
        val accountId = UUID.randomUUID().toString()
        val accountId2 = UUID.randomUUID().toString()
        eventstoreService.createAccount(accountId)
        eventstoreService.createAccount(accountId2)
        val accounts = eventstoreService.getAccounts().accounts
        accounts.filter { it.id == accountId || it.id == accountId2 }.size shouldBe 2
    }

    "Test create account with events" {
        val eventstoreService = EventstoreService(
            EventStoreDBClient.create(
                EventStoreDBClientSettings.builder().addHost(Endpoint(eventstoreDb.host, eventstoreDb.getMappedPort(2113))).tls(false)
                    .tlsVerifyCert(false).buildConnectionSettings()
            )
        )
        val accountId = UUID.randomUUID().toString()
        eventstoreService.createAccount(accountId)
        //eventstoreService.writeTransaction(accountId, "deposit", 100)
        val accounts = eventstoreService.getAccounts().accounts
        accounts.shouldContain(HasPropertyWithValue<String>("id", Matchers.equalTo(accountId)))
    }

})