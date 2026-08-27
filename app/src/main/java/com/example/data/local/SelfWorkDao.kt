package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppNotification
import com.example.data.model.AppSettings
import com.example.data.model.ReferralItem
import com.example.data.model.Task
import com.example.data.model.TaskSession
import com.example.data.model.TaskSubmission
import com.example.data.model.TransactionRecord
import com.example.data.model.User
import com.example.data.model.WithdrawalRequest
import kotlinx.coroutines.flow.Flow

@Dao
interface SelfWorkDao {

    // Users
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserFlow(userId: String): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE referralCode = :code LIMIT 1")
    suspend fun getUserByReferralCode(code: String): User?

    @Query("SELECT * FROM users ORDER BY createdAtMillis DESC")
    fun getAllUsersFlow(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    // Tasks
    @Query("SELECT * FROM tasks WHERE isActive = 1 ORDER BY isFeatured DESC, createdAtMillis DESC")
    fun getActiveTasksFlow(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY createdAtMillis DESC")
    fun getAllTasksFlow(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): Task?

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskFlow(taskId: String): Flow<Task?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<Task>)

    @Update
    suspend fun updateTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    // Task Sessions (Live Work)
    @Query("SELECT * FROM task_sessions WHERE userId = :userId AND status IN ('RUNNING', 'PAUSED') LIMIT 1")
    fun getActiveSessionFlow(userId: String): Flow<TaskSession?>

    @Query("SELECT * FROM task_sessions WHERE userId = :userId AND status IN ('RUNNING', 'PAUSED') LIMIT 1")
    suspend fun getActiveSession(userId: String): TaskSession?

    @Query("SELECT * FROM task_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): TaskSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TaskSession)

    @Update
    suspend fun updateSession(session: TaskSession)

    // Submissions
    @Query("SELECT * FROM task_submissions WHERE userId = :userId ORDER BY submittedAtMillis DESC")
    fun getUserSubmissionsFlow(userId: String): Flow<List<TaskSubmission>>

    @Query("SELECT * FROM task_submissions ORDER BY submittedAtMillis DESC")
    fun getAllSubmissionsFlow(): Flow<List<TaskSubmission>>

    @Query("SELECT * FROM task_submissions WHERE status = 'PENDING' ORDER BY submittedAtMillis ASC")
    fun getPendingSubmissionsFlow(): Flow<List<TaskSubmission>>

    @Query("SELECT * FROM task_submissions WHERE id = :id")
    suspend fun getSubmissionById(id: String): TaskSubmission?

    @Query("SELECT COUNT(*) FROM task_submissions WHERE userId = :userId AND taskId = :taskId AND status IN ('PENDING', 'APPROVED')")
    suspend fun countSubmissionsForTask(userId: String, taskId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: TaskSubmission)

    @Update
    suspend fun updateSubmission(submission: TaskSubmission)

    // Transactions
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestampMillis DESC")
    fun getUserTransactionsFlow(userId: String): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions ORDER BY timestampMillis DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionRecord)

    // Withdrawals
    @Query("SELECT * FROM withdrawals WHERE userId = :userId ORDER BY requestedAtMillis DESC")
    fun getUserWithdrawalsFlow(userId: String): Flow<List<WithdrawalRequest>>

    @Query("SELECT * FROM withdrawals ORDER BY requestedAtMillis DESC")
    fun getAllWithdrawalsFlow(): Flow<List<WithdrawalRequest>>

    @Query("SELECT * FROM withdrawals WHERE status = 'PENDING' ORDER BY requestedAtMillis ASC")
    fun getPendingWithdrawalsFlow(): Flow<List<WithdrawalRequest>>

    @Query("SELECT * FROM withdrawals WHERE id = :id")
    suspend fun getWithdrawalById(id: String): WithdrawalRequest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(withdrawal: WithdrawalRequest)

    @Update
    suspend fun updateWithdrawal(withdrawal: WithdrawalRequest)

    // Referrals
    @Query("SELECT * FROM referrals WHERE referrerId = :userId ORDER BY joinedAtMillis DESC")
    fun getUserReferralsFlow(userId: String): Flow<List<ReferralItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferral(referral: ReferralItem)

    // Notifications
    @Query("SELECT * FROM notifications WHERE userId = :userId OR userId = 'ALL' ORDER BY timestampMillis DESC")
    fun getNotificationsFlow(userId: String): Flow<List<AppNotification>>

    @Query("SELECT COUNT(*) FROM notifications WHERE (userId = :userId OR userId = 'ALL') AND isRead = 0")
    fun getUnreadNotificationCountFlow(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId OR userId = 'ALL'")
    suspend fun markAllNotificationsRead(userId: String)

    // App Settings
    @Query("SELECT * FROM app_settings WHERE id = 'global_config'")
    fun getAppSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 'global_config'")
    suspend fun getAppSettings(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAppSettings(settings: AppSettings)
}
