package eventstoredb.workshop.model

import kotlinx.serialization.Serializable

@Serializable
data class Account(val id: String, val amount: Long, val transactions: List<Transaction>)
@Serializable
data class Transaction(val description: String, val amount: Long)

