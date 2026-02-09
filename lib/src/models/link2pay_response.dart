/// Response from retrieving a Link2Pay session
class Link2PayResponse {
  /// Whether the session retrieval was successful
  final bool isSuccessful;

  /// Error message if unsuccessful
  final String? error;

  /// Transaction amount in cents
  final int? amount;

  /// Reference ID for the transaction
  final String? referenceId;

  /// Merchant ID associated with the session
  final String? merchantId;

  /// Additional metadata from the session
  final Map<String, dynamic>? metadata;

  Link2PayResponse({
    required this.isSuccessful,
    this.error,
    this.amount,
    this.referenceId,
    this.merchantId,
    this.metadata,
  });

  factory Link2PayResponse.fromMap(Map<String, dynamic> map) {
    return Link2PayResponse(
      isSuccessful: map['isSuccessful'] as bool? ?? false,
      error: map['error'] as String?,
      amount: map['amount'] as int?,
      referenceId: map['referenceId'] as String?,
      merchantId: map['merchantId'] as String?,
      metadata: map['metadata'] as Map<String, dynamic>?,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'isSuccessful': isSuccessful,
      'error': error,
      'amount': amount,
      'referenceId': referenceId,
      'merchantId': merchantId,
      'metadata': metadata,
    };
  }

  @override
  String toString() {
    return 'Link2PayResponse(isSuccessful: $isSuccessful, amount: $amount, '
        'referenceId: $referenceId, error: $error)';
  }
}
