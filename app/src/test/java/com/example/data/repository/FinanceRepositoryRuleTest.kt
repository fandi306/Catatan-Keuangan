package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Account
import com.example.data.model.Transaction
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun userProfile_createAndRead_succeeds() = runBlocking {
        val uid = signInTestUser("alice@test.com")
        val repo = FinanceRepository(firestore)

        val profile = UserProfile(userId = uid, displayName = "Alice", email = "alice@test.com", savingsTarget = 75_000_000L)
        repo.saveUserProfile(profile)

        val result = repo.observeUserProfile(uid).first()
        assertNotNull(result)
        assertEquals(75_000_000L, result?.savingsTarget)
    }

    @Test
    fun accountAndTransaction_crud_succeeds() = runBlocking {
        val uid = signInTestUser("bob@test.com")
        val repo = FinanceRepository(firestore)

        val account = Account(id = "acc_test", userId = uid, name = "Mandiri", type = "liquid")
        repo.addAccount(account)

        val accounts = repo.observeAccounts(uid).first()
        assertTrue(accounts.any { it.name == "Mandiri" })

        val tx = Transaction(
            id = "tx_test",
            userId = uid,
            type = "in",
            description = "Bonus",
            amount = 1_500_000L,
            account = "Mandiri",
            category = "",
            date = "2026-10-09"
        )
        repo.addTransaction(tx)

        val transactions = repo.observeTransactions(uid).first()
        assertTrue(transactions.any { it.description == "Bonus" && it.amount == 1_500_000L })
    }

    @Test
    fun seedInitialData_populatesAccounts_andSamplePopulatesTransactions() = runBlocking {
        val uid = signInTestUser("fandi@test.com")
        val repo = FinanceRepository(firestore)

        repo.seedInitialDataIfEmpty(uid, displayName = "Fandi Ahmad", email = "fandi@test.com")

        val accounts = repo.observeAccounts(uid).first()
        assertTrue(accounts.size >= 5)

        val transactions = repo.observeTransactions(uid).first()
        assertEquals(0, transactions.size)

        repo.populateSampleData(uid)
        val sampled = repo.observeTransactions(uid).first()
        assertTrue(sampled.size >= 5)
    }
}
