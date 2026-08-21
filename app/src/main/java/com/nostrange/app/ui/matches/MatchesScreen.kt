package com.nostrange.app.ui.matches

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.nostrange.app.domain.model.CandidateProfile
import com.nostrange.app.ui.components.CompatibilityBadge
import com.nostrange.app.ui.components.NoMediaNotice
import com.nostrange.app.ui.components.StatCard
import com.nostrange.app.ui.me.PromptDisplayDialog
import com.nostrange.app.ui.theme.AccentGreen
import com.nostrange.app.ui.theme.DarkBackground
import com.nostrange.app.ui.theme.DarkBorder
import com.nostrange.app.ui.theme.DarkSurface
import com.nostrange.app.ui.theme.DarkSurfaceVariant
import com.nostrange.app.ui.theme.PrimaryPurple
import com.nostrange.app.ui.theme.SecondaryCyan
import com.nostrange.app.ui.theme.TextMuted
import com.nostrange.app.ui.theme.TextPrimary
import com.nostrange.app.ui.theme.TextSecondary

@Composable
fun MatchesScreen(
    onOpenChat: (String) -> Unit,
    viewModel: MatchesViewModel = viewModel()
) {
    val context = LocalContext.current
    val topMatches by viewModel.topAiMatches.collectAsState(initial = emptyList())
    val candidateCount by viewModel.candidateCount.collectAsState(initial = 0)
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val generatedPrompt by viewModel.generatedPrompt.collectAsState()
    val importError by viewModel.importError.collectAsState()

    var showImportDialog by remember { mutableStateOf(false) }
    var selectedCandidate by remember { mutableStateOf<CandidateProfile?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "همسان‌های سازگار (Matches)",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "رتبه‌بندی دو مرحله‌ای محلی + هوش مصنوعی شخصی",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        NoMediaNotice()
        Spacer(modifier = Modifier.height(10.dp))

        // Pipeline Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "بانک کاندیداها",
                value = "$candidateCount",
                icon = Icons.Default.Favorite,
                modifier = Modifier.weight(1f),
                tint = PrimaryPurple
            )
            StatCard(
                title = "رتبه‌بندی AI",
                value = "${topMatches.size} مورد",
                icon = Icons.Default.AutoAwesome,
                modifier = Modifier.weight(1f),
                tint = SecondaryCyan
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons: Generate AI prompt & Import AI JSON
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.generateMatchingPrompt() },
                modifier = Modifier.weight(1f),
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextPrimary)
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("پرامپت ۵۰۰ کاندیدا", fontSize = 12.sp)
                }
            }

            Button(
                onClick = { showImportDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryCyan)
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("وارد کردن ۵۰ رتبه برتر", color = DarkBackground, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Matches List
        if (topMatches.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "هنوز نتیجه رتبه‌بندی هوش مصنوعی وارد نشده است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "دکمه 'پرامپت ۵۰۰ کاندیدا' را بزنید و نتیجه را در 'وارد کردن ۵۰ رتبه برتر' ثبت کنید.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(topMatches, key = { it.pubkey }) { candidate ->
                    MatchCard(
                        candidate = candidate,
                        onClick = { selectedCandidate = candidate },
                        onRequestIntro = {
                            viewModel.requestIntroduction(candidate) {
                                Toast.makeText(context, "درخواست آشنایی رمزنگاری شده ارسال شد!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenChat = { onOpenChat(candidate.pubkey) }
                    )
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
                Toast.makeText(context, "پرامپت کاندیداها کپی شد!", Toast.LENGTH_SHORT).show()
            },
            onShare = {
                viewModel.sharePrompt(context, generatedPrompt!!)
            }
        )
    }

    if (showImportDialog) {
        ImportMatchesDialog(
            isImporting = isImporting,
            errorMessage = importError,
            onDismiss = { showImportDialog = false },
            onConfirmImport = { rawJson ->
                viewModel.importMatchingResult(rawJson) {
                    showImportDialog = false
                    Toast.makeText(context, "رتبه‌بندی هوش مصنوعی با موفقیت اعمال شد!", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    selectedCandidate?.let { candidate ->
        CandidateDetailDialog(
            candidate = candidate,
            onDismiss = { selectedCandidate = null },
            onRequestIntro = {
                viewModel.requestIntroduction(candidate) {
                    selectedCandidate = null
                    Toast.makeText(context, "درخواست آشنایی ارسال شد!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun MatchCard(
    candidate: CandidateProfile,
    onClick: () -> Unit,
    onRequestIntro: () -> Unit,
    onOpenChat: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Rank Badge
                    candidate.ai_rank?.let { rank ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#$rank",
                                color = PrimaryPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Column {
                        Text(
                            text = "${candidate.country} / ${candidate.region}",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${candidate.age} سال • ${candidate.gender} • هدف: ${candidate.relationship_goal}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                CompatibilityBadge(score = candidate.ai_score ?: candidate.initial_score)
            }

            // Key AI Reasons
            if (candidate.ai_reasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .padding(8.dp)
                ) {
                    candidate.ai_reasons.take(2).forEach { reason ->
                        Text(
                            text = "✓ $reason",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (candidate.is_intro_sent) {
                    Button(
                        onClick = onOpenChat,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ادامه چت", fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onRequestIntro,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("درخواست آشنایی", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
