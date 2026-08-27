package com.example.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppSettings
import com.example.data.model.Task
import com.example.data.model.TaskSubmission
import com.example.data.model.User
import com.example.data.model.WithdrawalRequest
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatMetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberGold
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RoseError
import com.example.ui.theme.SapphireBlue
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val pendingSubmissions by viewModel.pendingSubmissions.collectAsState()
    val allSubmissions by viewModel.allSubmissions.collectAsState()
    val pendingWithdrawals by viewModel.pendingWithdrawals.collectAsState()
    val allWithdrawals by viewModel.allWithdrawals.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()

    var adminPin by remember { mutableStateOf("") }
    var selectedAdminTab by remember { mutableIntStateOf(0) }
    val adminTabs = listOf("Metrics", "Submissions (${pendingSubmissions.size})", "Withdrawals (${pendingWithdrawals.size})", "Tasks", "Users", "Settings")

    // State for action dialogs
    var rejectSubmissionTarget by remember { mutableStateOf<TaskSubmission?>(null) }
    var rejectReasonText by remember { mutableStateOf("") }

    var processWithdrawalTarget by remember { mutableStateOf<WithdrawalRequest?>(null) }
    var withdrawalActionType by remember { mutableStateOf("MARK_PAID") }
    var trxIdOrRejectReason by remember { mutableStateOf("") }

    var userAdjustmentTarget by remember { mutableStateOf<User?>(null) }
    var adjustAmountText by remember { mutableStateOf("15.0") }
    var adjustReasonText by remember { mutableStateOf("") }

    var showCreateTaskDialog by remember { mutableStateOf(false) }

    if (!isAdminLoggedIn) {
        // Admin PIN lock screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Self Work Admin Portal",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Enter secure administrative PIN to continue (Default: 2026)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = adminPin,
                onValueChange = { adminPin = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Admin Security PIN") },
                placeholder = { Text("2026") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.loginAdmin(adminPin) {}
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Unlock Admin Portal", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Return to App")
            }
        }
        return
    }

    // Admin Dashboard View
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("Admin Control Panel", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Self Work Management", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary)
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.logoutAdmin() }) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock", tint = RoseError)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        ScrollableTabRow(
            selectedTabIndex = selectedAdminTab,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 12.dp
        ) {
            adminTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedAdminTab == index,
                    onClick = { selectedAdminTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedAdminTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        when (selectedAdminTab) {
            0 -> {
                // METRICS TAB
                val totalApprovedSubmissions = allSubmissions.count { it.status == "APPROVED" }
                val totalRewardsDisbursed = allSubmissions.filter { it.status == "APPROVED" }.sumOf { it.calculatedReward }
                val totalPaidWithdrawals = allWithdrawals.filter { it.status == "PAID" }.sumOf { it.netAmount }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatMetricCard(
                                title = "Total Workers",
                                value = "${allUsers.size}",
                                subtitle = "Registered users",
                                icon = Icons.Default.People,
                                accentColor = SapphireBlue,
                                modifier = Modifier.weight(1f)
                            )

                            StatMetricCard(
                                title = "Active Tasks",
                                value = "${allTasks.count { it.isActive }}",
                                subtitle = "Available jobs",
                                icon = Icons.Default.Work,
                                accentColor = EmeraldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatMetricCard(
                                title = "Pending Reviews",
                                value = "${pendingSubmissions.size}",
                                subtitle = "Task submissions",
                                icon = Icons.Default.CheckCircle,
                                accentColor = AmberGold,
                                modifier = Modifier.weight(1f)
                            )

                            StatMetricCard(
                                title = "Pending Payouts",
                                value = "${pendingWithdrawals.size}",
                                subtitle = "bKash & Nagad",
                                icon = Icons.Default.Payments,
                                accentColor = RoseError,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatMetricCard(
                                title = "Tasks Approved",
                                value = "$totalApprovedSubmissions",
                                subtitle = "৳${"%.2f".format(totalRewardsDisbursed)} credited",
                                icon = Icons.Default.MonetizationOn,
                                accentColor = EmeraldPrimary,
                                modifier = Modifier.weight(1f)
                            )

                            StatMetricCard(
                                title = "Total Payouts Paid",
                                value = "৳${"%.2f".format(totalPaidWithdrawals)}",
                                subtitle = "Verified withdrawals",
                                icon = Icons.Default.Payments,
                                accentColor = SapphireBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            1 -> {
                // TASK SUBMISSIONS REVIEW TAB
                if (pendingSubmissions.isEmpty()) {
                    EmptyStateView(
                        title = "No Pending Submissions",
                        message = "All worker task submissions have been reviewed and processed.",
                        icon = Icons.Default.CheckCircle
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pendingSubmissions) { sub ->
                            AdminSubmissionCard(
                                submission = sub,
                                onApprove = { viewModel.adminApproveSubmission(sub.id) },
                                onReject = {
                                    rejectSubmissionTarget = sub
                                    rejectReasonText = ""
                                }
                            )
                        }
                    }
                }
            }

            2 -> {
                // WITHDRAWALS MANAGEMENT TAB
                if (pendingWithdrawals.isEmpty()) {
                    EmptyStateView(
                        title = "No Pending Withdrawals",
                        message = "All worker withdrawal requests have been verified and processed.",
                        icon = Icons.Default.Payments
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pendingWithdrawals) { wdr ->
                            AdminWithdrawalCard(
                                withdrawal = wdr,
                                onMarkPaid = {
                                    processWithdrawalTarget = wdr
                                    withdrawalActionType = "MARK_PAID"
                                    trxIdOrRejectReason = "TrxID_${UUID.randomUUID().toString().take(6).uppercase()}"
                                },
                                onReject = {
                                    processWithdrawalTarget = wdr
                                    withdrawalActionType = "REJECT"
                                    trxIdOrRejectReason = ""
                                }
                            )
                        }
                    }
                }
            }

            3 -> {
                // TASK MANAGEMENT TAB
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { showCreateTaskDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create New Work Task", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allTasks) { task ->
                            AdminTaskCard(
                                task = task,
                                onToggleStatus = {
                                    viewModel.adminCreateOrUpdateTask(task.copy(isActive = !task.isActive)) {}
                                },
                                onDelete = { viewModel.adminDeleteTask(task.id) }
                            )
                        }
                    }
                }
            }

            4 -> {
                // USER MANAGEMENT TAB
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allUsers) { user ->
                        AdminUserCard(
                            user = user,
                            onAdjustBalance = {
                                userAdjustmentTarget = user
                                adjustAmountText = "15.0"
                                adjustReasonText = ""
                            },
                            onToggleSuspension = { viewModel.adminToggleSuspension(user.id) }
                        )
                    }
                }
            }

            5 -> {
                // GLOBAL SETTINGS TAB
                AdminSettingsTab(
                    settings = appSettings ?: AppSettings(),
                    onSave = { updated ->
                        viewModel.adminUpdateSettings(updated) {}
                    }
                )
            }
        }
    }

    // Reject Submission Dialog
    rejectSubmissionTarget?.let { sub ->
        AlertDialog(
            onDismissRequest = { rejectSubmissionTarget = null },
            title = { Text("Reject Submission") },
            text = {
                Column {
                    Text("Enter rejection reason for worker (${sub.userName}):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReasonText,
                        onValueChange = { rejectReasonText = it },
                        placeholder = { Text("e.g. Incomplete proof / Incorrect data entry") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = rejectSubmissionTarget!!
                        rejectSubmissionTarget = null
                        viewModel.adminRejectSubmission(target.id, rejectReasonText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) {
                    Text("Confirm Rejection")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { rejectSubmissionTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Process Withdrawal Dialog
    processWithdrawalTarget?.let { wdr ->
        AlertDialog(
            onDismissRequest = { processWithdrawalTarget = null },
            title = {
                Text(if (withdrawalActionType == "MARK_PAID") "Mark as Paid (${wdr.method})" else "Reject & Refund Withdrawal")
            },
            text = {
                Column {
                    Text("User: ${wdr.userName} (${wdr.accountNumber})")
                    Text("Amount: ৳${"%.2f".format(wdr.amount)} (Net: ৳${"%.2f".format(wdr.netAmount)})")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = trxIdOrRejectReason,
                        onValueChange = { trxIdOrRejectReason = it },
                        label = { Text(if (withdrawalActionType == "MARK_PAID") "Official Transaction ID (TrxID)" else "Rejection Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = processWithdrawalTarget!!
                        processWithdrawalTarget = null
                        viewModel.adminProcessWithdrawal(target.id, withdrawalActionType, trxIdOrRejectReason)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (withdrawalActionType == "MARK_PAID") EmeraldPrimary else RoseError
                    )
                ) {
                    Text(if (withdrawalActionType == "MARK_PAID") "Confirm Paid" else "Reject & Refund")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { processWithdrawalTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Adjust Balance Dialog
    userAdjustmentTarget?.let { user ->
        AlertDialog(
            onDismissRequest = { userAdjustmentTarget = null },
            title = { Text("Audit Balance Adjustment") },
            text = {
                Column {
                    Text("Worker: ${user.name} (${user.email})")
                    Text("Current Balance: ৳${"%.2f".format(user.balance)}")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = adjustAmountText,
                        onValueChange = { adjustAmountText = it },
                        label = { Text("Adjustment Amount (+ or - ৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = adjustReasonText,
                        onValueChange = { adjustReasonText = it },
                        label = { Text("Auditable Reason (Required)") },
                        placeholder = { Text("e.g. Manual task compensation") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = adjustAmountText.toDoubleOrNull() ?: 0.0
                        val target = userAdjustmentTarget!!
                        userAdjustmentTarget = null
                        viewModel.adminAdjustBalance(target.id, amt, adjustReasonText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Adjustment")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { userAdjustmentTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create Task Dialog
    if (showCreateTaskDialog) {
        AdminCreateTaskDialog(
            hourlyRate = appSettings?.hourlyReward ?: 15.0,
            onDismiss = { showCreateTaskDialog = false },
            onCreate = { newTask ->
                showCreateTaskDialog = false
                viewModel.adminCreateOrUpdateTask(newTask) {}
            }
        )
    }
}

@Composable
fun AdminSubmissionCard(
    submission: TaskSubmission,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = submission.taskTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Worker: ${submission.userName} (${submission.userEmail})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "৳${"%.2f".format(submission.calculatedReward)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Work Duration: ${submission.workDurationMinutes} mins • Rate: ৳${submission.hourlyRateAtSubmission.toInt()}/hr",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "Proof Evidence:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = submission.proofText,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (submission.proofUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "URL: ${submission.proofUrl}",
                            style = MaterialTheme.typography.labelSmall.copy(color = SapphireBlue)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError)
                ) {
                    Text("Reject Work")
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve & Credit ৳${"%.2f".format(submission.calculatedReward)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AdminWithdrawalCard(
    withdrawal: WithdrawalRequest,
    onMarkPaid: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${withdrawal.method}: ${withdrawal.accountNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Worker: ${withdrawal.userName} (${withdrawal.userEmail})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "৳${"%.2f".format(withdrawal.netAmount)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = AmberGold
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError)
                ) {
                    Text("Reject / Refund")
                }

                Button(
                    onClick = onMarkPaid,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Mark as PAID (TrxID)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AdminTaskCard(
    task: Task,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${task.category} • ${task.estimatedDurationMinutes}m • ৳${task.hourlyRate.toInt()}/hr (Total: ৳${"%.2f".format(task.totalPossibleReward)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onToggleStatus,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (task.isActive) "Active" else "Disabled", fontSize = 11.sp)
                }

                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RoseError)
                }
            }
        }
    }
}

@Composable
fun AdminUserCard(
    user: User,
    onAdjustBalance: () -> Unit,
    onToggleSuspension: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name + if (user.isSuspended) " [SUSPENDED]" else "",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (user.isSuspended) RoseError else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "${user.email} • Code: ${user.referralCode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "৳${"%.2f".format(user.balance)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAdjustBalance,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Adjust Balance", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onToggleSuspension,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (user.isSuspended) EmeraldPrimary else RoseError
                    )
                ) {
                    Text(if (user.isSuspended) "Activate" else "Suspend", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun AdminSettingsTab(
    settings: AppSettings,
    onSave: (AppSettings) -> Unit
) {
    var hourlyRewardText by remember { mutableStateOf(settings.hourlyReward.toString()) }
    var minWithdrawalText by remember { mutableStateOf(settings.minimumWithdrawal.toString()) }
    var referralBonusText by remember { mutableStateOf(settings.referralReward.toString()) }
    var telegramUrlText by remember { mutableStateOf(settings.telegramChannelUrl) }
    var announcementText by remember { mutableStateOf(settings.announcementText) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Platform Earning & System Settings",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        OutlinedTextField(
            value = hourlyRewardText,
            onValueChange = { hourlyRewardText = it },
            label = { Text("Default Hourly Reward (BDT ৳)") },
            placeholder = { Text("15.0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = minWithdrawalText,
            onValueChange = { minWithdrawalText = it },
            label = { Text("Minimum Withdrawal Limit (BDT ৳)") },
            placeholder = { Text("50.0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = referralBonusText,
            onValueChange = { referralBonusText = it },
            label = { Text("Referral Bonus per Worker (BDT ৳)") },
            placeholder = { Text("10.0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = telegramUrlText,
            onValueChange = { telegramUrlText = it },
            label = { Text("Official Telegram Channel URL") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = announcementText,
            onValueChange = { announcementText = it },
            label = { Text("In-App Broadcast Announcement") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val updated = settings.copy(
                    hourlyReward = hourlyRewardText.toDoubleOrNull() ?: 15.0,
                    minimumWithdrawal = minWithdrawalText.toDoubleOrNull() ?: 50.0,
                    referralReward = referralBonusText.toDoubleOrNull() ?: 10.0,
                    telegramChannelUrl = telegramUrlText.trim(),
                    announcementText = announcementText.trim()
                )
                onSave(updated)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save Global Settings", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AdminCreateTaskDialog(
    hourlyRate: Double,
    onDismiss: () -> Unit,
    onCreate: (Task) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Data Entry") }
    var description by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("60") }
    var proofType by remember { mutableStateOf("Completed Sheet Link / Summary Notes") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create New Task", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Data Entry, QA & Testing)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    label = { Text("Estimated Duration (Minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Brief Description") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Step-by-Step Instructions") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = proofType,
                    onValueChange = { proofType = it },
                    label = { Text("Required Proof Type") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val duration = durationText.toIntOrNull() ?: 60
                        val reward = (duration / 60.0) * hourlyRate
                        val newTask = Task(
                            id = "task_" + UUID.randomUUID().toString().take(8),
                            title = title.trim().ifBlank { "Untitled Task" },
                            category = category.trim().ifBlank { "General Work" },
                            description = description.trim(),
                            instructions = instructions.trim(),
                            estimatedDurationMinutes = duration,
                            hourlyRate = hourlyRate,
                            totalPossibleReward = reward,
                            requiredProofType = proofType.trim(),
                            isActive = true
                        )
                        onCreate(newTask)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Publish Task", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
