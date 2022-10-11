package eventstoredb.workshop.service

import eventstoredb.workshop.eventstore.EventstoreRepo
import eventstoredb.workshop.model.Account

class AccountService(val repo: EventstoreRepo) {

    fun get(id: String): Account = repo.getAccount(id).build()
    fun create(id: String, name: String) { repo.createAccount(id, name) }
    fun deposit(id: String, description: String, amount: Long) { repo.deposit(id, description, amount) }
    fun withdraw(id: String, description: String, amount: Long) { repo.withdrawal(id, description, amount) }

}