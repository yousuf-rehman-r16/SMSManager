package com.example.smsmanager

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import android.content.SharedPreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPreferences.init(this)
        setContent {
            AppUI()
        }
        val smsReceiver = SMSReceiver()
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        registerReceiver(smsReceiver, filter)
    }
}

@Composable
fun AppUI() {
    var botToken by remember { mutableStateOf(TextFieldValue(AppPreferences.botToken)) }
    var chatId by remember { mutableStateOf(TextFieldValue(AppPreferences.chatId)) }
    var notifyCalls by remember { mutableStateOf(AppPreferences.notifyCalls) }
    var notifySms by remember { mutableStateOf(AppPreferences.notifySms) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Notify Abroad", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Telegram bot token")
        BasicTextField(
            value = botToken,
            onValueChange = { botToken = it },
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(8.dp)) { innerTextField() }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Telegram Chat ID")
        BasicTextField(
            value = chatId,
            onValueChange = { chatId = it },
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(8.dp)) { innerTextField() }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Categories of notifications")
        Row {
            Checkbox(checked = notifyCalls, onCheckedChange = {
                notifyCalls = it
                AppPreferences.notifyCalls = it
            })
            Text(text = "Notify Calls")
        }

        Row {
            Checkbox(checked = notifySms, onCheckedChange = {
                notifySms = it
                AppPreferences.notifySms = it
            })
            Text(text = "Notify SMS")
        }

        Button(onClick = {
            AppPreferences.botToken = botToken.text
            AppPreferences.chatId = chatId.text
        }) {
            Text("Save Settings")
        }
        Button(onClick = { /* Show how-to */ }) {
            Text("How to Use?")
        }
    }
}

fun saveSettings(botToken: String, chatId: String, notifyCalls: Boolean, notifySms: Boolean) {
    AppPreferences.botToken = botToken
    AppPreferences.chatId = chatId
    AppPreferences.notifyCalls = notifyCalls
    AppPreferences.notifySms = notifySms
}

class SMSReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.displayOriginatingAddress
                val body = sms.messageBody
                val simNumber = sms.serviceCenterAddress ?: "Unknown SIM"

                val formattedMessage = """
                    [$simNumber]
                    [$sender]
                    [$body]
                """.trimIndent()

                CoroutineScope(Dispatchers.IO).launch {
                    sendToTelegram(formattedMessage)
                }
            }
        }
    }
}

fun sendToTelegram(message: String) {
    val botToken = AppPreferences.botToken
    val chatId = AppPreferences.chatId
    if (botToken.isEmpty() || chatId.isEmpty()) return

    val url = "https://api.telegram.org/bot$botToken/sendMessage?chat_id=$chatId&text=$message"

    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()
    val response: Response = client.newCall(request).execute()
    response.close()
}

object AppPreferences {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }

    var botToken: String
        get() = prefs.getString("bot_token", "") ?: ""
        set(value) = prefs.edit().putString("bot_token", value).apply()

    var chatId: String
        get() = prefs.getString("chat_id", "") ?: ""
        set(value) = prefs.edit().putString("chat_id", value).apply()

    var notifyCalls: Boolean
        get() = prefs.getBoolean("notify_calls", false)
        set(value) = prefs.edit().putBoolean("notify_calls", value).apply()

    var notifySms: Boolean
        get() = prefs.getBoolean("notify_sms", false)
        set(value) = prefs.edit().putBoolean("notify_sms", value).apply()
}