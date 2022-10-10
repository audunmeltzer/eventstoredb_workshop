package eventstoredb.workshop

import com.eventstore.dbclient.Endpoint
import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBClientSettings
import com.eventstore.dbclient.SubscribeToStreamOptions
import eventstoredb.workshop.services.AccountProjection
import eventstoredb.workshop.services.BY_CATEGORY_STREAM_NAME
import eventstoredb.workshop.services.EventstoreRepo
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.StringSpec
import io.kotest.framework.concurrency.eventually
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.testcontainers.containers.GenericContainer
import java.lang.Thread.sleep
import java.util.*

@OptIn(ExperimentalKotest::class)
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
    lateinit var eventstoreService: EventstoreRepo

    lateinit var client: EventStoreDBClient

    beforeTest {
        println("Before every spec/test case")
    }


    beforeSpec {
        println("Before every test suite")
        eventstoreDb.start()

        var count = 0
        while(!eventstoreDb.isHealthy && count < 50){
            withContext(Dispatchers.IO) {
                sleep(500L)
            }
            count++
        }
        println("EventstoreDB started on host ${eventstoreDb.host}:${eventstoreDb.getMappedPort(2113)}, after $count retries")

        client = EventStoreDBClient.create(
                EventStoreDBClientSettings.builder().addHost(Endpoint(eventstoreDb.host, eventstoreDb.getMappedPort(2113)))
                    .tls(false)
                    .tlsVerifyCert(false).buildConnectionSettings()
                )
        eventstoreService = EventstoreRepo(client)
    }

    afterTest {
        println("After every spec/test case")
    }

    afterSpec {
        println("After every test suite")
        eventstoreDb.stop()
    }

    "Test create and get account" {
        val accountId = UUID.randomUUID().toString()
        eventstoreService.createAccount(accountId)

        val account = eventstoreService.getAccount(accountId)

        account shouldNotBe null
        account.id shouldBe accountId
        account.amount shouldBe 0L
    }

    "Test create account and deposit" {
        val accountId = UUID.randomUUID().toString()
        eventstoreService.createAccount(accountId)
        eventstoreService.deposit(accountId, 100)
        val account = eventstoreService.getAccount(accountId)

        account shouldNotBe null
        account.id shouldBe accountId
        account.amount shouldBe 100L
    }

    "Test create account and withdrawal" {
        val accountId = UUID.randomUUID().toString()
        eventstoreService.createAccount(accountId)
        eventstoreService.withdrawal(accountId, 100)
        val account = eventstoreService.getAccount(accountId)

        account shouldNotBe null
        account.id shouldBe accountId
        account.amount shouldBe -100L
    }

    "Test read accounts from projection" {
        val accountProjection = AccountProjection()
        client.subscribeToStream(BY_CATEGORY_STREAM_NAME, accountProjection, SubscribeToStreamOptions.get().resolveLinkTos().fromStart())

        val accountId = UUID.randomUUID().toString()
        eventstoreService.createAccount(accountId)

        eventually(5000) {
            val account = accountProjection.accounts[accountId]

            account shouldNotBe null
            account?.id shouldBe accountId
        }
    }


    "Account should have updated amount from projection state" {
        val accountProjection = AccountProjection()
        client.subscribeToStream(BY_CATEGORY_STREAM_NAME, accountProjection, SubscribeToStreamOptions.get().resolveLinkTos().fromStart())

        val accountId = UUID.randomUUID().toString()
        eventstoreService.createAccount(accountId)
        eventstoreService.deposit(accountId, 300)
        eventstoreService.withdrawal(accountId, 100)

        eventually(5000) {
            val account = accountProjection.accounts[accountId]

            account shouldNotBe null
            account?.id shouldBe accountId
            account?.amount shouldBe 200
        }
    }

})