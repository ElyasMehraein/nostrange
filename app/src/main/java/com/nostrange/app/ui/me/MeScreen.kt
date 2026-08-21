package com.nostrange.app.ui.me

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nostrange.app.domain.model.UserProfile
import com.nostrange.app.ui.theme.AccentGreen
import com.nostrange.app.ui.theme.DarkBackground
import com.nostrange.app.ui.theme.DarkBorder
import com.nostrange.app.ui.theme.DarkSurface
import com.nostrange.app.ui.theme.DarkSurfaceVariant
import com.nostrange.app.ui.theme.PrimaryPurple
import com.nostrange.app.ui.theme.PrimaryPurpleLight
import com.nostrange.app.ui.theme.SecondaryCyan
import com.nostrange.app.ui.theme.TextMuted
import com.nostrange.app.ui.theme.TextPrimary
import com.nostrange.app.ui.theme.TextSecondary

@Composable
fun MeScreen(
    onNavigateToMatches: () -> Unit,
    viewModel: MeViewModel = viewModel()
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfileFlow.collectAsState(initial = null)
    val chatMessages by viewModel.chatMessages.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val generatedPrompt by viewModel.generatedPrompt.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importError by viewModel.importError.collectAsState()

    var showImportDialog by remember { mutableStateOf(false) }
    var inputAnswerText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // RTL Persian Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "پروفایل من",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "مصاحبه گفتگویی ساخت پروفایل سازگاری",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            IconButton(onClick = { viewModel.restartInterview() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "شروع مجدد",
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // If user profile is already saved
        if (userProfile != null) {
            ProfileSummaryCard(
                profile = userProfile!!,
                onReEdit = { viewModel.restartInterview() },
                onNavigateToMatches = onNavigateToMatches
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Conversational Chat list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatMessages, key = { it.id }) { msg ->
                ChatBubble(item = msg)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input & Actions (Always above keyboard due to imePadding)
        if (currentIndex < viewModel.questions.size) {
            val currentQ = viewModel.questions[currentIndex]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputAnswerText,
                    onValueChange = { inputAnswerText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(currentQ.placeholder, color = TextMuted, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryPurple,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = false,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.submitAnswer(inputAnswerText)
                        inputAnswerText = ""
                    },
                    enabled = inputAnswerText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputAnswerText.isNotBlank()) PrimaryPurple else DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "ارسال",
                        tint = if (inputAnswerText.isNotBlank()) TextPrimary else TextMuted
                    )
                }
            }
        } else {
            // All questions answered -> Show AI Prompt / Import buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.generateAiPrompt() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تولید پرامپت هوش مصنوعی", fontSize = 12.sp)
                }

                Button(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryCyan)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("وارد کردن خروجی JSON", color = DarkBackground, fontSize = 12.sp)
                }
            }
        }
    }

    // Dialogs
    if (generatedPrompt != null) {
        PromptDisplayDialog(
            prompt = generatedPrompt!!,
            onDismiss = { viewModel.clearGeneratedPrompt() },
            onCopy = {
                viewModel.copyPromptToClipboard(context, generatedPrompt!!)
                Toast.makeText(context, "پرامپت در حافظه کپی شد!", Toast.LENGTH_SHORT).show()
            },
            onShare = {
                viewModel.sharePrompt(context, generatedPrompt!!)
            }
        )
    }

    if (showImportDialog) {
        ImportProfileDialog(
            isImporting = isImporting,
            errorMessage = importError,
            onDismiss = { showImportDialog = false },
            onConfirmImport = { rawJson ->
                viewModel.importProfileJson(rawJson) {
                    showImportDialog = false
                    Toast.makeText(context, "پروفایل با موفقیت در Nostr ثبت و منتشر شد!", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

@Composable
private fun ChatBubble(item: ChatMessageItem) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (item.isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (item.isUser) 16.dp else 4.dp,
                        bottomEnd = if (item.isUser) 4.dp else 16.dp
                    )
                )
                .background(if (item.isUser) PrimaryPurple.copy(alpha = 0.85f) else DarkSurface)
                .border(
                    1.dp,
                    if (item.isUser) PrimaryPurple else DarkBorder,
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (item.isUser) 16.dp else 4.dp,
                        bottomEnd = if (item.isUser) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(0.85f)
        ) {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                lineHeight = 22.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileSummaryCard(
    profile: UserProfile,
    onReEdit: () -> Unit,
    onNavigateToMatches: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "پروفایل فعال و منتشر شده",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "${profile.country} / ${profile.region} • ${profile.age} سال",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "هدف رابطه: ${profile.relationship_goal} | جنسیت: ${profile.gender}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (profile.values.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    profile.values.take(4).forEach { v ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "#$v", color = PrimaryPurpleLight, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onReEdit) {
                    Text("ویرایش پاسخ‌ها", color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onNavigateToMatches,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("مشاهده همسان‌ها", fontSize = 12.sp)
                }
            }
        }
    }
}
