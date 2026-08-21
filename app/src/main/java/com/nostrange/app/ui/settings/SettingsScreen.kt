package com.nostrange.app.ui.settings

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nostrange.app.domain.model.Relay
import com.nostrange.app.domain.model.RelayStatus
import com.nostrange.app.ui.components.NoMediaNotice
import com.nostrange.app.ui.theme.AccentAmber
import com.nostrange.app.ui.theme.AccentGreen
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

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val relays by viewModel.relaysFlow.collectAsState(initial = emptyList())
    val candidateCount by viewModel.candidateCount.collectAsState(initial = 0)

    var showAddRelayDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Text(
            text = "تنظیمات و حریم خصوصی",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Text(
            text = "مدیریت رله‌های Nostr، هویت رمزنگاری شده و دیتابیس محلی",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))
        NoMediaNotice()
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Nostr Identity Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = PrimaryPurple)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "هویت Nostr من", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "کلید خصوصی شما فقط در سخت‌افزار دستگاه ذخیره شده و هرگز به سرور، AI، رله یا Analytics ارسال نمی‌شود.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Public Key (npub):", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${viewModel.userPubkeyNpub.take(16)}...${viewModel.userPubkeyNpub.takeLast(10)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                color = TextPrimary
                            )
                            IconButton(onClick = {
                                viewModel.copyToClipboard(context, "Nostr npub", viewModel.userPubkeyNpub)
                                Toast.makeText(context, "کلید عمومی (npub) کپی شد", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "کپی", tint = SecondaryCyan, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Relay Management Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                Icon(imageVector = Icons.Default.Lan, contentDescription = null, tint = SecondaryCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "رله‌های غیرمتمرکز Nostr", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            }
                            IconButton(onClick = { showAddRelayDialog = true }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "افزودن رله", tint = SecondaryCyan)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        relays.forEach { relay ->
                            RelayItemRow(
                                relay = relay,
                                onReconnect = { viewModel.reconnectRelay(relay.url) },
                                onRemove = { viewModel.removeRelay(relay.url) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }

            // Local Database Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = AccentAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "دیتابیس محلی (Room)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "تعداد پروفایل‌های کاندیدای کش شده: $candidateCount (ظرفیت تا 10,000+)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.clearCandidateCache()
                                Toast.makeText(context, "کش پروفایل‌های کاندیدا پاک شد.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = AccentPink, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("پاک‌سازی کش کاندیداها", color = AccentPink)
                        }
                    }
                }
            }

            // Privacy Audit Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = AccentGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "تضمین‌های حفظ حریم خصوصی", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "✓ بدون سرور یا دیتابیس مرکزی متعلق به توسعه‌دهنده", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(text = "✓ بدون هوش مصنوعی مرکزی یا ارسال لاگ و تحلیل", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(text = "✓ بدون قابلیت عکس، ویدئو یا هرگونه فایل رسانه‌ای", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(text = "✓ فیلترینگ سخت‌گیرانه اطلاعات هویتی و شماره تماس", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(text = "✓ پردازش و امتیازدهی سازگاری روی حافظه محلی دستگاه", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }
    }

    if (showAddRelayDialog) {
        AddRelayDialog(
            onDismiss = { showAddRelayDialog = false },
            onAddRelay = { url ->
                viewModel.addRelay(url)
                showAddRelayDialog = false
            }
        )
    }
}

@Composable
private fun RelayItemRow(
    relay: Relay,
    onReconnect: () -> Unit,
    onRemove: () -> Unit
) {
    val statusColor = when (relay.status) {
        RelayStatus.CONNECTED -> AccentGreen
        RelayStatus.CONNECTING -> AccentAmber
        RelayStatus.ERROR -> AccentPink
        RelayStatus.DISCONNECTED -> TextMuted
    }

    val statusText = when (relay.status) {
        RelayStatus.CONNECTED -> "متصل"
        RelayStatus.CONNECTING -> "در حال اتصال..."
        RelayStatus.ERROR -> "خطا"
        RelayStatus.DISCONNECTED -> "قطع"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = relay.url,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = statusText, color = statusColor, fontSize = 10.sp)
            }
        }

        Row {
            IconButton(onClick = onReconnect, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "اتصال مجدد", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = AccentPink, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun AddRelayDialog(
    onDismiss: () -> Unit,
    onAddRelay: (String) -> Unit
) {
    var relayUrl by remember { mutableStateOf("wss://") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(text = "افزودن رله Nostr جدید", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        },
        text = {
            Column {
                Text(text = "آدرس WebSocket رله عمومی یا خصوصی خود را وارد کنید:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = relayUrl,
                    onValueChange = { relayUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryPurple
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAddRelay(relayUrl) },
                enabled = relayUrl.startsWith("wss://") || relayUrl.startsWith("ws://"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("افزودن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", color = TextSecondary)
            }
        }
    )
}
