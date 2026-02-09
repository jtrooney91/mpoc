/// Types of transactions supported by the EMV SDK
enum TransactionType {
  /// Standard purchase transaction
  purchase,

  /// Refund transaction (requires supervisor PIN if configured)
  refund,

  /// Void a previous transaction
  void_,

  /// OTP registration transaction
  otp,
}

extension TransactionTypeExtension on TransactionType {
  String get value {
    switch (this) {
      case TransactionType.purchase:
        return 'PURCHASE';
      case TransactionType.refund:
        return 'REFUND';
      case TransactionType.void_:
        return 'VOID';
      case TransactionType.otp:
        return 'OTP';
    }
  }

  static TransactionType fromString(String value) {
    switch (value.toUpperCase()) {
      case 'PURCHASE':
        return TransactionType.purchase;
      case 'REFUND':
        return TransactionType.refund;
      case 'VOID':
        return TransactionType.void_;
      case 'OTP':
        return TransactionType.otp;
      default:
        return TransactionType.purchase;
    }
  }
}
