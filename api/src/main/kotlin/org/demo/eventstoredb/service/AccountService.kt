package org.demo.eventstoredb.service

import org.demo.eventstoredb.eventstore.EventstoreRepo
import org.demo.eventstoredb.api.model.Account
import org.springframework.stereotype.Service

@Service
class AccountService(val repo: EventstoreRepo) {

    fun get(id: String): Account = repo.getAccount(id).build()
    fun create(id: String, name: String) { repo.createAccount(id, name) }
    fun deposit(id: String, description: String, amount: Long) { repo.deposit(id, description, amount) }
    fun withdraw(id: String, description: String, amount: Long) { repo.withdrawal(id, description, amount) }

}