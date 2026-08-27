package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppNotification
import com.example.data.model.AppSettings
import com.example.data.model.ReferralItem
import com.example.data.model.Task
import com.example.data.model.TaskSession
import com.example.data.model.TaskSubmission
import com.example.data.model.TransactionRecord
import com.example.data.model.User
import com.example.data.model.WithdrawalRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        User::class,
        Task::class,
        TaskSession::class,
        TaskSubmission::class,
        TransactionRecord::class,
        WithdrawalRequest::class,
        ReferralItem::class,
        AppNotification::class,
        AppSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SelfWorkDatabase : RoomDatabase() {

    abstract fun dao(): SelfWorkDao

    companion object {
        @Volatile
        private var INSTANCE: SelfWorkDatabase? = null

        fun getInstance(context: Context): SelfWorkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SelfWorkDatabase::class.java,
                    "self_work_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database.dao())
                }
            }
        }

        private suspend fun populateInitialData(dao: SelfWorkDao) {
            // Seed Settings
            val defaultSettings = AppSettings(
                id = "global_config",
                hourlyReward = 15.0, // ৳15 per hour default
                minimumWithdrawal = 50.0,
                withdrawalFeePercent = 0.0,
                referralReward = 10.0,
                maxDailyEarning = 300.0,
                taskCooldownMinutes = 0,
                maintenanceMode = false,
                telegramChannelUrl = "https://t.me/selfworkofficial",
                announcementText = "🎉 Welcome to Self Work! Legit tasks rewarded at ৳15/hour. Join our Telegram for live updates.",
                startIoAppId = "207153365"
            )
            dao.saveAppSettings(defaultSettings)

            // Seed Demo User
            val demoUser = User(
                id = "user_demo_101",
                name = "Sifat Islam",
                email = "mdsifatislam609@gmail.com",
                phone = "01700123456",
                referralCode = "SW7890",
                referredBy = null,
                balance = 45.0,
                totalEarnings = 120.0,
                totalWithdrawn = 75.0,
                pendingWithdrawal = 0.0,
                taskEarnings = 105.0,
                referralEarnings = 15.0,
                todayEarnings = 15.0,
                completedTasksCount = 4,
                activeTaskId = null,
                role = "USER"
            )
            dao.insertOrUpdateUser(demoUser)

            // Seed Admin Profile
            val adminUser = User(
                id = "admin_root_999",
                name = "Self Work Admin",
                email = "admin@selfwork.official",
                phone = "01999887766",
                referralCode = "SWADMIN",
                role = "ADMIN"
            )
            dao.insertOrUpdateUser(adminUser)

            // Seed Authentic Tasks
            val initialTasks = listOf(
                Task(
                    id = "task_001",
                    title = "E-Commerce Product Data Categorization",
                    category = "Data Entry",
                    description = "Accurately sort and verify product catalog attributes, SKU details, and Bangla pricing for 50 marketplace items.",
                    instructions = "1. Review the provided sample product spreadsheet.\n2. Verify product category (Electronics / Fashion / Grocery).\n3. Check Bangla title formatting and ensure pricing matches standard BDT.\n4. Record all corrections and upload spreadsheet link or formatted summary.",
                    estimatedDurationMinutes = 60,
                    hourlyRate = 15.0,
                    totalPossibleReward = 15.0,
                    deadlineStr = "Tonight, 11:59 PM",
                    requiredProofType = "Completed Sheet Link / Summary Notes",
                    isFeatured = true
                ),
                Task(
                    id = "task_002",
                    title = "Bangla Voice-to-Text Audio Verification",
                    category = "Transcription",
                    description = "Listen to short standard Bengali audio snippets and verify the transcribed sentences for punctuation and dialect accuracy.",
                    instructions = "1. Listen to 10 short audio clips (1-2 minutes each).\n2. Fix spelling errors in Bengali Unicode text.\n3. Mark timestamps of any inaudible words.\n4. Submit the corrected transcript in the submission box.",
                    estimatedDurationMinutes = 120,
                    hourlyRate = 15.0,
                    totalPossibleReward = 30.0,
                    deadlineStr = "Tomorrow, 6:00 PM",
                    requiredProofType = "Corrected Bengali Transcript Text",
                    isFeatured = true
                ),
                Task(
                    id = "task_003",
                    title = "Mobile Application Usability & Bug Testing",
                    category = "QA & Testing",
                    description = "Test the Self Work UI flow across different screens, test dark mode toggling, button responsiveness, and report anomalies.",
                    instructions = "1. Navigate through all 5 bottom navigation tabs.\n2. Perform a test stopwatch session for at least 5 minutes.\n3. Test the Withdrawal form validation for bKash & Nagad numbers.\n4. Submit a structured bug report or UX feedback checklist.",
                    estimatedDurationMinutes = 60,
                    hourlyRate = 15.0,
                    totalPossibleReward = 15.0,
                    deadlineStr = "Ongoing",
                    requiredProofType = "Detailed Bug/Feedback Report",
                    isFeatured = false
                ),
                Task(
                    id = "task_004",
                    title = "Bangla Educational Article Proofreading",
                    category = "Content Writing",
                    description = "Proofread a 1,200-word educational tutorial on digital literacy for grammar, clarity, and readability.",
                    instructions = "1. Read through the provided Bangla article draft.\n2. Correct grammatical, syntax, and typographical errors.\n3. Ensure sentences flow naturally.\n4. Submit the edited version along with summary notes.",
                    estimatedDurationMinutes = 180,
                    hourlyRate = 15.0,
                    totalPossibleReward = 45.0,
                    deadlineStr = "In 3 Days",
                    requiredProofType = "Proofread Text Document / Google Docs link",
                    isFeatured = false
                ),
                Task(
                    id = "task_005",
                    title = "Local Market Price Research & Entry",
                    category = "Research",
                    description = "Compile average retail prices of 20 daily grocery essentials across local markets in Dhaka/Chittagong.",
                    instructions = "1. Document prices for rice, lentils, oil, eggs, and seasonal vegetables.\n2. Note the date, market name, and unit price.\n3. Enter data cleanly formatted in table format.\n4. Submit structured notes.",
                    estimatedDurationMinutes = 60,
                    hourlyRate = 15.0,
                    totalPossibleReward = 15.0,
                    deadlineStr = "In 2 Days",
                    requiredProofType = "Market Price Table / Notes",
                    isFeatured = false
                )
            )
            dao.insertTasks(initialTasks)

            // Seed Initial Transactions for demo user
            val transactions = listOf(
                TransactionRecord(
                    id = "tx_${UUID.randomUUID()}",
                    userId = demoUser.id,
                    amount = 30.0,
                    type = "TASK_REWARD",
                    title = "Task Approved: Audio Verification",
                    description = "2 hours of verified work credited at ৳15/hr",
                    timestampMillis = System.currentTimeMillis() - 86400000L * 3,
                    status = "SUCCESS"
                ),
                TransactionRecord(
                    id = "tx_${UUID.randomUUID()}",
                    userId = demoUser.id,
                    amount = 15.0,
                    type = "TASK_REWARD",
                    title = "Task Approved: Data Entry",
                    description = "1 hour of verified work credited at ৳15/hr",
                    timestampMillis = System.currentTimeMillis() - 86400000L * 2,
                    status = "SUCCESS"
                ),
                TransactionRecord(
                    id = "tx_${UUID.randomUUID()}",
                    userId = demoUser.id,
                    amount = -75.0,
                    type = "WITHDRAWAL_PAID",
                    title = "bKash Payout Paid",
                    description = "Paid to 01700123456 (TrxID: 9X87K2LM)",
                    timestampMillis = System.currentTimeMillis() - 86400000L,
                    status = "SUCCESS"
                ),
                TransactionRecord(
                    id = "tx_${UUID.randomUUID()}",
                    userId = demoUser.id,
                    amount = 15.0,
                    type = "REFERRAL_REWARD",
                    title = "Referral Bonus",
                    description = "Friend completed first verified task",
                    timestampMillis = System.currentTimeMillis() - 3600000L * 5,
                    status = "SUCCESS"
                )
            )
            transactions.forEach { dao.insertTransaction(it) }

            // Seed Initial Submissions
            val demoSubmission = TaskSubmission(
                id = "sub_demo_1",
                taskId = "task_001",
                taskTitle = "E-Commerce Product Data Categorization",
                userId = demoUser.id,
                userName = demoUser.name,
                userEmail = demoUser.email,
                proofText = "Completed all 50 product categorizations with SKU tags and Bangla titles.",
                proofUrl = "https://docs.google.com/spreadsheets/d/sample-ecommerce-selfwork",
                workDurationMinutes = 60,
                hourlyRateAtSubmission = 15.0,
                calculatedReward = 15.0,
                status = "APPROVED",
                submittedAtMillis = System.currentTimeMillis() - 86400000L * 2,
                reviewedAtMillis = System.currentTimeMillis() - 86400000L * 2 + 3600000L,
                reviewedByAdmin = "Admin"
            )
            dao.insertSubmission(demoSubmission)

            // Seed a pending submission for testing admin panel
            val pendingSubmission = TaskSubmission(
                id = "sub_demo_pending",
                taskId = "task_002",
                taskTitle = "Bangla Voice-to-Text Audio Verification",
                userId = "user_rahim_202",
                userName = "Rahim Ahmed",
                userEmail = "rahim@example.com",
                proofText = "Verified 10 audio snippets. Fixed spelling mistakes in clips 3, 7, and 9.",
                proofUrl = "https://example.com/rahim-transcripts.txt",
                workDurationMinutes = 120,
                hourlyRateAtSubmission = 15.0,
                calculatedReward = 30.0,
                status = "PENDING",
                submittedAtMillis = System.currentTimeMillis() - 1800000L
            )
            dao.insertSubmission(pendingSubmission)

            // Seed a demo user for Rahim
            val rahimUser = User(
                id = "user_rahim_202",
                name = "Rahim Ahmed",
                email = "rahim@example.com",
                phone = "01811223344",
                referralCode = "SW9911",
                referredBy = "SW7890",
                balance = 15.0,
                totalEarnings = 15.0,
                totalWithdrawn = 0.0,
                pendingWithdrawal = 0.0,
                taskEarnings = 15.0,
                referralEarnings = 0.0,
                todayEarnings = 0.0,
                completedTasksCount = 1
            )
            dao.insertOrUpdateUser(rahimUser)

            // Seed a pending withdrawal for testing admin panel
            val pendingWithdrawal = WithdrawalRequest(
                id = "wdr_demo_1",
                userId = "user_rahim_202",
                userName = "Rahim Ahmed",
                userEmail = "rahim@example.com",
                method = "bKash",
                accountNumber = "01811223344",
                amount = 50.0,
                fee = 0.0,
                netAmount = 50.0,
                status = "PENDING",
                requestedAtMillis = System.currentTimeMillis() - 3600000L * 2
            )
            dao.insertWithdrawal(pendingWithdrawal)

            // Seed Notifications
            val welcomeNotification = AppNotification(
                id = "notif_1",
                userId = demoUser.id,
                title = "Welcome to Self Work!",
                message = "Earn approved rewards at ৳15/hour by doing legitimate work. Check available tasks now.",
                type = "SYSTEM",
                isRead = false,
                timestampMillis = System.currentTimeMillis() - 86400000L * 3
            )
            val taskApprovedNotification = AppNotification(
                id = "notif_2",
                userId = demoUser.id,
                title = "Task Approved (+৳15)",
                message = "Your submission for E-Commerce Data Categorization has been approved and ৳15 credited to your wallet.",
                type = "TASK",
                isRead = true,
                timestampMillis = System.currentTimeMillis() - 86400000L * 2
            )
            dao.insertNotification(welcomeNotification)
            dao.insertNotification(taskApprovedNotification)
        }
    }
}
