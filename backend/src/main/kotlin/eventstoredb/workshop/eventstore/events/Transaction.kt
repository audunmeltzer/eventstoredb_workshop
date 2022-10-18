package eventstoredb.workshop.eventstore.events

interface Transaction {
    val description: String
    val amount: Long
}