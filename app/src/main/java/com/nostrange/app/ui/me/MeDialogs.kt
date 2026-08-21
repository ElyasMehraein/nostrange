package com.nostrange.app.ui.me

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.nostrange.app.ai.schema.JsonSanitizerUtils
import com.nostrange.app.ui.theme.AccentGreen
import com.nostrange.app.ui.theme.AccentPink
import com.nostrange.app.ui.theme.DarkBorder
import com.nostrange.app.ui.theme.DarkSurface
import com.nostrange.app.ui.theme.DarkSurfaceVariant
import com.nostrange.app.ui.theme.PrimaryPurple
import com.nostrange.app.ui.theme.SecondaryCyan
import com.nostrange.app.ui.theme.TextMuted
import com.nostrange.app.ui.theme.TextPrimary
import com.nostrange.app.ui.theme.TextSecondary

@Composable
fun PromptDisplayDialog(
    prompt: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "پرامپت تحلیل پروفایل با هوش مصنوعی",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // کارت اطمینان حریم خصوصی و راهنما
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "حریم خصوصی ۱۰۰٪ تضمین‌شده",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = AccentGreen
                            )
                        }
                        Text(
                            text = "تمام اطلاعات تماس، نام و مشخصات هویتی حذف شده‌اند. این پرامپت صرفاً شامل پاسخ‌های ساختاریافته شما به سوالات است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                // راهنمای گام‌به‌گام
                Text(
                    text = "۱. پرامپت را کپی کنید.\n۲. در ChatGPT، Gemini یا Claude ارسال کنید.\n۳. خروجی JSON هوش مصنوعی را کپی کرده و در دکمه «وارد کردن خروجی JSON» ثبت نمایید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                // دکمه باز/بسته کردن متن کامل پرامپت (Accordion)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isExpanded = !isExpanded },
                    color = DarkBorder.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpanded) "پنهان‌سازی متن کامل پرامپت" else "مشاهده متن کامل پرامپت (${prompt.length} کاراکتر)",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryCyan
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = SecondaryCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // بخش متن پرامپت
                AnimatedVisibility(visible = isExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = TextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onShare) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = SecondaryCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ارسال مستقیم", color = SecondaryCyan, fontSize = 12.sp)
                }
                Button(
                    onClick = onCopy,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("کپی پرامپت", fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن", color = TextSecondary)
            }
        }
    )
}

@Composable
fun ImportProfileDialog(
    isImporting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirmImport: (String) -> Unit
) {
    val context = LocalContext.current
    var rawText by remember { mutableStateOf("") }

    // پاک‌سازی هوشمند و ارزیابی زنده ساختار JSON
    val cleanedJson = remember(rawText) {
        if (rawText.isBlank()) "" else JsonSanitizerUtils.extractJson(rawText)
    }

    val isValidFormat = remember(cleanedJson) {
        cleanedJson.isNotBlank() && cleanedJson.trim().startsWith("{") && cleanedJson.trim().endsWith("}")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "وارد کردن خروجی هوش مصنوعی",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "خروجی JSON دریافت شده از ChatGPT، Gemini یا Claude را در کادر زیر وارد کنید (متن‌های اضافی به صورت خودکار پاک‌سازی می‌شوند):",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    placeholder = { Text("پاسخ کامل چت‌بات را در اینجا Paste کنید...", color = TextMuted, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isValidFormat) AccentGreen else PrimaryPurple,
                        unfocusedBorderColor = if (isValidFormat) AccentGreen.copy(alpha = 0.5f) else DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryPurple
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )

                // دکمه جای‌گذاری هوشمند از Clipboard
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.primaryClip?.let { clip ->
                            if (clip.itemCount > 0) {
                                val pasted = clip.getItemAt(0).text?.toString() ?: ""
                                rawText = pasted
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, tint = SecondaryCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("جای‌گذاری خودکار از Clipboard", color = SecondaryCyan, fontSize = 12.sp)
                }

                // نشانگر وضعیت اعتبارسنجی زنده
                if (rawText.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isValidFormat) AccentGreen.copy(alpha = 0.15f) else AccentPink.copy(alpha = 0.15f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isValidFormat) Icons.Default.CheckCircle else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isValidFormat) AccentGreen else AccentPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isValidFormat) "✓ ساختار JSON شناسایی و آماده ثبت است." else "⚠️ متن ورودی هنوز شامل ساختار کامل JSON نیست.",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isValidFormat) AccentGreen else AccentPink
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(text = errorMessage, color = AccentPink, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmImport(cleanedJson.ifBlank { rawText }) },
                enabled = rawText.isNotBlank() && !isImporting,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextPrimary)
                } else {
                    Text("اعتبارسنجی و ذخیره پروفایل", fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", color = TextSecondary)
            }
        }
    )
}
