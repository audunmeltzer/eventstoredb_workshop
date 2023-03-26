package org.demo.eventstoredb.api

import com.eventstore.dbclient.*
import io.kotest.common.ExperimentalKotest
import io.kotest.core.NamedTag
import io.kotest.core.spec.style.FreeSpec
import io.kotest.framework.concurrency.eventually
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.demo.eventstoredb.eventstore.AccountProjection
import org.demo.eventstoredb.eventstore.BY_CATEGORY_STREAM_NAME
import org.demo.eventstoredb.eventstore.EventstoreRepo
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.GenericContainer
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.ExecutionException

lateinit var eventstoreService: EventstoreRepo
val accountProjection = AccountProjection()

@OptIn(ExperimentalKotest::class)
@Suppress("unused")
class AccountRepoTest : FreeSpec({

    val task1 = NamedTag("task1")
    val task2 = NamedTag("task2")
    val task3 = NamedTag("task3")
    val task4 = NamedTag("task4")


    "Task 1 tests".config(tags = setOf(task1)) - {

        "Create account".config(tags = setOf(task1)) {
            val accountId = UUID.randomUUID().toString()
            val writeResult = eventstoreService.createAccount(accountId, "MyAccount")

            writeResult.nextExpectedRevision.valueUnsigned shouldBe 0
        }

        "Create account with deposit".config(tags = setOf(task1)) {
            val accountId = UUID.randomUUID().toString()
            val writeResult1 = eventstoreService.createAccount(accountId, "MyAccount")

            writeResult1.nextExpectedRevision.valueUnsigned shouldBe 0

            val writeResult2 = eventstoreService.deposit(accountId, "Salary", 100)

            writeResult2.nextExpectedRevision.valueUnsigned shouldBe 1
        }

        "Create account with deposit and withdrawal events".config(tags = setOf(task1)) {
            val accountId = UUID.randomUUID().toString()
            val writeResult1 = eventstoreService.createAccount(accountId, "MyAccount")

            writeResult1.nextExpectedRevision.valueUnsigned shouldBe 0

            val writeResult2 = eventstoreService.deposit(accountId, "Salary", 100)

            writeResult2.nextExpectedRevision.valueUnsigned shouldBe 1

            val writeResult3 = eventstoreService.deposit(accountId, "Beer", 50)

            writeResult3.nextExpectedRevision.valueUnsigned shouldBe 2
        }
    }



    "Task 2 tests".config(tags = setOf(task2)) - {

        "Two accounts with same ID, can not exists".config(tags = setOf(task2)) {
            val accountId = UUID.randomUUID().toString()
            eventstoreService.createAccount(accountId, "Demo")

            assertThrows<ExecutionException> {
                eventstoreService.createAccount(accountId, "Demo")
            }
        }

        "Deposit event should not be first event on stream".config(tags = setOf(task2)) {
            val accountId = UUID.randomUUID().toString()
            assertThrows<ExecutionException> {
                eventstoreService.deposit(accountId, "Salary", 100)
            }
        }

        "Withdrawal event should not be first event on stream".config(tags = setOf(task2)) {
            val accountId = UUID.randomUUID().toString()
            assertThrows<ExecutionException> {
                eventstoreService.deposit(accountId, "Salary", 100)
            }
        }
    }

    "Task 3 tests".config(tags = setOf(task3)) - {

        "I can get account from EventstoreDB".config(tags = setOf(task3)) {
            val accountId = UUID.randomUUID().toString()
            val writeResult = eventstoreService.createAccount(accountId, "MyAccount")

            writeResult.nextExpectedRevision.valueUnsigned shouldBe 0

            val account = eventstoreService.getAccount(accountId)

            account shouldNotBe null
            account.id shouldBe accountId
            account.amount shouldBe 0L
        }



        "Account with deposit event, should reflect on account balance".config(tags = setOf(task3)) {
            val accountId = UUID.randomUUID().toString()
            eventstoreService.createAccount(accountId, "Demo")
            eventstoreService.deposit(accountId, "Salary", 100)
            val account = eventstoreService.getAccount(accountId).build()

            account shouldNotBe null
            account.id shouldBe accountId
            account.balance shouldBe 100L
        }



        "Account with withdrawal event, should reflect on account balance".config(tags = setOf(task3)) {
            val accountId = UUID.randomUUID().toString()
            eventstoreService.createAccount(accountId, "Demo")
            eventstoreService.withdrawal(accountId, "Shopping", 100)
            val account = eventstoreService.getAccount(accountId)

            account shouldNotBe null
            account.id shouldBe accountId
            account.amount shouldBe -100L
        }
    }

    "Task 4 tests".config(tags = setOf(task4)) - {

        "Read accounts from projection".config(tags = setOf(task4)) {
            val accountId = UUID.randomUUID().toString()
            eventstoreService.createAccount(accountId, "Demo")

            eventually(5000) {
                val account = accountProjection.accounts[accountId]

                account shouldNotBe null
                account?.id shouldBe accountId
            }
        }

        "Account should have updated amount from projection state".config(tags = setOf(task4)) {
            val accountId = UUID.randomUUID().toString()
            eventstoreService.createAccount(accountId, "Demo")
            eventstoreService.deposit(accountId, "Salary", 300)
            eventstoreService.withdrawal(accountId, "Shopping", 100)

            eventually(5000) {
                val account = accountProjection.accounts[accountId]

                account shouldNotBe null
                account?.id shouldBe accountId
                account?.balance shouldBe 200
            }
        }
    }


    val eventstoreEnv = mutableMapOf<String, String>().also {
        it["EVENTSTORE_CLUSTER_SIZE"] = "1"
        it["EVENTSTORE_RUN_PROJECTIONS"] = "All"
        it["EVENTSTORE_START_STANDARD_PROJECTIONS"] = "True"
        it["EVENTSTORE_DISCOVER_VIA_DNS"] = "True"
        it["EVENTSTORE_ENABLE_ATOM_PUB_OVER_HTTP"] = "True"
        it["EVENTSTORE_INSECURE"] = "True"
    }
    val eventstoreDb =
        GenericContainer("eventstore/eventstore:22.6.0-buster-slim").withAccessToHost(true).withExposedPorts(2113)
            .withEnv(eventstoreEnv.also { it.forEach { pair -> println("${pair.key}:${pair.value}") } })

    beforeSpec {
        eventstoreDb.start()

        var count = 0
        while (!eventstoreDb.isHealthy && count < 50) {
            withContext(Dispatchers.IO) {
                sleep(500L)
            }
            count++
        }
        println("EventstoreDB started on host ${eventstoreDb.host}:${eventstoreDb.getMappedPort(2113)}, after $count retries")

        val client = EventStoreDBClient.create(
            EventStoreDBClientSettings.builder().addHost(Endpoint(eventstoreDb.host, eventstoreDb.getMappedPort(2113)))
                .tls(false)
                .tlsVerifyCert(false).buildConnectionSettings()
        )
        eventstoreService = EventstoreRepo(client)

        client.subscribeToStream(
            BY_CATEGORY_STREAM_NAME,
            accountProjection,
            SubscribeToStreamOptions.get().resolveLinkTos().fromStart()
        )
    }

    afterSpec {
        println("After every test suite")
        eventstoreDb.stop()
    }

})