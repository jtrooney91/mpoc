/// Result of EMV adapter initialization
class InitializeResult {
  /// Whether the initialization was successful
  final bool isInitialized;

  /// Whether the device is registered with the payment processor
  final bool isRegistered;

  /// Error message if initialization failed
  final String? errorMessage;

  /// Merchant ID if registered
  final String? merchantId;

  /// Terminal ID if registered
  final String? terminalId;

  /// Device ID
  final String? deviceId;

  const InitializeResult({
    required this.isInitialized,
    required this.isRegistered,
    this.errorMessage,
    this.merchantId,
    this.terminalId,
    this.deviceId,
  });

  /// Create from map returned by native code
  factory InitializeResult.fromMap(Map<String, dynamic> map) {
    return InitializeResult(
      isInitialized: map['isInitialized'] as bool? ?? false,
      isRegistered: map['isRegistered'] as bool? ?? false,
      errorMessage: map['errorMessage'] as String?,
      merchantId: map['merchantId'] as String?,
      terminalId: map['terminalId'] as String?,
      deviceId: map['deviceId'] as String?,
    );
  }

  /// Whether OTP is required for device registration
  bool get requiresOtp => isInitialized && !isRegistered;

  @override
  String toString() {
    return 'InitializeResult(isInitialized: $isInitialized, isRegistered: $isRegistered, errorMessage: $errorMessage)';
  }
}
