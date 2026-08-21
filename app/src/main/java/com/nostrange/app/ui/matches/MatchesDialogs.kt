package com.nostrange.app.ui.matches

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.nostrange.app.domain.model.CandidateProfile
import com.nostrange.app.ui.components.CompatibilityBadge
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
    var jsonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "وارد کردن خروجی رتبه‌بندی هوش مصنوعی",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "خروجی JSON حاصل از تحلیل AI برای 50 مورد برتر را در کادر زیر وارد کنید:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    placeholder = { Text("{\n  \"matches\": [\n    {\n      \"rank\": 1,\n      \"pubkey\": \"...\"\n    }\n  ]\n}", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryPurple
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.primaryClip?.let { clip ->
                            if (clip.itemCount > 0) {
                                jsonText = clip.getItemAt(0).text.toString()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, tint = SecondaryCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("جای‌گذاری از Clipboard", color = SecondaryCyan)
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage, color = AccentPink, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmImport(jsonText) },
                enabled = jsonText.isNotBlank() && !isImporting,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp).width(18.dp), color = TextPrimary)
                } else {
                    Text("اعتبارسنجی و ثبت رتبه‌بندی")
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
            Text(
                text = "مشخصات سازگاری کاندیدا",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                    Text(text = "${candidate.country} / ${candidate.region}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    CompatibilityBadge(score = candidate.aiScore ?: candidate.initial_score)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "سن: ${candidate.age} سال | جنسیت: ${candidate.gender}", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Text(text = "هدف رابطه: ${candidate.relationship_goal}", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                Text(text = "ازدواج: ${if (candidate.wants_marriage) "بله" else "خیر"} | فرزند: ${if (candidate.wants_children) "بله" else "خیر"}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "ویژگی‌های روان‌شناختی (0-100):", color = PrimaryPurple, style = MaterialTheme.typography.labelSmall)
                Text(text = "استقلال: ${candidate.personality.independence} | اجتماعی بودن: ${candidate.personality.sociability} | گشودگی: ${candidate.personality.openness} | ثبات هیجانی: ${candidate.personality.emotional_stability}", color = TextSecondary, style = MaterialTheme.typography.labelSmall)

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "ارزش‌ها:", color = PrimaryPurple, style = MaterialTheme.typography.labelSmall)
                Text(text = candidate.values.joinToString(", "), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "علایق:", color = PrimaryPurple, style = MaterialTheme.typography.labelSmall)
                Text(text = candidate.interests.joinToString(", "), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)

                if (candidate.ai_reasons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "دلایل هوش مصنوعی:", color = SecondaryCyan, style = MaterialTheme.typography.labelSmall)
                    candidate.ai_reasons.forEach { r ->
                        Text(text = "• $r", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRequestIntro,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Text("درخواست آشنایی")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن", color = TextSecondary)
            }
        }
    )
}
