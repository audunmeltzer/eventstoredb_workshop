package org.demo.eventstoredb.api.model

data class Account(val id: String, val balance: Long, val transactions: List<Transaction>)
