package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Account
import com.example.data.model.Transaction
import com.example.data.model.UserProfile
import com.example.data.repository.FinanceRepository
import com.example.util.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FinanceViewModel(
    val userId: String,
    private val repository: FinanceRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val accounts: StateFlow<List<Account>> = repository.observeAccounts(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.observeTransactions(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMonth = MutableStateFlow(Formatters.currentYearMonth())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _filterType = MutableStateFlow("all") // "all", "out", "in"
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _lastDeletedTransaction = MutableStateFlow<Transaction?>(null)
    val lastDeletedTransaction: StateFlow<Transaction?> = _lastDeletedTransaction.asStateFlow()

    private val _uiNotice = MutableStateFlow<String?>(null)
    val uiNotice: StateFlow<String?> = _uiNotice.asStateFlow()

    // Calculated balances per account name
    val accountBalances: StateFlow<Map<String, Long>> = combine(accounts, transactions) { accList, txList ->
        val balances = mutableMapOf<String, Long>()
        for (acc in accList) {
            balances[acc.name] = 0L
        }
        for (tx in txList) {
            val current = balances[tx.account] ?: 0L
            balances[tx.account] = if (tx.type == "in") current + tx.amount else current - tx.amount
        }
        balances
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val liquidAsset: StateFlow<Long> = combine(accounts, accountBalances) { accList, balances ->
        accList.filter { it.isLiquid }.sumOf { balances[it.name] ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val nonLiquidAsset: StateFlow<Long> = combine(accounts, accountBalances) { accList, balances ->
        accList.filter { !it.isLiquid }.sumOf { balances[it.name] ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalAsset: StateFlow<Long> = combine(liquidAsset, nonLiquidAsset) { liquid, nonLiquid ->
        liquid + nonLiquid
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val savingsTarget: StateFlow<Long> = userProfile.map { profile ->
        profile?.savingsTarget ?: 0L
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val targetProgressPct: StateFlow<Float> = combine(totalAsset, savingsTarget) { total, target ->
        if (target <= 0L) 0f else ((total.toFloat() / target.toFloat()) * 100f).coerceIn(0f, 100f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Monthly filtered transactions
    val monthlyTransactions: StateFlow<List<Transaction>> = combine(transactions, selectedMonth) { txList, month ->
        txList.filter { it.date.startsWith(month) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyIncome: StateFlow<Long> = monthlyTransactions.map { list ->
        list.filter { it.type == "in" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val monthlyExpense: StateFlow<Long> = monthlyTransactions.map { list ->
        list.filter { it.type == "out" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val monthlyNet: StateFlow<Long> = combine(monthlyIncome, monthlyExpense) { inc, exp ->
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val categoryBreakdown: StateFlow<List<Pair<String, Long>>> = monthlyTransactions.map { list ->
        val outTxs = list.filter { it.type == "out" }
        val map = mutableMapOf<String, Long>()
        for (tx in outTxs) {
            val cat = if (tx.category.isNotBlank()) tx.category else "Lainnya"
            map[cat] = (map[cat] ?: 0L) + tx.amount
        }
        map.toList().sortedByDescending { it.second }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val displayedTransactions: StateFlow<List<Transaction>> = combine(
        monthlyTransactions,
        filterType,
        searchQuery
    ) { list, filter, query ->
        val q = query.trim().lowercase(Locale.ROOT)
        list.filter { tx ->
            val matchFilter = when (filter) {
                "in" -> tx.type == "in"
                "out" -> tx.type == "out"
                else -> true
            }
            val matchQuery = if (q.isBlank()) true else {
                tx.description.lowercase(Locale.ROOT).contains(q) ||
                        tx.category.lowercase(Locale.ROOT).contains(q) ||
                        tx.account.lowercase(Locale.ROOT).contains(q)
            }
            matchFilter && matchQuery
        }.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt?.seconds ?: 0L })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty(userId)
        }
    }

    fun changeMonth(delta: Int) {
        val current = _selectedMonth.value
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            val cal = Calendar.getInstance()
            val parsed = sdf.parse(current)
            if (parsed != null) {
                cal.time = parsed
                cal.add(Calendar.MONTH, delta)
                _selectedMonth.value = sdf.format(cal.time)
            }
        } catch (_: Exception) {}
    }

    fun setFilter(filter: String) {
        _filterType.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearUiNotice() {
        _uiNotice.value = null
    }

    fun addTransaction(
        type: String,
        description: String,
        amount: Long,
        account: String,
        category: String,
        date: String
    ) {
        viewModelScope.launch {
            val tx = Transaction(
                id = "",
                userId = userId,
                type = type,
                description = description.trim(),
                amount = amount,
                account = account.trim(),
                category = if (type == "out") category.trim() else "",
                date = date
            )
            repository.addTransaction(tx)
            // Adjust month if transaction date is different
            if (date.length >= 7) {
                _selectedMonth.value = date.substring(0, 7)
            }
            _uiNotice.value = "Transaksi berhasil dicatat"
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            _lastDeletedTransaction.value = transaction
            repository.deleteTransaction(transaction.id)
            _uiNotice.value = "Transaksi dihapus"
        }
    }

    fun undoDelete() {
        val tx = _lastDeletedTransaction.value ?: return
        viewModelScope.launch {
            repository.addTransaction(tx)
            _lastDeletedTransaction.value = null
            _uiNotice.value = "Transaksi berhasil dipulihkan"
        }
    }

    fun updateSavingsTarget(target: Long) {
        viewModelScope.launch {
            repository.updateSavingsTarget(target)
            _uiNotice.value = "Target tabungan diperbarui"
        }
    }

    fun updateProfilePhoto(photoUri: String) {
        viewModelScope.launch {
            repository.updateProfilePhoto(photoUri)
            _uiNotice.value = "Foto profil berhasil diperbarui"
        }
    }

    fun removeProfilePhoto() {
        viewModelScope.launch {
            repository.updateProfilePhoto("")
            _uiNotice.value = "Foto profil berhasil dihapus"
        }
    }

    fun updateProfileBio(name: String, bio: String) {
        viewModelScope.launch {
            repository.updateProfileBio(name.trim(), bio.trim())
            _uiNotice.value = "Profil & bio berhasil disimpan"
        }
    }

    fun addAccount(name: String, type: String) {
        viewModelScope.launch {
            val acc = Account(
                id = "",
                userId = userId,
                name = name.trim(),
                type = type
            )
            repository.addAccount(acc)
            _uiNotice.value = "Akun '${name.trim()}' berhasil ditambahkan"
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
            _uiNotice.value = "Akun berhasil dihapus"
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData(userId)
            _uiNotice.value = "Semua transaksi telah direset"
        }
    }

    fun restoreSeedData() {
        viewModelScope.launch {
            repository.resetAllData(userId)
            repository.populateSampleData(userId)
            _uiNotice.value = "Data contoh berhasil dimuat"
        }
    }

    class Factory(
        private val userId: String,
        private val repository: FinanceRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FinanceViewModel(userId, repository) as T
        }
    }
}
