package com.wizzitdigital.emv.flutter

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/**
 * WizzitEmvFlutterPlugin
 *
 * Flutter plugin for Wizzit EMV NFC card payment processing.
 */
class WizzitEmvFlutterPlugin : FlutterPlugin, MethodCallHandler, ActivityAware,
    EventChannel.StreamHandler {

    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var eventSink: EventChannel.EventSink? = null

    private var context: Context? = null
    private var activity: Activity? = null

    private var emvHandler: EmvHandler? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext

        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "wizzit_emv_flutter")
        channel.setMethodCallHandler(this)

        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "wizzit_emv_flutter/events")
        eventChannel.setStreamHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "initialize" -> handleInitialize(call, result)
            "dispose" -> handleDispose(result)
            "checkDeviceRegistration" -> handleCheckDeviceRegistration(result)
            "registerDevice" -> handleRegisterDevice(call, result)
            "startTransaction" -> handleStartTransaction(call, result)
            "cancelTransaction" -> handleCancelTransaction(result)
            "voidTransaction" -> handleVoidTransaction(call, result)
            "generateQrPayment" -> handleGenerateQrPayment(call, result)
            "getLink2PaySession" -> handleGetLink2PaySession(call, result)
            "checkSupervisorPinRequired" -> handleCheckSupervisorPinRequired(result)
            "setSupervisorPin" -> handleSetSupervisorPin(call, result)
            "verifySupervisorPin" -> handleVerifySupervisorPin(call, result)
            "getTransactionHistory" -> handleGetTransactionHistory(call, result)
            "sendEmailReceipt" -> handleSendEmailReceipt(call, result)
            "sendSmsReceipt" -> handleSendSmsReceipt(call, result)
            "getDeviceInfo" -> handleGetDeviceInfo(result)
            "updateConfig" -> handleUpdateConfig(call, result)
            "isNfcAvailable" -> handleIsNfcAvailable(result)
            "isReady" -> handleIsReady(result)
            else -> result.notImplemented()
        }
    }

    private fun handleInitialize(call: MethodCall, result: Result) {
        val ctx = context ?: run {
            result.error("NO_CONTEXT", "Context not available", null)
            return
        }
        val act = activity ?: run {
            result.error("NO_ACTIVITY", "Activity not available", null)
            return
        }

        try {
            val region = call.argument<String>("region") ?: "euStaging"
            val baseUrl = call.argument<String>("baseUrl") ?: "https://staging.wizzitdigital.com"
            val authCredentials = call.argument<String>("authCredentials")
            val merchantName = call.argument<String>("merchantName")
            val enableAudio = call.argument<Boolean>("enableAudio") ?: true
            val enableVibration = call.argument<Boolean>("enableVibration") ?: true

            emvHandler = EmvHandler(ctx, act, object : EmvEventListener {
                override fun onEvent(event: String, message: String?, data: Map<String, Any?>?) {
                    sendEvent(event, message, data)
                }
            })

            emvHandler?.initialize(
                baseUrl = baseUrl,
                authCredentials = authCredentials,
                merchantName = merchantName,
                enableAudio = enableAudio,
                enableVibration = enableVibration,
                callback = { success, error ->
                    if (success) {
                        result.success(true)
                    } else {
                        result.error("INIT_FAILED", error ?: "Initialization failed", null)
                    }
                }
            )
        } catch (e: Exception) {
            result.error("INIT_ERROR", e.message, e.stackTraceToString())
        }
    }

    private fun handleDispose(result: Result) {
        try {
            emvHandler?.dispose()
            emvHandler = null
            result.success(null)
        } catch (e: Exception) {
            result.error("DISPOSE_ERROR", e.message, null)
        }
    }

    private fun handleCheckDeviceRegistration(result: Result) {
        val handler = emvHandler ?: run {
            result.error("NOT_INITIALIZED", "EMV adapter not initialized", null)
            return
        }

        handler.checkDeviceRegistration { response ->
            result.success(response)
        }
    }

    private fun handleRegisterDevice(call: MethodCall, result: Result) {
        val handler = emvHandler ?: run {
            result.error("NOT_INITIALIZED", "EMV adapter not initialized", null)
            return
        }

        val otp = call.argument<String>("otp") ?: run {
            result.error("MISSING_PARAM", "OTP is required", null)
            return
        }

        handler.registerDevice(otp) { success, error ->
            if (success) {
                result.success(true)
            } else {
                result.error("REGISTRATION_FAILED", error ?: "Registration failed", null)
            }
        }
    }

    private fun handleStartTransaction(call: MethodCall, result: Result) {
        val handler = emvHandler ?: run {
            result.error("NOT_INITIALIZED", "EMV adapter not initialized", null)
            return
        }

        val amount = call.argument<Int>("amount") ?: run {
            result.error("MISSING_PARAM", "Amount is required", null)
            return
        }

        val transactionType = call.argument<String>("transactionType") ?: "PURCHASE"
        val referenceId = call.argument<String>("referenceId")
        val acquireTip = call.argument<Boolean>("acquireTip") ?: false
        val tipAmount = call.argument<Int>("tipAmount")
        val currencyCode = call.argument<String>("currencyCode")
        val metadata = call.argument<Map<String, Any>>("metadata")

        handler.startTransaction(
            amount = amount,
            transactionType = transactionType,
            referenceId = referenceId,
            acquireTip = acquireTip,
            tipAmount = tipAmount,
            currencyCode = currencyCode,
            metadata = metadata
        ) { transactionResult ->
            result.success(transactionResult)
        }
    }

    private fun handleCancelTransaction(result: Result) {
        val handler = emvHandler ?: run {
            result.error("NOT_INITIALIZED", "EMV adapter not initialized", null)
            return
        }

        handler.cancelTransaction()
        result.success(null)
    }

    private fun handleVoidTransaction(call: MethodCall, result: Result) {
        val handler = emvHandler ?: run {
            result.error("NOT_INITIALIZED", "EMV adapter not initialized", null)
            return
        }

        val rrn = call.argument<String>("rrn") ?: run {
            result.error("MISSING_PARAM", "RRN is required", null)
            return
        }

        val authCode = call.argument<String>("authCode") ?: run {
            result.error("MISSING_PARAM", "Auth code is required", null)
            return
        }

        val supervisorPin = call.argument<String>("supervisorPin")

        handler.voidTransaction(rrn, authCode, supervisorPin) { response ->
            result.success(response)
        }
    }

    private fun handleGenerateQrPayment(call: MethodCall, result: Result) {
        val handler = emvHandler ?: run {
            result.error("NOT_INITIALIZED", "EMV adapter not initialized", null)
            return
        }

        val amount = call.argument<Int>("amount") ?: run {
            result.error("MISSING_PARAM", "Amount is required", null)
            return
        }

        val msisdn = call.argument<String>("msisdn")
        val ttlSeconds = call.argument<Int>("ttlSeconds") ?: 300

        handler.generateQrPayment(amount, msisdn, ttlSeconds) { response ->
            result.success(response)
        }
    }

    private fun handleGetLink2PaySession(call: MethodCall, result: Result) {
        val handler = emvHandler ?: run {
            result.error("NOT_INITIALIZED", "EMV adapter not initialized", null)
            return
        }

        val url = call.argument<String>("url") ?: run {
            result.error("MISSING_PARAM", "URL is required", null)
            return
        }

        handler.getLink2PaySession(url) { response ->
            result.success(response)
        }
    }

    private fun handleCheckSupervisorPinRequired(result: Result) {
        val handler = emvHandler ?: run {
            result.error("NOT_INITIALIZED", "EMV adapter not initialized", null)
            return
        }

        handler.checkSupervisorPinRequired { response ->
            result.success(response)
        }
    }

    private fun handleSetSupervisorPin(call: MethodCall, result: Result) {
        val handler = emvHandler ?: run {
            result.error("NOT_INITIALIZED", "EMV adapter not initialized", null)
            return
        }

        val pin = call.argument<String>("pin") ?: run {
            result.error("MISSING_PARAM", "PIN is required", null)
            return
        }

        handler.setSupervisorPin(pin) { response ->
            result.success(response)
        }
    }

    private fun handleVerifySupervisorPin(call: MethodCall, result: Result) {
        val handler = emvHandler ?: run {
            result.error("NOT_INITIALIZED", "EMV adapter not initialized", null)
            return
        }

        val pin = call.argument<String>("pin") ?: run {
            result.error("MISSING_PARAM", "PIN is required", null)
            return
        }

        handler.verifySupervisorPin(pin) { response ->
            result.success(response)
        }
    }

    private fun handleGetTransactionHistory(call: MethodCall, result: Result) {
        val handler = emvHandler ?: run {
            result.error("NOT_INITIALIZED", "EMV adapter not initialized", null)
            return
        }

        val startDate = call.argument<String>("startDate")
        val endDate = call.argument<String>("endDate")
        val limit = call.argument<Int>("limit") ?: 50
        val offset = call.argument<Int>("offset") ?: 0

        handler.getTransactionHistory(startDate, endDate, limit, offset) { response ->
            result.success(response)
        }
    }

    private fun handleSendEmailReceipt(call: MethodCall, result: Result) {
        val handler = emvHandler ?: run {
            result.error("NOT_INITIALIZED", "EMV adapter not initialized", null)
            return
        }

        val email = call.argument<String>("email") ?: run {
            result.error("MISSING_PARAM", "Email is required", null)
            return
        }

        val transactionId = call.argument<String>("transactionId") ?: run {
            result.error("MISSING_PARAM", "Transaction ID is required", null)
            return
        }

        handler.sendEmailReceipt(email, transactionId) { success, error ->
            if (success) {
                result.success(true)
            } else {
                result.error("EMAIL_FAILED", error ?: "Failed to send email", null)
            }
        }
    }

    private fun handleSendSmsReceipt(call: MethodCall, result: Result) {
        val handler = emvHandler ?: run {
            result.error("NOT_INITIALIZED", "EMV adapter not initialized", null)
            return
        }

        val phoneNumber = call.argument<String>("phoneNumber") ?: run {
            result.error("MISSING_PARAM", "Phone number is required", null)
            return
        }

        val transactionId = call.argument<String>("transactionId") ?: run {
            result.error("MISSING_PARAM", "Transaction ID is required", null)
            return
        }

        handler.sendSmsReceipt(phoneNumber, transactionId) { success, error ->
            if (success) {
                result.success(true)
            } else {
                result.error("SMS_FAILED", error ?: "Failed to send SMS", null)
            }
        }
    }

    private fun handleGetDeviceInfo(result: Result) {
        val handler = emvHandler
        val ctx = context

        val deviceInfo = mutableMapOf<String, Any?>()

        if (ctx != null) {
            deviceInfo["deviceId"] = handler?.getDeviceId() ?: android.provider.Settings.Secure.getString(
                ctx.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )
            try {
                val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                deviceInfo["appVersion"] = packageInfo.versionName
                deviceInfo["appVersionCode"] = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        deviceInfo["sdkVersion"] = handler?.getSdkVersion()
        deviceInfo["isInitialized"] = handler != null

        result.success(deviceInfo)
    }

    private fun handleUpdateConfig(call: MethodCall, result: Result) {
        val handler = emvHandler ?: run {
            result.error("NOT_INITIALIZED", "EMV adapter not initialized", null)
            return
        }

        val baseUrl = call.argument<String>("baseUrl")
        val authCredentials = call.argument<String>("authCredentials")
        val merchantName = call.argument<String>("merchantName")
        val enableAudio = call.argument<Boolean>("enableAudio")
        val enableVibration = call.argument<Boolean>("enableVibration")

        handler.updateConfig(
            baseUrl = baseUrl,
            authCredentials = authCredentials,
            merchantName = merchantName,
            enableAudio = enableAudio,
            enableVibration = enableVibration
        ) { success, error ->
            if (success) {
                result.success(true)
            } else {
                result.error("CONFIG_UPDATE_FAILED", error ?: "Failed to update config", null)
            }
        }
    }

    private fun handleIsNfcAvailable(result: Result) {
        val ctx = context ?: run {
            result.success(false)
            return
        }

        val nfcAdapter = NfcAdapter.getDefaultAdapter(ctx)
        result.success(nfcAdapter != null && nfcAdapter.isEnabled)
    }

    private fun handleIsReady(result: Result) {
        result.success(emvHandler?.isReady() ?: false)
    }

    private fun sendEvent(event: String, message: String?, data: Map<String, Any?>?) {
        activity?.runOnUiThread {
            eventSink?.success(mapOf(
                "event" to event,
                "message" to message,
                "data" to data
            ))
        }
    }

    // EventChannel.StreamHandler implementation

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    // ActivityAware implementation

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        emvHandler?.setActivity(binding.activity)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        emvHandler?.setActivity(binding.activity)
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
        emvHandler?.dispose()
        emvHandler = null
    }
}

/**
 * Interface for receiving EMV events
 */
interface EmvEventListener {
    fun onEvent(event: String, message: String?, data: Map<String, Any?>?)
}
