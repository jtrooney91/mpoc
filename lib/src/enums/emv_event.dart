/// Events emitted by the EMV adapter during transaction processing
enum EmvEvent {
  /// Adapter has been initialized successfully
  adapterInitialized,

  /// Adapter initialization failed
  adapterInitializationFailed,

  /// Device is checking registration status
  checkingRegistration,

  /// Device is registered and ready
  deviceRegistered,

  /// Device is not registered
  deviceNotRegistered,

  /// OTP registration required
  otpRequired,

  /// OTP verification successful
  otpVerified,

  /// OTP verification failed
  otpFailed,

  /// Session initialized, ready for card tap
  sessionInitialized,

  /// Card has been detected
  cardDetected,

  /// Card reading in progress
  readingCard,

  /// Card read successfully
  cardReadSuccess,

  /// Card read failed
  cardReadFailed,

  /// Transaction is being processed
  processingTransaction,

  /// Transaction approved
  transactionApproved,

  /// Transaction declined
  transactionDeclined,

  /// Transaction cancelled by user
  transactionCancelled,

  /// Transaction error occurred
  transactionError,

  /// Session ended
  sessionEnded,

  /// Waiting for card removal
  waitingForCardRemoval,

  /// Card has been removed
  cardRemoved,

  /// PIN entry required
  pinEntryRequired,

  /// Online authorization in progress
  onlineAuthorization,

  /// Signature required
  signatureRequired,

  /// Unknown event
  unknown,
}

extension EmvEventExtension on EmvEvent {
  String get value {
    return name;
  }

  static EmvEvent fromString(String value) {
    try {
      return EmvEvent.values.firstWhere(
        (e) => e.name.toLowerCase() == value.toLowerCase(),
        orElse: () => EmvEvent.unknown,
      );
    } catch (_) {
      return EmvEvent.unknown;
    }
  }
}
