package com.wizzitdigital.emv.sdk.terminology

import io.flutter.embedding.android.FlutterActivity
import com.wizzitdigital.emv.sdk.EMVAdapterListener

class MainActivity: FlutterActivity(), EMVAdapterListener {
    // EMVAdapterListener implementation - delegated to plugin
    override fun onAdapterInitComplete(success: Boolean, message: String) {}
    override fun onAdapterInitializing() {}
    override fun onCardProcessing() {}
    override fun onCardProcessingComplete() {}
    override fun onCardProcessingNotify(message: String) {}
    override fun onCardRemoved() {}
    override fun onCheckDeviceRegistrationComplete(
        success: Boolean,
        message: String,
        merchantId: String,
        terminalId: String,
        extra1: String,
        extra2: String
    ) {}
    override fun onDeviceRegistrationComplete(success: Boolean, message: String) {}
    override fun onDeviceUnRegistrationComplete(success: Boolean, message: String) {}
    override fun onSessionComplete(
        success: Boolean,
        message: String,
        rrn: String,
        data: Map<String, String>
    ) {}
    override fun onSessionCountdown(seconds: Int) {}
    override fun onSessionInitComplete(success: Boolean, message: String) {}
    override fun onSessionTimeout() {}
}
