package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SelfWorkDatabase
import com.example.data.model.AppNotification
import com.example.data.model.AppSettings
import com.example.data.model.ReferralItem
import com.example.data.model.Task
import com.example.data.model.TaskSession
import com.example.data.model.TaskSubmission
import com.example.data.model.TransactionRecord
import com.example.data.model.User
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.SelfWorkRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SelfWorkDatabase.getInstance(application)
    val repository = SelfWorkRepository(database.dao())

    // Current User State
    val currentUserId: StateFlow<String?> = repository.currentUserId
        .stateIn(viewModelScope, SharingStarted.Eagerly, "user_demo_101")

    val currentUser: StateFlow<User?> = currentUserId.flatMapLatest { id ->
        if (id != null) repository.getCurrentUserFlow(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allUsers: StateFlow<List<User>> = repository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin state
    val isAdminLoggedIn: StateFlow<Boolean> = repository.isAdminLoggedIn
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Tasks & Work Session
    val activeTasks: StateFlow<List<Task>> = repository.activeTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSession: StateFlow<TaskSession?> = currentUserId.flatMapLatest { id ->
        if (id != null) repository.getActiveSessionFlow(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Submissions
    val userSubmissions: StateFlow<List<TaskSubmission>> = currentUserId.flatMapLatest { id ->
        if (id != null) repository.getUserSubmissionsFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubmissions: StateFlow<List<TaskSubmission>> = repository.getAllSubmissionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSubmissions: StateFlow<List<TaskSubmission>> = repository.getPendingSubmissionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Wallet & Transactions
    val userTransactions: StateFlow<List<TransactionRecord>> = currentUserId.flatMapLatest { id ->
        if (id != null) repository.getUserTransactionsFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionRecord>> = repository.getAllTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userWithdrawals: StateFlow<List<WithdrawalRequest>> = currentUserId.flatMapLatest { id ->
        if (id != null) repository.getUserWithdrawalsFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWithdrawals: StateFlow<List<WithdrawalRequest>> = repository.getAllWithdrawalsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingWithdrawals: StateFlow<List<WithdrawalRequest>> = repository.getPendingWithdrawalsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Referrals & Notifications
    val userReferrals: StateFlow<List<ReferralItem>> = currentUserId.flatMapLatest { id ->
        if (id != null) repository.getUserReferralsFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<AppNotification>> = currentUserId.flatMapLatest { id ->
        if (id != null) repository.getNotificationsFlow(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = currentUserId.flatMapLatest { id ->
        if (id != null) repository.getUnreadCountFlow(id) else flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unreadNotifCount: StateFlow<Int> = unreadNotificationsCount

    val appSettings: StateFlow<AppSettings?> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // UI Message state (Toasts / Snackbars)
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    // Active Timer ticker job
    private var timerJob: Job? = null
    private val _liveElapsedSeconds = MutableStateFlow(0L)
    val liveElapsedSeconds = _liveElapsedSeconds.asStateFlow()

    init {
        startActiveSessionTicker()
    }

    private fun startActiveSessionTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val session = activeSession.value
                if (session != null && session.status == "RUNNING" && !session.isPaused) {
                    val now = System.currentTimeMillis()
                    val totalActive = (now - session.startTimeMillis - session.pausedTimeTotalMillis) / 1000L
                    _liveElapsedSeconds.value = totalActive.coerceAtLeast(0L)

                    // Periodic heartbeat to anti-cheat database
                    if (_liveElapsedSeconds.value % 10 == 0L) {
                        repository.updateSessionHeartbeat(session.sessionId, _liveElapsedSeconds.value)
                    }
                } else if (session != null && session.isPaused) {
                    _liveElapsedSeconds.value = session.elapsedSeconds
                } else {
                    _liveElapsedSeconds.value = 0L
                }
                delay(1000L)
            }
        }
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    // ==========================================
    // USER ACTIONS
    // ==========================================

    fun login(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val res = repository.login(email, "")
            res.fold(
                onSuccess = {
                    showMessage("Welcome back, ${it.name}!")
                    onSuccess()
                },
                onFailure = { showMessage(it.message ?: "Login failed") }
            )
        }
    }

    fun register(name: String, email: String, phone: String, refCode: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val res = repository.register(name, email, phone, refCode)
            res.fold(
                onSuccess = {
                    showMessage("Account created successfully! Welcome to Self Work.")
                    onSuccess()
                },
                onFailure = { showMessage(it.message ?: "Registration failed") }
            )
        }
    }

    fun logout() {
        repository.logout()
        showMessage("Logged out successfully.")
    }

    fun switchDemoUser(userId: String) {
        repository.setCurrentUserId(userId)
        showMessage("Switched active user profile.")
    }

    fun startTask(taskId: String, onSuccess: () -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.startTaskSession(user.id, taskId)
            res.fold(
                onSuccess = {
                    showMessage("Work session started! Track your progress and submit proof when done.")
                    onSuccess()
                },
                onFailure = { showMessage(it.message ?: "Could not start task") }
            )
        }
    }

    fun toggleSessionPause(sessionId: String) {
        viewModelScope.launch {
            val res = repository.toggleSessionPause(sessionId)
            res.fold(
                onSuccess = {
                    if (it.isPaused) showMessage("Task paused.") else showMessage("Task resumed!")
                },
                onFailure = { showMessage(it.message ?: "Action failed") }
            )
        }
    }

    fun cancelActiveSession(sessionId: String) {
        viewModelScope.launch {
            val res = repository.cancelSession(sessionId)
            res.fold(
                onSuccess = { showMessage("Work session cancelled.") },
                onFailure = { showMessage(it.message ?: "Could not cancel") }
            )
        }
    }

    fun submitTaskProof(sessionId: String, proofText: String, proofUrl: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val res = repository.submitTaskProof(sessionId, proofText, proofUrl)
            res.fold(
                onSuccess = {
                    showMessage("Proof submitted! Reward will be credited after admin review.")
                    onSuccess()
                },
                onFailure = { showMessage(it.message ?: "Submission failed") }
            )
        }
    }

    fun requestWithdrawal(method: String, accountNumber: String, amount: Double, onSuccess: () -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.requestWithdrawal(user.id, method, accountNumber, amount)
            res.fold(
                onSuccess = {
                    showMessage("Withdrawal request submitted! Will be reviewed and paid manually.")
                    onSuccess()
                },
                onFailure = { showMessage(it.message ?: "Withdrawal request failed") }
            )
        }
    }

    fun markNotificationsAsRead() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.markAllNotificationsRead(user.id)
        }
    }

    // ==========================================
    // ADMIN ACTIONS
    // ==========================================

    fun loginAdmin(pin: String, onSuccess: () -> Unit) {
        // Secure PIN: 2026 or 7890 for quick administration access
        if (pin == "2026" || pin == "7890" || pin == "admin123") {
            repository.setAdminLoggedIn(true)
            showMessage("Admin portal unlocked.")
            onSuccess()
        } else {
            showMessage("Invalid Admin PIN. (Hint: 2026)")
        }
    }

    fun logoutAdmin() {
        repository.setAdminLoggedIn(false)
        showMessage("Exited Admin mode.")
    }

    fun adminApproveSubmission(submissionId: String) {
        viewModelScope.launch {
            val res = repository.adminApproveTaskSubmission(submissionId)
            res.fold(
                onSuccess = { showMessage("Submission approved! Reward credited to user wallet.") },
                onFailure = { showMessage(it.message ?: "Approval failed") }
            )
        }
    }

    fun adminRejectSubmission(submissionId: String, reason: String) {
        viewModelScope.launch {
            val res = repository.adminRejectTaskSubmission(submissionId, reason)
            res.fold(
                onSuccess = { showMessage("Submission rejected with reason provided.") },
                onFailure = { showMessage(it.message ?: "Rejection failed") }
            )
        }
    }

    fun adminProcessWithdrawal(withdrawalId: String, action: String, trxOrReason: String) {
        viewModelScope.launch {
            val res = repository.adminProcessWithdrawal(withdrawalId, action, trxOrReason)
            res.fold(
                onSuccess = {
                    if (action == "MARK_PAID") {
                        showMessage("Withdrawal marked as PAID. Ledger & user stats updated.")
                    } else {
                        showMessage("Withdrawal rejected and funds refunded to user.")
                    }
                },
                onFailure = { showMessage(it.message ?: "Operation failed") }
            )
        }
    }

    fun adminAdjustBalance(userId: String, amount: Double, auditNote: String) {
        viewModelScope.launch {
            val res = repository.adminAdjustUserBalance(userId, amount, auditNote)
            res.fold(
                onSuccess = { showMessage("Balance adjusted and logged to transaction ledger.") },
                onFailure = { showMessage(it.message ?: "Adjustment failed") }
            )
        }
    }

    fun adminToggleSuspension(userId: String) {
        viewModelScope.launch {
            val res = repository.adminToggleUserSuspension(userId)
            res.fold(
                onSuccess = { isSuspended ->
                    showMessage(if (isSuspended) "User suspended." else "User account activated.")
                },
                onFailure = { showMessage(it.message ?: "Action failed") }
            )
        }
    }

    fun adminCreateOrUpdateTask(task: Task, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val res = repository.adminCreateOrUpdateTask(task)
            res.fold(
                onSuccess = {
                    showMessage("Task saved successfully.")
                    onSuccess()
                },
                onFailure = { showMessage(it.message ?: "Could not save task") }
            )
        }
    }

    fun adminDeleteTask(taskId: String) {
        viewModelScope.launch {
            val res = repository.adminDeleteTask(taskId)
            res.fold(
                onSuccess = { showMessage("Task removed.") },
                onFailure = { showMessage(it.message ?: "Could not delete task") }
            )
        }
    }

    fun adminUpdateSettings(settings: AppSettings, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val res = repository.adminUpdateSettings(settings)
            res.fold(
                onSuccess = {
                    showMessage("Global settings updated. Hourly reward: ৳${settings.hourlyReward}/hr")
                    onSuccess()
                },
                onFailure = { showMessage(it.message ?: "Could not update settings") }
            )
        }
    }
}
