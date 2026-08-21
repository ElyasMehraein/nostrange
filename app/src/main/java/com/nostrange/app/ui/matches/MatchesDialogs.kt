package com.nostrange.app.ui.matches

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.nostrange.app.domain.model.CandidateProfile
import com.nostrange.app.ui.components.CompatibilityBadge
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
fun ImportMatchesDialog(
    isImporting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirmImport: (String) -> Unit
) {
    val context = LocalContext.current
    var rawText by remember { mutableStateOf("") }

    val cleanedJson = remember(rawText) {
        if (rawText.isBlank()) "" else JsonSanitizerUtils.extractJson(rawText)
    }

    // تشخیص زنده تعداد کاندیداها
    val detectedMatchesCount = remember(cleanedJson) {
        if (cleanedJson.isBlank()) 0
        else {
            val pubkeyMatches = Regex("\"pubkey\"\\s*:").findAll(cleanedJson).count()
            val rankMatches = Regex("\"rank\"\\s*:").findAll(cleanedJson).count()
            maxOf(pubkeyMatches, rankMatches)
        }
    }

    val isValidFormat = detectedMatchesCount > 0

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
                        .background(SecondaryCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = SecondaryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "ثبت نتایج رتبه‌بندی هوش مصنوعی",
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "خروجی JSON حاصل از تحلیل هوش مصنوعی (شامل کاندیداهای منتخب و دلایل) را در کادر زیر Paste کنید:",
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
                    placeholder = { Text("{\n  \"matches\": [\n    {\n      \"rank\": 1,\n      \"pubkey\": \"...\"\n    }\n  ]\n}", color = TextMuted, fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isValidFormat) AccentGreen else SecondaryCyan,
                        unfocusedBorderColor = if (isValidFormat) AccentGreen.copy(alpha = 0.5f) else DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = SecondaryCyan
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )

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

                // بازخورد پیش‌نمایش زنده
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
                            text = if (isValidFormat) "✓ شناسایی $detectedMatchesCount کاندیدای رتبه‌بندی‌شده" else "⚠️ خروجی باید شامل آرایه matches و شناسه کاندیداها باشد.",
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
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryCyan)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DarkSurface)
                } else {
                    Text("ثبت و نمایش در نتایج", color = DarkSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun CandidateDetailDialog(
    candidate: CandidateProfile,
    onDismiss: () -> Unit,
    onRequestIntro: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مشخصات و تحلیل کاندیدا",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${candidate.country} / ${candidate.region}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                CompatibilityBadge(score = candidate.aiScore ?: candidate.initial_score)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // بخش دلایل و استدلال‌های هوش مصنوعی (Explainable AI Reasoning)
                if (candidate.ai_reasons.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = SecondaryCyan.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryCyan.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = SecondaryCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "استدلال هوش مصنوعی برای این پیشنهاد:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SecondaryCyan
                                )
                            }
                            candidate.ai_reasons.forEach { reason ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text("✓ ", color = SecondaryCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        text = reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // مشخصات پایه
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "سن: ${candidate.age} سال | جنسیت: ${if (candidate.gender == "female") "خانم" else "آقا"}", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                        Text(text = "هدف رابطه: ${candidate.relationship_goal}", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                        Text(text = "ازدواج: ${if (candidate.wants_marriage) "بله" else "خیر"} | فرزند: ${if (candidate.wants_children) "بله" else "خیر"}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // ارزش‌ها و علایق
                if (candidate.values.isNotEmpty()) {
                    Text(text = "ارزش‌های کلیدی:", color = PrimaryPurple, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Text(text = candidate.values.joinToString(" • "), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }

                if (candidate.interests.isNotEmpty()) {
                    Text(text = "علایق و فعالیت‌ها:", color = PrimaryPurple, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Text(text = candidate.interests.joinToString(" • "), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRequestIntro,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("درخواست آشنایی", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن", color = TextSecondary)
            }
        }
    )
}
