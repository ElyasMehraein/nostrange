package com.nostrange.app.ui.chats

import android.widget.Toast
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nostrange.app.domain.model.ChatMessage
import com.nostrange.app.domain.model.Conversation
import com.nostrange.app.ui.components.CompatibilityBadge
import com.nostrange.app.ui.components.NoMediaNotice
import com.nostrange.app.ui.components.PrivacyDisclaimerBanner
import com.nostrange.app.ui.theme.AccentPink
import com.nostrange.app.ui.theme.DarkBackground
import com.nostrange.app.ui.theme.DarkBorder
import com.nostrange.app.ui.theme.DarkSurface
import com.nostrange.app.ui.theme.DarkSurfaceVariant
import com.nostrange.app.ui.theme.PrimaryPurple
import com.nostrange.app.ui.theme.SecondaryCyan
import com.nostrange.app.ui.theme.TextMuted
import com.nostrange.app.ui.theme.TextPrimary
import com.nostrange.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatsScreen(
    onOpenChat: (String) -> Unit,
    viewModel: ChatsViewModel = viewModel()
) {
    val conversations by viewModel.conversationsFlow.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // RTL Persian Header
        Text(
            text = "گفتگوهای خصوصی",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Text(
            text = "پیام‌رسانی سرتاسر رمزنگاری شده روی Nostr",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(6.dp))
        NoMediaNotice()
        Spacer(modifier = Modifier.height(8.dp))

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "هنوز گفتگویی شروع نشده است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "در تب «همسان‌ها» می‌توانید برای افراد سازگار «درخواست آشنایی» بفرستید.",
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conversations, key = { it.partnerPubkey }) { conv ->
                    ConversationItem(
                        conversation = conv,
                        onClick = { onOpenChat(conv.partnerPubkey) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    val truncatedPubkey = if (conversation.partnerPubkey.length > 16) {
        "${conversation.partnerPubkey.take(8)}...${conversation.partnerPubkey.takeLast(8)}"
    } else {
        conversation.partnerPubkey
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = truncatedPubkey,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CompatibilityBadge(score = conversation.matchScore)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = conversation.lastMessage.ifBlank { "شروع گفتگو..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    partnerPubkey: String,
    onBack: () -> Unit,
    viewModel: ChatDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val partnerScore by viewModel.partnerScore.collectAsState()
    val isBlocked by viewModel.isBlocked.collectAsState()

    var inputMessage by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(partnerPubkey) {
        viewModel.loadChat(partnerPubkey)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val truncatedPubkey = if (partnerPubkey.length > 16) {
        "${partnerPubkey.take(6)}...${partnerPubkey.takeLast(6)}"
    } else {
        partnerPubkey
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .imePadding()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            text = truncatedPubkey,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "سازگاری: ${String.format(Locale.US, "%.1f", partnerScore)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryCyan
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = TextPrimary)
                }
            },
            actions = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "گزینه‌ها", tint = TextPrimary)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = AccentPink, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("بلاک کردن این کاربر", color = AccentPink)
                                }
                            },
                            onClick = {
                                showMenu = false
                                viewModel.blockUser(partnerPubkey) {
                                    Toast.makeText(context, "این کاربر بلاک شد و دیگر نمی‌تواند پیام ارسال کند.", Toast.LENGTH_LONG).show()
                                    onBack()
                                }
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
        )

        // Fixed Top Privacy Disclaimer Banner
        PrivacyDisclaimerBanner(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatMessageBubble(message = msg)
            }
        }

        // Input Bar (Always raised smoothly above keyboard)
        if (isBlocked) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "شما این کاربر را بلاک کرده‌اید.",
                    color = AccentPink,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("پیام رمزنگاری شده...", color = TextMuted, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryPurple,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = false,
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.sendMessage(partnerPubkey, inputMessage)
                        inputMessage = ""
                    },
                    enabled = inputMessage.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputMessage.isNotBlank()) PrimaryPurple else DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "ارسال",
                        tint = if (inputMessage.isNotBlank()) TextPrimary else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    val timeFormatted = remember(message.timestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(message.timestamp * 1000))
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (message.isOutgoing) 14.dp else 2.dp,
                        bottomEnd = if (message.isOutgoing) 2.dp else 14.dp
                    )
                )
                .background(if (message.isOutgoing) PrimaryPurple else DarkSurfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(0.80f)
        ) {
            Column {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    lineHeight = 21.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
