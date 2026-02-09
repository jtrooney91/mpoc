/// Response from voiding a transaction
class VoidResponse {
  /// Whether the void was successful
  final bool isSuccessful;

  /// Error message if unsuccessful
  final String? error;

  /// Authorization code for the void
  final String? authCode;

  /// Description of the void result
  final String? description;

  VoidResponse({
    required this.isSuccessful,
    this.error,
    this.authCode,
    this.description,
  });

  factory VoidResponse.fromMap(Map<String, dynamic> map) {
    return VoidResponse(
      isSuccessful: map['isSuccessful'] as bool? ?? false,
      error: map['error'] as String?,
      authCode: map['authCode'] as String?,
      description: map['description'] as String?,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'isSuccessful': isSuccessful,
      'error': error,
      'authCode': authCode,
      'description': description,
    };
  }

  @override
  String toString() {
    return 'VoidResponse(isSuccessful: $isSuccessful, authCode: $authCode, '
        'description: $description)';
  }
}
