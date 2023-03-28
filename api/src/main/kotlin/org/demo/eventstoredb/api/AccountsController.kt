package org.demo.eventstoredb.api

import org.demo.eventstoredb.service.AccountService
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.servers.Server
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.demo.eventstoredb.api.model.Account
import org.demo.eventstoredb.eventstore.AccountProjection
import org.springdoc.api.annotations.ParameterObject
import org.springframework.web.bind.annotation.*


const val TAG = "AccountsController"

@RestController
@RequestMapping("/api/accounts")
@Produces(MediaType.APPLICATION_JSON)
@OpenAPIDefinition(
    info = Info(
        title = "AccountsController - Eventstore Accounts demo API",
        version = "v1"
    ),
    servers = [Server(url = "http://localhost:8080", description = "Local test environment")]
)
class AccountsController(private val accountService: AccountService, private val accountProjection: AccountProjection) {

    @GetMapping("/")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of all Accounts", content = [Content(array = ArraySchema(schema = Schema(implementation = Account::class)))]))
    @Operation(tags = [TAG], description = "Returns all accounts from in memory projection")
    fun getAccounts(): List<Account> {
        return accountProjection.accounts.values.toList()
    }

    @GetMapping("/{id}")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Get Account", content = [Content(schema = Schema(implementation = Account::class))]))
    @Operation(tags = [TAG], description = "Returns account from EventstoreDB")
    fun getAccount(@PathVariable id: String): Account {
        return accountService.get(id)
    }

    @PostMapping("/")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Create Account"))
    @Operation(tags = [TAG], description = "Creates account and writes to EventstoreDB")
    fun createAccount(@ParameterObject account: CreateAccountRequest) {
        return accountService.create(account.id, account.name)
    }

    @PostMapping("/{id}/deposit")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Add money to account"))
    @Operation(tags = [TAG], description = "Writes transaction to EventstoreDB")
    fun deposit(@PathVariable id: String, @ParameterObject depositRequest: DepositRequest) {
        accountService.deposit(id, depositRequest.description, depositRequest.amount)
    }

    @PostMapping("/{id}/withdraw")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Withdraw money from account"))
    @Operation(tags = [TAG], description = "Writes transaction to EventstoreDB")
    fun withdraw(@PathVariable id: String, @ParameterObject withdrawalRequest: WithdrawalRequest) {
        accountService.withdraw(id, withdrawalRequest.description, withdrawalRequest.amount)
    }


    data class CreateAccountRequest(val id: String, val name: String)
    data class DepositRequest(val amount: Long, val description: String)
    data class WithdrawalRequest(val amount: Long, val description: String)
}