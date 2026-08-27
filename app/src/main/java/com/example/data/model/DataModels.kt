package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val referralCode: String,
    val referredBy: String? = null,
    val balance: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val totalWithdrawn: Double = 0.0,
    val pendingWithdrawal: Double = 0.0,
    val taskEarnings: Double = 0.0,
    val referralEarnings: Double = 0.0,
    val todayEarnings: Double = 0.0,
    val completedTasksCount: Int = 0,
    val activeTaskId: String? = null,
    val isSuspended: Boolean = false,
    val role: String = "USER", // "USER" or "ADMIN"
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String,
    val title: String,
    val category: String, // "Data Entry", "QA & Testing", "Content Writing", "Audio Transcription", "Research"
    val description: String,
    val instructions: String,
    val estimatedDurationMinutes: Int = 60,
    val hourlyRate: Double = 15.0, // Default ৳15 / hour
    val totalPossibleReward: Double = 15.0, // calculated: (estimatedDurationMinutes / 60.0) * hourlyRate
    val deadlineStr: String = "No Expiry",
    val requiredProofType: String = "Text Summary & Work Links",
    val maxSubmissionsPerUser: Int = 1,
    val isActive: Boolean = true,
    val isFeatured: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "task_sessions")
data class TaskSession(
    @PrimaryKey val sessionId: String,
    val taskId: String,
    val taskTitle: String,
    val userId: String,
    val startTimeMillis: Long = System.currentTimeMillis(),
    val lastHeartbeatMillis: Long = System.currentTimeMillis(),
    val pausedTimeTotalMillis: Long = 0L,
    val isPaused: Boolean = false,
    val pauseTimestampMillis: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val isCompleted: Boolean = false,
    val status: String = "RUNNING", // RUNNING, PAUSED, SUBMITTED, CANCELLED
    val antiCheatHash: String = ""
)

@Entity(tableName = "task_submissions")
data class TaskSubmission(
    @PrimaryKey val id: String,
    val taskId: String,
    val taskTitle: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val proofText: String,
    val proofUrl: String = "",
    val workDurationMinutes: Int,
    val hourlyRateAtSubmission: Double,
    val calculatedReward: Double, // (workDurationMinutes / 60.0) * hourlyRateAtSubmission
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val rejectionReason: String? = null,
    val submittedAtMillis: Long = System.currentTimeMillis(),
    val reviewedAtMillis: Long? = null,
    val reviewedByAdmin: String? = null
)

@Entity(tableName = "transactions")
data class TransactionRecord(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Double,
    val type: String, // "TASK_REWARD", "REFERRAL_REWARD", "WITHDRAWAL_REQUEST", "WITHDRAWAL_PAID", "WITHDRAWAL_REJECTED", "ADMIN_ADJUSTMENT"
    val title: String,
    val description: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS", // SUCCESS, PENDING, REJECTED, REVERSED
    val referenceId: String = ""
)

@Entity(tableName = "withdrawals")
data class WithdrawalRequest(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val method: String, // "bKash", "Nagad"
    val accountNumber: String,
    val amount: Double,
    val fee: Double = 0.0,
    val netAmount: Double,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED, PAID
    val transactionReference: String? = null, // TrxID when paid
    val rejectionReason: String? = null,
    val requestedAtMillis: Long = System.currentTimeMillis(),
    val reviewedAtMillis: Long? = null
)

@Entity(tableName = "referrals")
data class ReferralItem(
    @PrimaryKey val id: String,
    val referrerId: String,
    val referredUserId: String,
    val referredUserName: String,
    val referredUserEmail: String,
    val rewardAmount: Double = 10.0,
    val status: String = "ACTIVE",
    val joinedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String, // "TASK", "WALLET", "REFERRAL", "SYSTEM", "ANNOUNCEMENT"
    val isRead: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: String = "global_config",
    val hourlyReward: Double = 15.0, // ৳15 per hour default
    val minimumWithdrawal: Double = 50.0,
    val withdrawalFeePercent: Double = 0.0,
    val referralReward: Double = 10.0,
    val maxDailyEarning: Double = 300.0,
    val taskCooldownMinutes: Int = 0,
    val maintenanceMode: Boolean = false,
    val telegramChannelUrl: String = "https://t.me/selfworkofficial",
    val announcementText: String = "Welcome to Self Work! Real tasks, verified rewards at ৳15/hour. Work diligently and submit authentic proof.",
    val startIoAppId: String = "207153365",
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)
