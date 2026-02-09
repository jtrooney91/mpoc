/// Response from supervisor PIN operations
class SupervisorPinResponse {
  /// Whether the operation was successful
  final bool isSuccessful;

  /// Error message if unsuccessful
  final String? error;

  /// Whether supervisor PIN is required for the merchant
  final bool? pinRequired;

  /// Whether the PIN was verified successfully
  final bool? pinVerified;

  SupervisorPinResponse({
    required this.isSuccessful,
    this.error,
    this.pinRequired,
    this.pinVerified,
  });

  factory SupervisorPinResponse.fromMap(Map<String, dynamic> map) {
    return SupervisorPinResponse(
      isSuccessful: map['isSuccessful'] as bool? ?? false,
      error: map['error'] as String?,
      pinRequired: map['pinRequired'] as bool?,
      pinVerified: map['pinVerified'] as bool?,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'isSuccessful': isSuccessful,
      'error': error,
      'pinRequired': pinRequired,
      'pinVerified': pinVerified,
    };
  }

  @override
  String toString() {
    return 'SupervisorPinResponse(isSuccessful: $isSuccessful, '
        'pinRequired: $pinRequired, pinVerified: $pinVerified)';
  }
}
