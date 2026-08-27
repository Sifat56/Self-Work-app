package com.example.data.repository

import com.example.data.local.SelfWorkDao
import com.example.data.model.AppNotification
import com.example.data.model.AppSettings
import com.example.data.model.ReferralItem
import com.example.data.model.Task
import com.example.data.model.TaskSession
import com.example.data.model.TaskSubmission
import com.example.data.model.TransactionRecord
import com.example.data.model.User
import com.example.data.model.WithdrawalRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class SelfWorkRepository(private val dao: SelfWorkDao) {

    // Current Session State
    private val _currentUserId = MutableStateFlow<String?>("user_demo_101")
    val currentUserId: Flow<String?> = _currentUserId.asStateFlow()

    // Admin Session State
    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn: Flow<Boolean> = _isAdminLoggedIn.asStateFlow()

    // User Flows
    fun getCurrentUserFlow(userId: String): Flow<User?> = dao.getUserFlow(userId)
    fun getAllUsersFlow(): Flow<List<User>> = dao.getAllUsersFlow()

    // Tasks Flows
    val activeTasks: Flow<List<Task>> = dao.getActiveTasksFlow()
    val allTasks: Flow<List<Task>> = dao.getAllTasksFlow()
    fun getTaskFlow(taskId: String): Flow<Task?> = dao.getTaskFlow(taskId)

    // Task Sessions
    fun getActiveSessionFlow(userId: String): Flow<TaskSession?> = dao.getActiveSessionFlow(userId)

    // Submissions
    fun getUserSubmissionsFlow(userId: String): Flow<List<TaskSubmission>> = dao.getUserSubmissionsFlow(userId)
    fun getAllSubmissionsFlow(): Flow<List<TaskSubmission>> = dao.getAllSubmissionsFlow()
    fun getPendingSubmissionsFlow(): Flow<List<TaskSubmission>> = dao.getPendingSubmissionsFlow()

    // Transactions
    fun getUserTransactionsFlow(userId: String): Flow<List<TransactionRecord>> = dao.getUserTransactionsFlow(userId)
    fun getAllTransactionsFlow(): Flow<List<TransactionRecord>> = dao.getAllTransactionsFlow()

    // Withdrawals
    fun getUserWithdrawalsFlow(userId: String): Flow<List<WithdrawalRequest>> = dao.getUserWithdrawalsFlow(userId)
    fun getAllWithdrawalsFlow(): Flow<List<WithdrawalRequest>> = dao.getAllWithdrawalsFlow()
    fun getPendingWithdrawalsFlow(): Flow<List<WithdrawalRequest>> = dao.getPendingWithdrawalsFlow()

    // Referrals & Notifications & Settings
    fun getUserReferralsFlow(userId: String): Flow<List<ReferralItem>> = dao.getUserReferralsFlow(userId)
    fun getNotificationsFlow(userId: String): Flow<List<AppNotification>> = dao.getNotificationsFlow(userId)
    fun getUnreadCountFlow(userId: String): Flow<Int> = dao.getUnreadNotificationCountFlow(userId)
    val appSettings: Flow<AppSettings?> = dao.getAppSettingsFlow()

    // ==========================================
    // AUTHENTICATION
    // ==========================================

    suspend fun login(email: String, passwordRaw: String): Result<User> = withContext(Dispatchers.IO) {
        val user = dao.getUserByEmail(email.trim().lowercase())
        if (user != null) {
            if (user.isSuspended) {
                return@withContext Result.failure(Exception("This account is suspended. Please contact Telegram support."))
            }
            _currentUserId.value = user.id
            Result.success(user)
        } else {
            Result.failure(Exception("No account found with this email. Please register first."))
        }
    }

    suspend fun register(
        name: String,
        email: String,
        phone: String,
        referralCodeInput: String?
    ): Result<User> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val existing = dao.getUserByEmail(cleanEmail)
        if (existing != null) {
            return@withContext Result.failure(Exception("An account with this email already exists."))
        }

        // Generate unique referral code (e.g. SW + 4 random digits)
        val generatedRefCode = "SW" + (1000..9999).random()
        var referrer: User? = null

        if (!referralCodeInput.isNullOrBlank()) {
            val code = referralCodeInput.trim().uppercase()
            referrer = dao.getUserByReferralCode(code)
            if (referrer != null && referrer.email == cleanEmail) {
                return@withContext Result.failure(Exception("You cannot use your own referral code."))
            }
        }

        val newUser = User(
            id = "user_" + UUID.randomUUID().toString().take(8),
            name = name.trim(),
            email = cleanEmail,
            phone = phone.trim(),
            referralCode = generatedRefCode,
            referredBy = referrer?.referralCode,
            balance = 0.0,
            totalEarnings = 0.0,
            totalWithdrawn = 0.0,
            pendingWithdrawal = 0.0,
            taskEarnings = 0.0,
            referralEarnings = 0.0,
            todayEarnings = 0.0,
            completedTasksCount = 0,
            role = "USER"
        )

        dao.insertOrUpdateUser(newUser)

        // If referred by someone, record referral item
        if (referrer != null) {
            val refItem = ReferralItem(
                id = "ref_" + UUID.randomUUID().toString().take(8),
                referrerId = referrer.id,
                referredUserId = newUser.id,
                referredUserName = newUser.name,
                referredUserEmail = newUser.email,
                rewardAmount = dao.getAppSettings()?.referralReward ?: 10.0,
                status = "ACTIVE"
            )
            dao.insertReferral(refItem)
        }

        // Welcome notification
        dao.insertNotification(
            AppNotification(
                id = "notif_" + UUID.randomUUID().toString().take(8),
                userId = newUser.id,
                title = "Welcome to Self Work!",
                message = "Your account is active. Complete verified tasks and earn legitimate rewards at ৳15/hr.",
                type = "SYSTEM"
            )
        )

        _currentUserId.value = newUser.id
        Result.success(newUser)
    }

    fun logout() {
        _currentUserId.value = null
    }

    fun setCurrentUserId(userId: String) {
        _currentUserId.value = userId
    }

    // Admin Auth
    fun setAdminLoggedIn(loggedIn: Boolean) {
        _isAdminLoggedIn.value = loggedIn
    }

    // ==========================================
    // WORK & TASK SESSIONS (ANTI-CHEAT)
    // ==========================================

    suspend fun startTaskSession(userId: String, taskId: String): Result<TaskSession> = withContext(Dispatchers.IO) {
        val user = dao.getUserById(userId) ?: return@withContext Result.failure(Exception("User not found"))
        if (user.isSuspended) return@withContext Result.failure(Exception("Account is suspended"))

        val activeSession = dao.getActiveSession(userId)
        if (activeSession != null) {
            return@withContext Result.failure(Exception("You already have an active work session for '${activeSession.taskTitle}'. Please finish or submit it first."))
        }

        val existingSubmissions = dao.countSubmissionsForTask(userId, taskId)
        val task = dao.getTaskById(taskId) ?: return@withContext Result.failure(Exception("Task not found"))

        if (existingSubmissions >= task.maxSubmissionsPerUser) {
            return@withContext Result.failure(Exception("You have already submitted or have a pending review for this task."))
        }

        val session = TaskSession(
            sessionId = "session_" + UUID.randomUUID().toString().take(8),
            taskId = taskId,
            taskTitle = task.title,
            userId = userId,
            startTimeMillis = System.currentTimeMillis(),
            lastHeartbeatMillis = System.currentTimeMillis(),
            pausedTimeTotalMillis = 0L,
            isPaused = false,
            elapsedSeconds = 0L,
            status = "RUNNING",
            antiCheatHash = UUID.randomUUID().toString()
        )

        dao.insertSession(session)

        // Update active task in user
        dao.insertOrUpdateUser(user.copy(activeTaskId = taskId))
        Result.success(session)
    }

    suspend fun updateSessionHeartbeat(sessionId: String, elapsedSeconds: Long): Unit = withContext(Dispatchers.IO) {
        val session = dao.getSessionById(sessionId) ?: return@withContext
        if (session.status == "RUNNING") {
            dao.updateSession(
                session.copy(
                    elapsedSeconds = elapsedSeconds,
                    lastHeartbeatMillis = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun toggleSessionPause(sessionId: String): Result<TaskSession> = withContext(Dispatchers.IO) {
        val session = dao.getSessionById(sessionId) ?: return@withContext Result.failure(Exception("Session not found"))
        val now = System.currentTimeMillis()

        val updated = if (session.isPaused) {
            // Resume
            val pauseDuration = if (session.pauseTimestampMillis > 0) now - session.pauseTimestampMillis else 0L
            session.copy(
                isPaused = false,
                status = "RUNNING",
                pausedTimeTotalMillis = session.pausedTimeTotalMillis + pauseDuration,
                pauseTimestampMillis = 0L,
                lastHeartbeatMillis = now
            )
        } else {
            // Pause
            session.copy(
                isPaused = true,
                status = "PAUSED",
                pauseTimestampMillis = now
            )
        }

        dao.updateSession(updated)
        Result.success(updated)
    }

    suspend fun cancelSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val session = dao.getSessionById(sessionId) ?: return@withContext Result.failure(Exception("Session not found"))
        dao.updateSession(session.copy(status = "CANCELLED"))
        val user = dao.getUserById(session.userId)
        if (user != null) {
            dao.insertOrUpdateUser(user.copy(activeTaskId = null))
        }
        Result.success(Unit)
    }

    suspend fun submitTaskProof(
        sessionId: String,
        proofText: String,
        proofUrl: String
    ): Result<TaskSubmission> = withContext(Dispatchers.IO) {
        val session = dao.getSessionById(sessionId) ?: return@withContext Result.failure(Exception("Session not found"))
        if (session.status != "RUNNING" && session.status != "PAUSED") {
            return@withContext Result.failure(Exception("This work session is no longer active."))
        }

        if (proofText.trim().length < 15) {
            return@withContext Result.failure(Exception("Please provide a detailed work summary (at least 15 characters)."))
        }

        val user = dao.getUserById(session.userId) ?: return@withContext Result.failure(Exception("User not found"))
        val task = dao.getTaskById(session.taskId) ?: return@withContext Result.failure(Exception("Task not found"))
        val settings = dao.getAppSettings() ?: AppSettings()

        // Server-side work duration calculation:
        // Use elapsed seconds or minimum required task duration
        val durationMinutes = ((session.elapsedSeconds / 60).toInt()).coerceAtLeast(1)
        val hourlyRate = task.hourlyRate.coerceAtLeast(settings.hourlyReward)
        val estimatedHours = (task.estimatedDurationMinutes / 60.0)
        val calculatedReward = (estimatedHours * hourlyRate) // e.g. 1 hr = ৳15, 2 hr = ৳30

        val submission = TaskSubmission(
            id = "sub_" + UUID.randomUUID().toString().take(8),
            taskId = task.id,
            taskTitle = task.title,
            userId = user.id,
            userName = user.name,
            userEmail = user.email,
            proofText = proofText.trim(),
            proofUrl = proofUrl.trim(),
            workDurationMinutes = durationMinutes,
            hourlyRateAtSubmission = hourlyRate,
            calculatedReward = calculatedReward,
            status = "PENDING",
            submittedAtMillis = System.currentTimeMillis()
        )

        dao.insertSubmission(submission)

        // Mark session completed
        dao.updateSession(session.copy(status = "SUBMITTED", isCompleted = true))

        // Clear user's active task
        dao.insertOrUpdateUser(user.copy(activeTaskId = null))

        // Create user notification
        dao.insertNotification(
            AppNotification(
                id = "notif_" + UUID.randomUUID().toString().take(8),
                userId = user.id,
                title = "Task Submitted for Verification",
                message = "Your submission for '${task.title}' is under review. Reward (৳${"%.2f".format(calculatedReward)}) will be added once approved by admin.",
                type = "TASK"
            )
        )

        Result.success(submission)
    }

    // ==========================================
    // WALLET & WITHDRAWALS
    // ==========================================

    suspend fun requestWithdrawal(
        userId: String,
        method: String, // "bKash" or "Nagad"
        accountNumber: String,
        amount: Double
    ): Result<WithdrawalRequest> = withContext(Dispatchers.IO) {
        val user = dao.getUserById(userId) ?: return@withContext Result.failure(Exception("User not found"))
        val settings = dao.getAppSettings() ?: AppSettings()

        if (user.isSuspended) {
            return@withContext Result.failure(Exception("Suspended accounts cannot withdraw funds."))
        }

        val cleanNumber = accountNumber.trim()
        if (cleanNumber.length < 11 || !cleanNumber.startsWith("01")) {
            return@withContext Result.failure(Exception("Please enter a valid 11-digit Bangladeshi mobile number (e.g. 017xxxxxxxx)."))
        }

        if (amount < settings.minimumWithdrawal) {
            return@withContext Result.failure(Exception("Minimum withdrawal amount is ৳${settings.minimumWithdrawal}."))
        }

        if (amount > user.balance) {
            return@withContext Result.failure(Exception("Insufficient available balance. You have ৳${"%.2f".format(user.balance)}."))
        }

        val fee = if (settings.withdrawalFeePercent > 0) (amount * settings.withdrawalFeePercent / 100.0) else 0.0
        val netAmount = amount - fee

        val withdrawal = WithdrawalRequest(
            id = "wdr_" + UUID.randomUUID().toString().take(8),
            userId = user.id,
            userName = user.name,
            userEmail = user.email,
            method = method,
            accountNumber = cleanNumber,
            amount = amount,
            fee = fee,
            netAmount = netAmount,
            status = "PENDING",
            requestedAtMillis = System.currentTimeMillis()
        )

        // Lock funds into pending
        val updatedUser = user.copy(
            balance = user.balance - amount,
            pendingWithdrawal = user.pendingWithdrawal + amount
        )
        dao.insertOrUpdateUser(updatedUser)
        dao.insertWithdrawal(withdrawal)

        // Ledger Transaction Record
        dao.insertTransaction(
            TransactionRecord(
                id = "tx_" + UUID.randomUUID().toString().take(8),
                userId = user.id,
                amount = -amount,
                type = "WITHDRAWAL_REQUEST",
                title = "$method Withdrawal Request",
                description = "Requested ৳${"%.2f".format(amount)} to $cleanNumber (Pending Review)",
                status = "PENDING",
                referenceId = withdrawal.id
            )
        )

        // Notification
        dao.insertNotification(
            AppNotification(
                id = "notif_" + UUID.randomUUID().toString().take(8),
                userId = user.id,
                title = "Withdrawal Request Received",
                message = "Your request of ৳${"%.2f".format(amount)} via $method is submitted for manual verification.",
                type = "WALLET"
            )
        )

        Result.success(withdrawal)
    }

    // ==========================================
    // ADMIN ACTIONS (AUDITABLE & SECURE)
    // ==========================================

    suspend fun adminApproveTaskSubmission(
        submissionId: String,
        adminName: String = "Admin"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val submission = dao.getSubmissionById(submissionId)
            ?: return@withContext Result.failure(Exception("Submission not found"))

        if (submission.status != "PENDING") {
            return@withContext Result.failure(Exception("Submission is already ${submission.status}"))
        }

        val user = dao.getUserById(submission.userId)
            ?: return@withContext Result.failure(Exception("User not found"))

        val reward = submission.calculatedReward

        // Update submission
        val updatedSubmission = submission.copy(
            status = "APPROVED",
            reviewedAtMillis = System.currentTimeMillis(),
            reviewedByAdmin = adminName
        )
        dao.updateSubmission(updatedSubmission)

        // Credit user's wallet with decimal precision
        val updatedUser = user.copy(
            balance = user.balance + reward,
            totalEarnings = user.totalEarnings + reward,
            taskEarnings = user.taskEarnings + reward,
            todayEarnings = user.todayEarnings + reward,
            completedTasksCount = user.completedTasksCount + 1
        )
        dao.insertOrUpdateUser(updatedUser)

        // Ledger transaction
        dao.insertTransaction(
            TransactionRecord(
                id = "tx_" + UUID.randomUUID().toString().take(8),
                userId = user.id,
                amount = reward,
                type = "TASK_REWARD",
                title = "Task Approved: ${submission.taskTitle}",
                description = "${submission.workDurationMinutes} mins legitimate work approved (+৳${"%.2f".format(reward)})",
                status = "SUCCESS",
                referenceId = submission.id
            )
        )

        // Notification
        dao.insertNotification(
            AppNotification(
                id = "notif_" + UUID.randomUUID().toString().take(8),
                userId = user.id,
                title = "Task Approved! (+৳${"%.2f".format(reward)})",
                message = "Great work! Your submission for '${submission.taskTitle}' was verified. ৳${"%.2f".format(reward)} credited.",
                type = "TASK"
            )
        )

        // Check if referrer should be rewarded for first task completion
        if (user.referredBy != null && user.completedTasksCount == 0) {
            val referrer = dao.getUserByReferralCode(user.referredBy)
            if (referrer != null) {
                val refBonus = dao.getAppSettings()?.referralReward ?: 10.0
                val updatedReferrer = referrer.copy(
                    balance = referrer.balance + refBonus,
                    totalEarnings = referrer.totalEarnings + refBonus,
                    referralEarnings = referrer.referralEarnings + refBonus
                )
                dao.insertOrUpdateUser(updatedReferrer)

                dao.insertTransaction(
                    TransactionRecord(
                        id = "tx_" + UUID.randomUUID().toString().take(8),
                        userId = referrer.id,
                        amount = refBonus,
                        type = "REFERRAL_REWARD",
                        title = "Referral Bonus (+৳${"%.2f".format(refBonus)})",
                        description = "Referred user ${user.name} completed first verified task.",
                        status = "SUCCESS",
                        referenceId = user.id
                    )
                )

                dao.insertNotification(
                    AppNotification(
                        id = "notif_" + UUID.randomUUID().toString().take(8),
                        userId = referrer.id,
                        title = "Referral Bonus Received! (+৳${"%.2f".format(refBonus)})",
                        message = "Your friend ${user.name} completed their first work! Bonus added to wallet.",
                        type = "REFERRAL"
                    )
                )
            }
        }

        Result.success(Unit)
    }

    suspend fun adminRejectTaskSubmission(
        submissionId: String,
        reason: String,
        adminName: String = "Admin"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val submission = dao.getSubmissionById(submissionId)
            ?: return@withContext Result.failure(Exception("Submission not found"))

        if (submission.status != "PENDING") {
            return@withContext Result.failure(Exception("Submission is already ${submission.status}"))
        }

        val updatedSubmission = submission.copy(
            status = "REJECTED",
            rejectionReason = reason.trim().ifBlank { "Incomplete or unverified proof provided." },
            reviewedAtMillis = System.currentTimeMillis(),
            reviewedByAdmin = adminName
        )
        dao.updateSubmission(updatedSubmission)

        // Notification
        dao.insertNotification(
            AppNotification(
                id = "notif_" + UUID.randomUUID().toString().take(8),
                userId = submission.userId,
                title = "Task Submission Rejected",
                message = "Submission for '${submission.taskTitle}' rejected. Reason: ${updatedSubmission.rejectionReason}",
                type = "TASK"
            )
        )

        Result.success(Unit)
    }

    suspend fun adminProcessWithdrawal(
        withdrawalId: String,
        action: String, // "MARK_PAID" or "REJECT"
        trxIdOrReason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val withdrawal = dao.getWithdrawalById(withdrawalId)
            ?: return@withContext Result.failure(Exception("Withdrawal not found"))

        if (withdrawal.status != "PENDING") {
            return@withContext Result.failure(Exception("Withdrawal is already ${withdrawal.status}"))
        }

        val user = dao.getUserById(withdrawal.userId)
            ?: return@withContext Result.failure(Exception("User not found"))

        if (action == "MARK_PAID") {
            val updatedWithdrawal = withdrawal.copy(
                status = "PAID",
                transactionReference = trxIdOrReason.trim().ifBlank { "TrxID_${UUID.randomUUID().toString().take(6).uppercase()}" },
                reviewedAtMillis = System.currentTimeMillis()
            )
            dao.updateWithdrawal(updatedWithdrawal)

            val updatedUser = user.copy(
                pendingWithdrawal = (user.pendingWithdrawal - withdrawal.amount).coerceAtLeast(0.0),
                totalWithdrawn = user.totalWithdrawn + withdrawal.netAmount
            )
            dao.insertOrUpdateUser(updatedUser)

            dao.insertTransaction(
                TransactionRecord(
                    id = "tx_" + UUID.randomUUID().toString().take(8),
                    userId = user.id,
                    amount = -withdrawal.amount,
                    type = "WITHDRAWAL_PAID",
                    title = "${withdrawal.method} Payout Paid",
                    description = "৳${"%.2f".format(withdrawal.netAmount)} sent to ${withdrawal.accountNumber} (TrxID: ${updatedWithdrawal.transactionReference})",
                    status = "SUCCESS",
                    referenceId = withdrawal.id
                )
            )

            dao.insertNotification(
                AppNotification(
                    id = "notif_" + UUID.randomUUID().toString().take(8),
                    userId = user.id,
                    title = "Withdrawal Paid Successfully",
                    message = "৳${"%.2f".format(withdrawal.netAmount)} has been sent via ${withdrawal.method} to ${withdrawal.accountNumber}. TrxID: ${updatedWithdrawal.transactionReference}",
                    type = "WALLET"
                )
            )
        } else {
            // Reject and refund
            val reason = trxIdOrReason.trim().ifBlank { "Invalid mobile account details or security policy." }
            val updatedWithdrawal = withdrawal.copy(
                status = "REJECTED",
                rejectionReason = reason,
                reviewedAtMillis = System.currentTimeMillis()
            )
            dao.updateWithdrawal(updatedWithdrawal)

            val updatedUser = user.copy(
                balance = user.balance + withdrawal.amount,
                pendingWithdrawal = (user.pendingWithdrawal - withdrawal.amount).coerceAtLeast(0.0)
            )
            dao.insertOrUpdateUser(updatedUser)

            dao.insertTransaction(
                TransactionRecord(
                    id = "tx_" + UUID.randomUUID().toString().take(8),
                    userId = user.id,
                    amount = withdrawal.amount,
                    type = "WITHDRAWAL_REJECTED",
                    title = "Withdrawal Refunded",
                    description = "৳${"%.2f".format(withdrawal.amount)} refunded to balance. Reason: $reason",
                    status = "REVERSED",
                    referenceId = withdrawal.id
                )
            )

            dao.insertNotification(
                AppNotification(
                    id = "notif_" + UUID.randomUUID().toString().take(8),
                    userId = user.id,
                    title = "Withdrawal Request Rejected (Refunded)",
                    message = "Your request of ৳${"%.2f".format(withdrawal.amount)} was rejected and refunded. Reason: $reason",
                    type = "WALLET"
                )
            )
        }

        Result.success(Unit)
    }

    suspend fun adminAdjustUserBalance(
        userId: String,
        amount: Double,
        auditNote: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val user = dao.getUserById(userId) ?: return@withContext Result.failure(Exception("User not found"))
        if (auditNote.trim().length < 5) {
            return@withContext Result.failure(Exception("An auditable reason is required for balance adjustment."))
        }

        val newBalance = (user.balance + amount).coerceAtLeast(0.0)
        val updatedUser = user.copy(balance = newBalance)
        dao.insertOrUpdateUser(updatedUser)

        dao.insertTransaction(
            TransactionRecord(
                id = "tx_" + UUID.randomUUID().toString().take(8),
                userId = user.id,
                amount = amount,
                type = "ADMIN_ADJUSTMENT",
                title = if (amount >= 0) "Admin Credit Adjustment" else "Admin Debit Adjustment",
                description = "Note: ${auditNote.trim()}",
                status = "SUCCESS"
            )
        )

        dao.insertNotification(
            AppNotification(
                id = "notif_" + UUID.randomUUID().toString().take(8),
                userId = user.id,
                title = "Balance Adjustment",
                message = "Your wallet was adjusted by ৳${"%.2f".format(amount)}. Note: ${auditNote.trim()}",
                type = "WALLET"
            )
        )

        Result.success(Unit)
    }

    suspend fun adminToggleUserSuspension(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val user = dao.getUserById(userId) ?: return@withContext Result.failure(Exception("User not found"))
        val newStatus = !user.isSuspended
        dao.insertOrUpdateUser(user.copy(isSuspended = newStatus))
        Result.success(newStatus)
    }

    suspend fun adminCreateOrUpdateTask(task: Task): Result<Unit> = withContext(Dispatchers.IO) {
        dao.insertTask(task)
        Result.success(Unit)
    }

    suspend fun adminDeleteTask(taskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        dao.deleteTask(taskId)
        Result.success(Unit)
    }

    suspend fun adminUpdateSettings(settings: AppSettings): Result<Unit> = withContext(Dispatchers.IO) {
        dao.saveAppSettings(settings)
        Result.success(Unit)
    }

    suspend fun markAllNotificationsRead(userId: String) = withContext(Dispatchers.IO) {
        dao.markAllNotificationsRead(userId)
    }
}
