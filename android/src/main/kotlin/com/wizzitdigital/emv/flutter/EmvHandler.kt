package com.wizzitdigital.emv.flutter

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wizzitdigital.emv.sdk.EMVAdapter
import com.wizzitdigital.emv.sdk.EMVAdapterListener
import com.wizzitdigital.emv.sdk.EMVCfgVars
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * Handler class that bridges Flutter calls to the native EMV SDK
 */
class EmvHandler(
    private val context: Context,
    private var activity: Activity?,
    private val eventListener: EmvEventListener
) : EMVAdapterListener {

    private var emvAdapter: EMVAdapter? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var baseUrl: String = "https://staging.wizzitdigital.com"
    private var authCredentials: String? = null
    private var merchantName: String? = null
    private var enableAudio: Boolean = true
    private var enableVibration: Boolean = true

    private var isInitialized: Boolean = false
    private var isDeviceRegistered: Boolean = false
    private var pendingTransactionCallback: ((Map<String, Any?>) -> Unit)? = null
    private var pendingInitCallback: ((Boolean, String?) -> Unit)? = null
    private var initTimeoutHandler: Handler? = null
    private val INIT_TIMEOUT_MS = 30000L // 30 seconds timeout

    private val httpClient: HttpClient by lazy { HttpClient(baseUrl, authCredentials) }

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "wizzit_emv_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun setActivity(activity: Activity) {
        this.activity = activity
    }

    fun initialize(
        baseUrl: String,
        authCredentials: String?,
        merchantName: String?,
        enableAudio: Boolean,
        enableVibration: Boolean,
        callback: (Boolean, String?) -> Unit
    ) {
        this.baseUrl = baseUrl
        this.authCredentials = authCredentials
        this.merchantName = merchantName
        this.enableAudio = enableAudio
        this.enableVibration = enableVibration

        // Store callback to be called when initialization completes
        pendingInitCallback = callback

        // Run on main thread like native app does
        mainHandler.post {
            try {
                val act = activity ?: throw IllegalStateException("Activity not available")

                // Create adapter like native app does - pass `this` as listener
                emvAdapter = EMVAdapter(act, this)

                // Load configuration from encrypted preferences
                loadConfig()

                emvAdapter?.let { adapter ->
                    // Set configs like native app does (in onCreate before checkDeviceRegistration)
                    android.util.Log.d("EmvHandler", "Setting EMVCfgVars configs like native app")
                    try {
                        adapter.setConfig(EMVCfgVars.PIN_REQUIREMENT, 1)
                        adapter.setConfig(EMVCfgVars.SIGNATURE_FLAG, 1)
                        adapter.setConfig(EMVCfgVars.CURRENCY_EXPONENT, 2)
                        adapter.setConfig(EMVCfgVars.CURRENCY_CODE, "0840")
                        adapter.setConfig(EMVCfgVars.COUNTRY_CODE, "0840")
                        adapter.setConfig(EMVCfgVars.READER_LIMIT, 500.00)
                        adapter.setConfig(EMVCfgVars.TX_TYPE, "00")
                        adapter.setConfig(EMVCfgVars.OVERRIDE_MASTERCARD_PROFILE_CONFIG, true)
                        adapter.setConfig(EMVCfgVars.REQUEST_TIMEOUT, 25)
                    } catch (configError: Exception) {
                        android.util.Log.w("EmvHandler", "setConfig failed: ${configError.message}")
                    }
                    
                    // Call checkDeviceRegistration FIRST (like native app does)
                    android.util.Log.d("EmvHandler", "Calling checkDeviceRegistration on main thread")
                    adapter.checkDeviceRegistration()
                    // Don't call callback here - wait for onCheckDeviceRegistrationComplete
                } ?: run {
                    pendingInitCallback = null
                    eventListener.onEvent("adapterInitializationFailed", "Failed to create EMV adapter", null)
                    callback(false, "Failed to create EMV adapter")
                }
            } catch (e: Exception) {
                pendingInitCallback = null
                eventListener.onEvent("adapterInitializationFailed", e.message, null)
                callback(false, e.message)
            }
        }
    }

    fun dispose() {
        try {
            initTimeoutHandler?.removeCallbacksAndMessages(null)
            initTimeoutHandler = null
            emvAdapter?.let { adapter ->
                adapter.cancelSession()
            }
            emvAdapter = null
            isInitialized = false
            isDeviceRegistered = false
            pendingInitCallback = null
        } catch (e: Exception) {
            // Ignore disposal errors
        }
    }

    fun isReady(): Boolean = isInitialized && emvAdapter != null

    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun getSdkVersion(): String? {
        return try {
            emvAdapter?.let { "1.7.3" } // Version from the AAR filename
        } catch (e: Exception) {
            null
        }
    }

    private var pendingRegistrationCheckCallback: ((Map<String, Any?>) -> Unit)? = null
    private var pendingRegisterCallback: ((Boolean, String?) -> Unit)? = null

    fun checkDeviceRegistration(callback: (Map<String, Any?>) -> Unit) {
        val adapter = emvAdapter
        if (adapter == null) {
            mainHandler.post {
                callback(mapOf(
                    "isRegistered" to false,
                    "error" to "EMV adapter not initialized"
                ))
            }
            return
        }

        // Store callback to be invoked when onCheckDeviceRegistrationComplete is called
        pendingRegistrationCheckCallback = callback
        
        // Use SDK's built-in method - this may show SDK's own UI
        try {
            adapter.checkDeviceRegistration()
        } catch (e: Exception) {
            pendingRegistrationCheckCallback = null
            mainHandler.post {
                callback(mapOf(
                    "isRegistered" to false,
                    "error" to e.message
                ))
            }
        }
    }

    fun registerDevice(otp: String, callback: (Boolean, String?) -> Unit) {
        val adapter = emvAdapter
        if (adapter == null) {
            mainHandler.post {
                callback(false, "EMV adapter not initialized")
            }
            return
        }

        // Store callback to be invoked when onDeviceRegistrationComplete is called
        pendingRegisterCallback = callback
        
        // Use SDK's built-in method - like native app: registerDevice(otp, null, androidId)
        try {
            val deviceId = getDeviceId()
            android.util.Log.d("EmvHandler", "Calling registerDevice with OTP=$otp, deviceId=$deviceId")
            adapter.registerDevice(otp, null, deviceId)
        } catch (e: Exception) {
            android.util.Log.e("EmvHandler", "registerDevice failed: ${e.message}")
            pendingRegisterCallback = null
            eventListener.onEvent("otpFailed", e.message, null)
            mainHandler.post { callback(false, e.message) }
        }
    }

    fun startTransaction(
        amount: Int,
        transactionType: String,
        referenceId: String?,
        acquireTip: Boolean,
        tipAmount: Int?,
        currencyCode: String?,
        metadata: Map<String, Any>?,
        callback: (Map<String, Any?>) -> Unit
    ) {
        pendingTransactionCallback = callback

        executor.execute {
            try {
                val adapter = emvAdapter ?: throw IllegalStateException("EMV adapter not initialized")

                eventListener.onEvent("sessionInitialized", "Ready for card tap", mapOf(
                    "amount" to amount,
                    "transactionType" to transactionType
                ))

                // Start the session with required parameters
                // initSession(transactionType: String, currencyCode: String, amount: Int)
                val currency = currencyCode ?: "ZAR"
                adapter.initSession(transactionType, currency, amount)

            } catch (e: Exception) {
                eventListener.onEvent("transactionError", e.message, null)
                mainHandler.post {
                    callback(mapOf(
                        "isSuccessful" to false,
                        "reason" to e.message
                    ))
                }
                pendingTransactionCallback = null
            }
        }
    }

    fun cancelTransaction() {
        try {
            emvAdapter?.cancelSession()
            eventListener.onEvent("transactionCancelled", "Transaction cancelled by user", null)
        } catch (e: Exception) {
            // Ignore cancellation errors
        }
        pendingTransactionCallback = null
    }

    fun voidTransaction(
        rrn: String,
        authCode: String,
        supervisorPin: String?,
        callback: (Map<String, Any?>) -> Unit
    ) {
        executor.execute {
            try {
                val deviceId = getDeviceId()
                val params = mutableMapOf<String, Any>(
                    "device_id" to deviceId,
                    "rrn" to rrn,
                    "auth_code" to authCode
                )
                supervisorPin?.let { params["supervisor_pin"] = it }

                val response = httpClient.post("/api/v1/void_transaction", params)

                val result = mutableMapOf<String, Any?>()

                if (response.isSuccessful && response.body != null) {
                    val json = JSONObject(response.body!!)
                    result["isSuccessful"] = json.optBoolean("success", false)
                    result["authCode"] = json.optString("auth_code", null)
                    result["description"] = json.optString("description", null)
                } else {
                    result["isSuccessful"] = false
                    result["error"] = response.error ?: "Void transaction failed"
                }

                mainHandler.post { callback(result) }
            } catch (e: Exception) {
                mainHandler.post {
                    callback(mapOf(
                        "isSuccessful" to false,
                        "error" to e.message
                    ))
                }
            }
        }
    }

    fun generateQrPayment(
        amount: Int,
        msisdn: String?,
        ttlSeconds: Int,
        callback: (Map<String, Any?>) -> Unit
    ) {
        executor.execute {
            try {
                val deviceId = getDeviceId()
                val params = mutableMapOf<String, Any>(
                    "device_id" to deviceId,
                    "amount" to amount,
                    "ttl" to ttlSeconds
                )
                msisdn?.let { params["msisdn"] = it }

                val response = httpClient.post("/api/v1/session_from_device", params)

                val result = mutableMapOf<String, Any?>()

                if (response.isSuccessful && response.body != null) {
                    val json = JSONObject(response.body!!)
                    result["isSuccessful"] = true
                    result["paymentUrl"] = json.optString("url", null)
                    result["sessionId"] = json.optString("session_id", null)
                } else {
                    result["isSuccessful"] = false
                    result["error"] = response.error ?: "Failed to generate QR payment"
                }

                mainHandler.post { callback(result) }
            } catch (e: Exception) {
                mainHandler.post {
                    callback(mapOf(
                        "isSuccessful" to false,
                        "error" to e.message
                    ))
                }
            }
        }
    }

    fun getLink2PaySession(url: String, callback: (Map<String, Any?>) -> Unit) {
        executor.execute {
            try {
                // Extract session ID from URL
                val sessionPath = url.substringAfter("/api/v1/cp_transact/", "")
                if (sessionPath.isEmpty()) {
                    mainHandler.post {
                        callback(mapOf(
                            "isSuccessful" to false,
                            "error" to "Invalid Link2Pay URL"
                        ))
                    }
                    return@execute
                }

                val response = httpClient.get("/api/v1/cp_transact/$sessionPath")

                val result = mutableMapOf<String, Any?>()

                if (response.isSuccessful && response.body != null) {
                    val json = JSONObject(response.body!!)
                    result["isSuccessful"] = true
                    result["amount"] = json.optInt("amount", 0)
                    result["referenceId"] = json.optString("ref_id", null)
                    result["merchantId"] = json.optString("merchant_id", null)

                    if (json.has("meta")) {
                        val meta = json.getJSONObject("meta")
                        val metaMap = mutableMapOf<String, Any>()
                        meta.keys().forEach { key ->
                            metaMap[key] = meta.get(key)
                        }
                        result["metadata"] = metaMap
                    }
                } else {
                    result["isSuccessful"] = false
                    result["error"] = response.error ?: "Failed to get session"
                }

                mainHandler.post { callback(result) }
            } catch (e: Exception) {
                mainHandler.post {
                    callback(mapOf(
                        "isSuccessful" to false,
                        "error" to e.message
                    ))
                }
            }
        }
    }

    fun checkSupervisorPinRequired(callback: (Map<String, Any?>) -> Unit) {
        executor.execute {
            try {
                val deviceId = getDeviceId()
                val response = httpClient.get("/api/v1/supervisor_pin/requires?device_id=$deviceId")

                val result = mutableMapOf<String, Any?>()

                if (response.isSuccessful && response.body != null) {
                    val json = JSONObject(response.body!!)
                    result["isSuccessful"] = true
                    result["pinRequired"] = json.optBoolean("requires_pin", false)
                } else {
                    result["isSuccessful"] = false
                    result["error"] = response.error
                }

                mainHandler.post { callback(result) }
            } catch (e: Exception) {
                mainHandler.post {
                    callback(mapOf(
                        "isSuccessful" to false,
                        "error" to e.message
                    ))
                }
            }
        }
    }

    fun setSupervisorPin(pin: String, callback: (Map<String, Any?>) -> Unit) {
        executor.execute {
            try {
                val deviceId = getDeviceId()
                val response = httpClient.post(
                    "/api/v1/supervisor_pin/insert",
                    mapOf(
                        "device_id" to deviceId,
                        "pin" to pin
                    )
                )

                val result = mutableMapOf<String, Any?>()

                if (response.isSuccessful && response.body != null) {
                    val json = JSONObject(response.body!!)
                    result["isSuccessful"] = json.optBoolean("success", false)
                } else {
                    result["isSuccessful"] = false
                    result["error"] = response.error
                }

                mainHandler.post { callback(result) }
            } catch (e: Exception) {
                mainHandler.post {
                    callback(mapOf(
                        "isSuccessful" to false,
                        "error" to e.message
                    ))
                }
            }
        }
    }

    fun verifySupervisorPin(pin: String, callback: (Map<String, Any?>) -> Unit) {
        executor.execute {
            try {
                val deviceId = getDeviceId()
                val response = httpClient.post(
                    "/api/v1/supervisor_pin/verify",
                    mapOf(
                        "device_id" to deviceId,
                        "pin" to pin
                    )
                )

                val result = mutableMapOf<String, Any?>()

                if (response.isSuccessful && response.body != null) {
                    val json = JSONObject(response.body!!)
                    result["isSuccessful"] = true
                    result["pinVerified"] = json.optBoolean("verified", false)
                } else {
                    result["isSuccessful"] = false
                    result["error"] = response.error
                }

                mainHandler.post { callback(result) }
            } catch (e: Exception) {
                mainHandler.post {
                    callback(mapOf(
                        "isSuccessful" to false,
                        "error" to e.message
                    ))
                }
            }
        }
    }

    fun getTransactionHistory(
        startDate: String?,
        endDate: String?,
        limit: Int,
        offset: Int,
        callback: (List<Map<String, Any?>>) -> Unit
    ) {
        executor.execute {
            try {
                val deviceId = getDeviceId()
                val params = StringBuilder("?device_id=$deviceId&limit=$limit&offset=$offset")
                startDate?.let { params.append("&start_date=$it") }
                endDate?.let { params.append("&end_date=$it") }

                val response = httpClient.get("/api/v1/reports/tx_log$params")

                if (response.isSuccessful && response.body != null) {
                    val json = JSONObject(response.body!!)
                    val transactions = json.optJSONArray("transactions")
                    val result = mutableListOf<Map<String, Any?>>()

                    transactions?.let {
                        for (i in 0 until it.length()) {
                            val tx = it.getJSONObject(i)
                            result.add(mapOf(
                                "transactionId" to tx.optString("id", null),
                                "dateTime" to tx.optString("date_time", null),
                                "amount" to tx.optInt("amount", 0),
                                "tipAmount" to tx.optInt("tip_amount", 0),
                                "currencyCode" to tx.optString("currency", null),
                                "transactionType" to tx.optString("type", null),
                                "status" to tx.optString("status", null),
                                "rrn" to tx.optString("rrn", null),
                                "authCode" to tx.optString("auth_code", null),
                                "maskedPan" to tx.optString("masked_pan", null),
                                "cardScheme" to tx.optString("card_scheme", null),
                                "merchantId" to tx.optString("merchant_id", null),
                                "terminalId" to tx.optString("terminal_id", null),
                                "referenceId" to tx.optString("reference_id", null)
                            ))
                        }
                    }

                    mainHandler.post { callback(result) }
                } else {
                    mainHandler.post { callback(emptyList()) }
                }
            } catch (e: Exception) {
                mainHandler.post { callback(emptyList()) }
            }
        }
    }

    fun sendEmailReceipt(email: String, transactionId: String, callback: (Boolean, String?) -> Unit) {
        executor.execute {
            try {
                val deviceId = getDeviceId()
                val response = httpClient.post(
                    "/api/v1/mail_from_device",
                    mapOf(
                        "device_id" to deviceId,
                        "transaction_id" to transactionId,
                        "email" to email
                    )
                )

                if (response.isSuccessful) {
                    mainHandler.post { callback(true, null) }
                } else {
                    mainHandler.post { callback(false, response.error) }
                }
            } catch (e: Exception) {
                mainHandler.post { callback(false, e.message) }
            }
        }
    }

    fun sendSmsReceipt(phoneNumber: String, transactionId: String, callback: (Boolean, String?) -> Unit) {
        executor.execute {
            try {
                val deviceId = getDeviceId()
                val response = httpClient.post(
                    "/api/v1/submitSMS",
                    mapOf(
                        "device_id" to deviceId,
                        "transaction_id" to transactionId,
                        "msisdn" to phoneNumber
                    )
                )

                if (response.isSuccessful) {
                    mainHandler.post { callback(true, null) }
                } else {
                    mainHandler.post { callback(false, response.error) }
                }
            } catch (e: Exception) {
                mainHandler.post { callback(false, e.message) }
            }
        }
    }

    fun updateConfig(
        baseUrl: String?,
        authCredentials: String?,
        merchantName: String?,
        enableAudio: Boolean?,
        enableVibration: Boolean?,
        callback: (Boolean, String?) -> Unit
    ) {
        try {
            baseUrl?.let { this.baseUrl = it }
            authCredentials?.let { this.authCredentials = it }
            merchantName?.let { this.merchantName = it }
            enableAudio?.let { this.enableAudio = it }
            enableVibration?.let { this.enableVibration = it }

            saveConfig()
            callback(true, null)
        } catch (e: Exception) {
            callback(false, e.message)
        }
    }

    private fun loadConfig() {
        encryptedPrefs.getString("baseUrl", null)?.let { baseUrl = it }
        encryptedPrefs.getString("authCredentials", null)?.let { authCredentials = it }
        encryptedPrefs.getString("merchantName", null)?.let { merchantName = it }
        enableAudio = encryptedPrefs.getBoolean("enableAudio", true)
        enableVibration = encryptedPrefs.getBoolean("enableVibration", true)
    }

    private fun saveConfig() {
        encryptedPrefs.edit().apply {
            putString("baseUrl", baseUrl)
            authCredentials?.let { putString("authCredentials", it) }
            merchantName?.let { putString("merchantName", it) }
            putBoolean("enableAudio", enableAudio)
            putBoolean("enableVibration", enableVibration)
            apply()
        }
    }

    // EMVAdapterListener implementation
    // Method signatures match the actual SDK interface

    override fun onAdapterInitComplete(success: Boolean, message: String) {
        android.util.Log.d("EmvHandler", "onAdapterInitComplete: success=$success, message=$message, pendingInitCallback=${pendingInitCallback != null}")
        
        // Cancel timeout
        initTimeoutHandler?.removeCallbacksAndMessages(null)
        initTimeoutHandler = null
        
        isInitialized = success
        if (success) {
            eventListener.onEvent("adapterInitialized", message, null)
        } else {
            eventListener.onEvent("adapterInitializationFailed", message, null)
        }
        
        // Invoke the pending init callback
        pendingInitCallback?.let { callback ->
            android.util.Log.d("EmvHandler", "Calling pendingInitCallback with success=$success")
            mainHandler.post {
                callback(success, if (success) null else message)
            }
        } ?: run {
            android.util.Log.w("EmvHandler", "onAdapterInitComplete called but pendingInitCallback is null!")
        }
        pendingInitCallback = null
    }

    override fun onAdapterInitializing() {
        android.util.Log.d("EmvHandler", "onAdapterInitializing called. pendingInitCallback=${pendingInitCallback != null}")
        eventListener.onEvent("adapterInitializing", "EMV Adapter initializing", null)
    }

    override fun onCardProcessing() {
        eventListener.onEvent("cardDetected", "Card detected, processing", null)
    }

    override fun onCardProcessingComplete() {
        eventListener.onEvent("cardReadComplete", "Card processing complete", null)
    }

    override fun onCardProcessingNotify(message: String) {
        eventListener.onEvent("processingTransaction", message, null)
    }

    override fun onCardRemoved() {
        eventListener.onEvent("cardRemoved", "Card removed", null)
    }

    override fun onCheckDeviceRegistrationComplete(
        success: Boolean,
        message: String,
        merchantId: String,
        terminalId: String,
        extra1: String,
        extra2: String
    ) {
        android.util.Log.d("EmvHandler", "onCheckDeviceRegistrationComplete: success=$success, message=$message, merchantId=$merchantId, terminalId=$terminalId, isDeviceRegistered=$isDeviceRegistered")
        
        val result = mapOf<String, Any?>(
            "isRegistered" to success,
            "message" to message,
            "merchantId" to merchantId,
            "terminalId" to terminalId,
            "merchantName" to extra1,
            "deviceId" to getDeviceId()
        )
        
        if (success) {
            // Device is registered - mark flag and proceed with initAdapter
            isDeviceRegistered = true
            eventListener.onEvent("deviceRegistered", message, result)
            
            emvAdapter?.let { adapter ->
                val isMainThread = Looper.getMainLooper().thread == Thread.currentThread()
                android.util.Log.d("EmvHandler", "Device registered, calling initAdapter. pendingInitCallback=${pendingInitCallback != null}, isMainThread=$isMainThread")
                
                // Set up timeout in case onAdapterInitComplete never fires
                initTimeoutHandler?.removeCallbacksAndMessages(null)
                initTimeoutHandler = Handler(Looper.getMainLooper())
                initTimeoutHandler?.postDelayed({
                    if (pendingInitCallback != null && !isInitialized) {
                        android.util.Log.w("EmvHandler", "initAdapter timeout - onAdapterInitComplete never called")
                        pendingInitCallback?.let { callback ->
                            callback(false, "Initialization timeout - adapter did not complete initialization")
                        }
                        pendingInitCallback = null
                    }
                }, INIT_TIMEOUT_MS)
                
                // Always call initAdapter on main thread - SDK callbacks may come from background threads
                if (isMainThread) {
                    try {
                        adapter.initAdapter()
                        android.util.Log.d("EmvHandler", "initAdapter() called successfully on main thread")
                    } catch (e: Exception) {
                        android.util.Log.e("EmvHandler", "initAdapter() failed: ${e.message}", e)
                        initTimeoutHandler?.removeCallbacksAndMessages(null)
                        pendingInitCallback?.let { callback ->
                            callback(false, "initAdapter failed: ${e.message}")
                        }
                        pendingInitCallback = null
                    }
                } else {
                    // Post to main thread
                    mainHandler.post {
                        try {
                            adapter.initAdapter()
                            android.util.Log.d("EmvHandler", "initAdapter() called successfully on main thread (posted)")
                        } catch (e: Exception) {
                            android.util.Log.e("EmvHandler", "initAdapter() failed: ${e.message}", e)
                            initTimeoutHandler?.removeCallbacksAndMessages(null)
                            pendingInitCallback?.let { callback ->
                                callback(false, "initAdapter failed: ${e.message}")
                            }
                            pendingInitCallback = null
                        }
                    }
                }
            }
        } else if (isDeviceRegistered) {
            // Already confirmed registered - ignore this spurious false callback
            // This can happen when initAdapter() internally re-triggers checkDeviceRegistration
            android.util.Log.d("EmvHandler", "Ignoring false callback - device already confirmed registered. pendingInitCallback=${pendingInitCallback != null}")
            // DO NOT clear pendingInitCallback here - it's waiting for onAdapterInitComplete
        } else {
            // Device NOT registered - emit otpRequired event
            // The FLUTTER APP must show its own OTP input UI (like native app does)
            android.util.Log.d("EmvHandler", "Device NOT registered - emitting otpRequired event")
            eventListener.onEvent("otpRequired", "Please enter OTP to register device", result)
            
            // Let the Flutter app know initialization is "paused" waiting for OTP
            pendingInitCallback?.let { callback ->
                mainHandler.post {
                    callback(false, "OTP_REQUIRED")
                }
            }
            pendingInitCallback = null
        }
        
        // Invoke pending registration check callback if any
        pendingRegistrationCheckCallback?.let { callback ->
            mainHandler.post { callback(result) }
        }
        pendingRegistrationCheckCallback = null
    }

    override fun onDeviceRegistrationComplete(success: Boolean, message: String) {
        android.util.Log.d("EmvHandler", "onDeviceRegistrationComplete: success=$success, message=$message")
        
        eventListener.onEvent(
            if (success) "otpVerified" else "otpFailed",
            message,
            mapOf<String, Any>("success" to success)
        )
        
        if (success) {
            // Registration successful - call checkDeviceRegistration again (like native app)
            // This will trigger onCheckDeviceRegistrationComplete which will then call initAdapter
            android.util.Log.d("EmvHandler", "Registration successful, calling checkDeviceRegistration again")
            emvAdapter?.checkDeviceRegistration()
        }
        
        // Invoke pending callback
        pendingRegisterCallback?.let { callback ->
            mainHandler.post { callback(success, if (success) null else message) }
        }
        pendingRegisterCallback = null
    }

    override fun onDeviceUnRegistrationComplete(success: Boolean, message: String) {
        eventListener.onEvent(
            "deviceUnregistered",
            message,
            mapOf<String, Any>("success" to success)
        )
    }

    override fun onSessionComplete(
        success: Boolean,
        message: String,
        rrn: String,
        data: Map<String, String>
    ) {
        @Suppress("UNCHECKED_CAST")
        val transactionResult = mutableMapOf<String, Any>(
            "isSuccessful" to success,
            "reason" to message,
            "rrn" to rrn
        )
        
        transactionResult.putAll(data)

        val eventName = if (success) "transactionApproved" else "transactionDeclined"
        eventListener.onEvent(eventName, message, transactionResult)

        pendingTransactionCallback?.let { callback ->
            mainHandler.post { callback(transactionResult) }
        }
        pendingTransactionCallback = null
    }

    override fun onSessionCountdown(seconds: Int) {
        eventListener.onEvent("sessionCountdown", "Session countdown: $seconds", mapOf<String, Any>("seconds" to seconds))
    }

    override fun onSessionInitComplete(success: Boolean, message: String) {
        if (success) {
            eventListener.onEvent("sessionInitialized", "Ready for card tap", null)
        } else {
            eventListener.onEvent("sessionInitFailed", message, null)
            pendingTransactionCallback?.let { callback ->
                mainHandler.post {
                    callback(mapOf<String, Any>(
                        "isSuccessful" to false,
                        "reason" to message
                    ))
                }
            }
            pendingTransactionCallback = null
        }
    }

    override fun onSessionTimeout() {
        eventListener.onEvent("sessionTimeout", "Session timed out", null)
        pendingTransactionCallback?.let { callback ->
            mainHandler.post {
                callback(mapOf<String, Any>(
                    "isSuccessful" to false,
                    "reason" to "Session timed out"
                ))
            }
        }
        pendingTransactionCallback = null
    }
}
