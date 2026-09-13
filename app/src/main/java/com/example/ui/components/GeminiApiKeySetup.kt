package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.GeminiApiKeyManager
import com.example.ui.theme.MetricStepsColor
import com.example.ui.theme.MetricWorkoutColor
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleLightText
import com.example.ui.theme.PurpleMutedText
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurpleSurface
import com.example.ui.theme.PurpleSurfaceBorder
import com.example.ui.theme.PurpleSurfaceVariant
import kotlinx.coroutines.launch

@Composable
fun GeminiApiKeySetupCard(
    onKeyUpdated: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var isConfigured by remember { mutableStateOf(GeminiApiKeyManager.isApiKeyConfigured(context)) }
    var isUserKey by remember { mutableStateOf(GeminiApiKeyManager.isUserKeyAttached(context)) }
    var maskedKey by remember { mutableStateOf(GeminiApiKeyManager.getMaskedKey(context)) }

    var isEditing by remember { mutableStateOf(!isConfigured) }
    var inputKey by remember { mutableStateOf("") }
    var isKeyVisible by remember { mutableStateOf(false) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testError by remember { mutableStateOf<String?>(null) }
    var showHowTo by remember { mutableStateOf(false) }

    fun refreshState() {
        isConfigured = GeminiApiKeyManager.isApiKeyConfigured(context)
        isUserKey = GeminiApiKeyManager.isUserKeyAttached(context)
        maskedKey = GeminiApiKeyManager.getMaskedKey(context)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PurpleSurfaceVariant)
            .border(
                1.dp,
                if (isConfigured) PurpleAccent.copy(alpha = 0.5f) else PurpleSurfaceBorder,
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isConfigured) PurplePrimary.copy(alpha = 0.35f) else PurpleSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isConfigured) PurpleAccent else PurpleMutedText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "Google Gemini AI Vision",
                        color = PurpleLightText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isConfigured) {
                            "API Key Configured • $maskedKey"
                        } else {
                            "No API Key • Enter Free Key Below"
                        },
                        color = if (isConfigured) MetricStepsColor else Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = if (isConfigured) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }

            if (isConfigured && !isEditing) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            isEditing = true
                            testResult = null
                            testError = null
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit API Key",
                            tint = PurpleAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            GeminiApiKeyManager.clearUserApiKey(context)
                            refreshState()
                            isEditing = true
                            testResult = null
                            testError = null
                            onKeyUpdated?.invoke()
                            Toast.makeText(context, "Removed Gemini API key", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove Key",
                            tint = Color(0xFFFB7185),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Privacy note
        Text(
            text = "Bring Your Own Key (BYOK) keeps your data 100% private and enables free meal photo analysis without shared server limits.",
            color = PurpleMutedText,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Guide expander toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(PurpleSurface)
                .clickable { showHowTo = !showHowTo }
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = PurpleAccent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "How to get a free Google Gemini key (30 sec)",
                    color = PurpleLightText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 15.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PurpleAccent.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (showHowTo) "Hide" else "Show",
                    color = PurpleAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AnimatedVisibility(
            visible = showHowTo,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PurpleSurface.copy(alpha = 0.6f))
                    .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "1. Tap 'Get Free Gemini Key' below to open Google AI Studio.\n" +
                            "2. Sign in with your Google account.\n" +
                            "3. Click 'Create API key' and copy the code (starts with AIzaSy...).\n" +
                            "4. Paste it into the box below and tap 'Save & Verify'.",
                    color = PurpleLightText,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.clickable {
                        uriHandler.openUri("https://aistudio.google.com/app/apikey")
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Open Google AI Studio",
                        color = PurpleAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isEditing) {
            // Input field
            OutlinedTextField(
                value = inputKey,
                onValueChange = {
                    inputKey = it.trim()
                    testError = null
                    testResult = null
                },
                label = { Text("Paste your Gemini API Key") },
                placeholder = { Text("AIzaSy...", color = PurpleMutedText) },
                singleLine = true,
                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { }),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    inputKey = clip.trim()
                                    testError = null
                                    testResult = null
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = PurpleAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { isKeyVisible = !isKeyVisible },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isKeyVisible) "Hide Key" else "Show Key",
                                tint = PurpleMutedText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = PurpleSurfaceBorder,
                    focusedTextColor = PurpleLightText,
                    unfocusedTextColor = PurpleLightText,
                    focusedLabelColor = PurpleAccent,
                    unfocusedLabelColor = PurpleMutedText
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("gemini_api_key_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action row: Get Free Key + Test Key
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        uriHandler.openUri("https://aistudio.google.com/app/apikey")
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PurpleAccent,
                        containerColor = PurpleSurface.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PurpleSurfaceBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("get_free_gemini_key_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Get Free Key", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        if (inputKey.isBlank()) {
                            testError = "Please paste or enter your Gemini API key first"
                            testResult = null
                            return@OutlinedButton
                        }
                        isTesting = true
                        testError = null
                        testResult = null
                        coroutineScope.launch {
                            val res = GeminiApiKeyManager.testApiKey(inputKey)
                            isTesting = false
                            res.onSuccess { msg ->
                                testResult = msg
                            }.onFailure { err ->
                                testError = err.message ?: "Connection test failed"
                            }
                        }
                    },
                    enabled = !isTesting && inputKey.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PurpleAccent,
                        containerColor = PurpleSurface.copy(alpha = 0.5f),
                        disabledContentColor = PurpleMutedText
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (inputKey.isNotBlank()) PurpleAccent.copy(alpha = 0.6f) else PurpleSurfaceBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("test_input_key_btn")
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            color = PurpleAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Testing...", fontSize = 11.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Key", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save & Activate Button
            Button(
                onClick = {
                    if (inputKey.isBlank()) {
                        testError = "Please enter or paste your Gemini API key first"
                        return@Button
                    }
                    isTesting = true
                    testError = null
                    testResult = null
                    coroutineScope.launch {
                        val testRes = GeminiApiKeyManager.testApiKey(inputKey)
                        isTesting = false
                        testRes.onSuccess { msg ->
                            GeminiApiKeyManager.saveUserApiKey(context, inputKey)
                            refreshState()
                            isEditing = false
                            inputKey = ""
                            testResult = msg
                            onKeyUpdated?.invoke()
                            Toast.makeText(context, "Gemini API key saved & activated!", Toast.LENGTH_SHORT).show()
                        }.onFailure { err ->
                            testError = err.message ?: "Failed to verify API key"
                        }
                    }
                },
                enabled = !isTesting && inputKey.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurplePrimary,
                    disabledContainerColor = PurpleSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("save_gemini_api_key_btn")
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verifying & Saving...", fontSize = 12.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Activate Key", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isConfigured) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            isEditing = false
                            testResult = null
                            testError = null
                        }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Cancel editing",
                        color = PurpleMutedText,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            // Configured view: Clean status and prominent Test Connection button
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PurpleSurface)
                        .border(1.dp, PurpleSurfaceBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MetricStepsColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "Active Key: $maskedKey",
                                color = PurpleLightText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Saved securely on device • Never shared",
                                color = PurpleMutedText,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                isEditing = true
                                testResult = null
                                testError = null
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit API Key",
                                tint = PurpleAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                GeminiApiKeyManager.clearUserApiKey(context)
                                refreshState()
                                isEditing = true
                                testResult = null
                                testError = null
                                onKeyUpdated?.invoke()
                                Toast.makeText(context, "Gemini API key removed", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove Key",
                                tint = Color(0xFFFB7185),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Dedicated Test Connection Button - Full width, accessible height, clear styling
                OutlinedButton(
                    onClick = {
                        val activeKey = GeminiApiKeyManager.getActiveApiKey(context) ?: return@OutlinedButton
                        isTesting = true
                        testError = null
                        testResult = null
                        coroutineScope.launch {
                            val res = GeminiApiKeyManager.testApiKey(activeKey)
                            isTesting = false
                            res.onSuccess { msg ->
                                testResult = msg
                            }.onFailure { err ->
                                testError = err.message ?: "Connection check failed"
                            }
                        }
                    },
                    enabled = !isTesting,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PurpleAccent,
                        containerColor = PurpleSurface.copy(alpha = 0.6f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("test_gemini_connection_btn")
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            color = PurpleAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Testing Connection with Gemini...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Test Connection",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Test feedback banner: Success
        if (testResult != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF064E3B).copy(alpha = 0.5f))
                    .border(1.dp, Color(0xFF059669).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MetricStepsColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = testResult ?: "",
                    color = Color(0xFF6EE7B7),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { testResult = null },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFF6EE7B7),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Test feedback banner: Error
        if (testError != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF881337).copy(alpha = 0.5f))
                    .border(1.dp, Color(0xFFE11D48).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠️ ${testError ?: ""}",
                    color = Color(0xFFFECDD3),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { testError = null },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFFFB7185),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
