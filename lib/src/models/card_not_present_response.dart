/// Response from generating a Card Not Present (QR code) payment session
class CardNotPresentResponse {
  /// Whether the QR code generation was successful
  final bool isSuccessful;

  /// Error message if unsuccessful
  final String? error;

  /// URL for the QR code payment
  final String? paymentUrl;

  /// Session ID for tracking
  final String? sessionId;

  /// Expiry time for the QR code
  final DateTime? expiresAt;

  CardNotPresentResponse({
    required this.isSuccessful,
    this.error,
    this.paymentUrl,
    this.sessionId,
    this.expiresAt,
  });

  factory CardNotPresentResponse.fromMap(Map<String, dynamic> map) {
    return CardNotPresentResponse(
      isSuccessful: map['isSuccessful'] as bool? ?? false,
      error: map['error'] as String?,
      paymentUrl: map['paymentUrl'] as String?,
      sessionId: map['sessionId'] as String?,
      expiresAt: map['expiresAt'] != null
          ? DateTime.tryParse(map['expiresAt'] as String)
          : null,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'isSuccessful': isSuccessful,
      'error': error,
      'paymentUrl': paymentUrl,
      'sessionId': sessionId,
      'expiresAt': expiresAt?.toIso8601String(),
    };
  }

  @override
  String toString() {
    return 'CardNotPresentResponse(isSuccessful: $isSuccessful, '
        'paymentUrl: $paymentUrl, sessionId: $sessionId)';
  }
}
