package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.Account
import com.example.data.model.Transaction
import com.example.data.model.UserProfile
import com.example.util.Formatters
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FinanceRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    // --- User Profile ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java)
                } else {
                    null
                }
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                throw e
            }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        val path = "users/$uid"
        val docRef = db.collection("users").document(uid)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "displayName" to profile.displayName,
            "email" to profile.email,
            "photoUri" to profile.photoUri,
            "bio" to profile.bio,
            "savingsTarget" to profile.savingsTarget,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    suspend fun updateProfilePhoto(photoUri: String) {
        val uid = requireUserId()
        val path = "users/$uid"
        val docRef = db.collection("users").document(uid)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "photoUri" to photoUri,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    suspend fun updateProfileBio(displayName: String, bio: String) {
        val uid = requireUserId()
        val path = "users/$uid"
        val docRef = db.collection("users").document(uid)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "displayName" to displayName,
            "bio" to bio,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    suspend fun updateSavingsTarget(target: Long) {
        val uid = requireUserId()
        val path = "users/$uid"
        val docRef = db.collection("users").document(uid)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "savingsTarget" to target,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    // --- Accounts ---

    fun observeAccounts(userId: String): Flow<List<Account>> {
        val path = "users/$userId/accounts"
        return db.collection("users").document(userId).collection("accounts")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Account::class.java)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun addAccount(account: Account) {
        val uid = requireUserId()
        val id = if (account.id.isNotBlank()) account.id else db.collection("users").document(uid).collection("accounts").document().id
        val path = "users/$uid/accounts/$id"
        val payload = hashMapOf<String, Any>(
            "id" to id,
            "userId" to uid,
            "name" to account.name,
            "type" to account.type,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            db.collection("users").document(uid).collection("accounts").document(id)
                .set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun deleteAccount(accountId: String) {
        val uid = requireUserId()
        val path = "users/$uid/accounts/$accountId"
        try {
            db.collection("users").document(uid).collection("accounts").document(accountId)
                .delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    // --- Transactions ---

    fun observeTransactions(userId: String): Flow<List<Transaction>> {
        val path = "users/$userId/transactions"
        return db.collection("users").document(userId).collection("transactions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Transaction::class.java)
            }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun addTransaction(transaction: Transaction) {
        val uid = requireUserId()
        val id = if (transaction.id.isNotBlank()) transaction.id else db.collection("users").document(uid).collection("transactions").document().id
        val path = "users/$uid/transactions/$id"
        val payload = hashMapOf<String, Any>(
            "id" to id,
            "userId" to uid,
            "type" to transaction.type,
            "description" to transaction.description,
            "amount" to transaction.amount,
            "account" to transaction.account,
            "category" to transaction.category,
            "date" to transaction.date,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            db.collection("users").document(uid).collection("transactions").document(id)
                .set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }

    suspend fun deleteTransaction(transactionId: String) {
        val uid = requireUserId()
        val path = "users/$uid/transactions/$transactionId"
        try {
            db.collection("users").document(uid).collection("transactions").document(transactionId)
                .delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    suspend fun resetAllData(userId: String) {
        val uid = requireUserId()
        val txCollection = db.collection("users").document(uid).collection("transactions")
        val snapshots = txCollection.get().await()
        val batch = db.batch()
        for (doc in snapshots.documents) {
            batch.delete(doc.reference)
        }
        val userRef = db.collection("users").document(uid)
        batch.update(userRef, "savingsTarget", 0L)
        batch.commit().await()
    }

    suspend fun seedInitialDataIfEmpty(userId: String, displayName: String = "", email: String = "") {
        val uid = requireUserId()
        val profileDoc = db.collection("users").document(uid).get().await()
        if (!profileDoc.exists()) {
            val userProfile = hashMapOf<String, Any>(
                "userId" to uid,
                "displayName" to displayName,
                "email" to email,
                "savingsTarget" to 0L,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).set(userProfile).await()
        }

        val accSnap = db.collection("users").document(uid).collection("accounts").get().await()
        if (accSnap.isEmpty) {
            val initialAccounts = listOf(
                Account(id = "acc_tunai", userId = uid, name = "Tunai", type = "liquid"),
                Account(id = "acc_bri", userId = uid, name = "BRI", type = "liquid"),
                Account(id = "acc_line", userId = uid, name = "Line Bank", type = "liquid"),
                Account(id = "acc_4", userId = uid, name = "Account 4", type = "non_liquid"),
                Account(id = "acc_5", userId = uid, name = "Account 5", type = "non_liquid")
            )
            val batch = db.batch()
            for (acc in initialAccounts) {
                val doc = db.collection("users").document(uid).collection("accounts").document(acc.id)
                batch.set(
                    doc,
                    hashMapOf<String, Any>(
                        "id" to acc.id,
                        "userId" to uid,
                        "name" to acc.name,
                        "type" to acc.type,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
        }

        // No transactions are seeded by default so new user starts with 0!
    }

    suspend fun populateSampleData(userId: String) {
        val uid = requireUserId()
        val dateStr = Formatters.currentIsoDate()
        val sampleTx = listOf(
            Transaction(id = "sample_1", userId = uid, type = "in", description = "Gaji Bulanan", amount = 5_000_000L, account = "BRI", date = dateStr),
            Transaction(id = "sample_2", userId = uid, type = "in", description = "Aset Drone DJI Mini", amount = 4_000_000L, account = "Account 4", date = dateStr),
            Transaction(id = "sample_3", userId = uid, type = "in", description = "Aset Tablet Xiaomi Pad", amount = 3_000_000L, account = "Account 4", date = dateStr),
            Transaction(id = "sample_4", userId = uid, type = "out", description = "Bayar Kos Bulanan", amount = 750_000L, account = "BRI", category = "Kebutuhan Kos", date = dateStr),
            Transaction(id = "sample_5", userId = uid, type = "out", description = "Cicilan Motor", amount = 608_000L, account = "BRI", category = "Pembayaran Lainnya", date = dateStr),
            Transaction(id = "sample_6", userId = uid, type = "out", description = "Makan Siang & Kopi", amount = 45_000L, account = "Tunai", category = "Makan", date = dateStr)
        )
        val batch = db.batch()
        for (tx in sampleTx) {
            val doc = db.collection("users").document(uid).collection("transactions").document(tx.id)
            batch.set(
                doc,
                hashMapOf<String, Any>(
                    "id" to tx.id,
                    "userId" to uid,
                    "type" to tx.type,
                    "description" to tx.description,
                    "amount" to tx.amount,
                    "account" to tx.account,
                    "category" to tx.category,
                    "date" to tx.date,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
    }
}
