/// Entry in the transaction log/history
class TransactionLogEntry {
  /// Transaction ID
  final String? transactionId;

  /// Transaction date and time
  final DateTime? dateTime;

  /// Transaction amount in cents
  final int? amount;

  /// Tip amount in cents
  final int? tipAmount;

  /// Currency code
  final String? currencyCode;

  /// Transaction type (PURCHASE, REFUND, etc.)
  final String? transactionType;

  /// Transaction status (APPROVED, DECLINED, etc.)
  final String? status;

  /// Retrieval Reference Number
  final String? rrn;

  /// Authorization code
  final String? authCode;

  /// Masked PAN
  final String? maskedPan;

  /// Card scheme
  final String? cardScheme;

  /// Merchant ID
  final String? merchantId;

  /// Terminal ID
  final String? terminalId;

  /// Reference ID
  final String? referenceId;

  TransactionLogEntry({
    this.transactionId,
    this.dateTime,
    this.amount,
    this.tipAmount,
    this.currencyCode,
    this.transactionType,
    this.status,
    this.rrn,
    this.authCode,
    this.maskedPan,
    this.cardScheme,
    this.merchantId,
    this.terminalId,
    this.referenceId,
  });

  factory TransactionLogEntry.fromMap(Map<String, dynamic> map) {
    return TransactionLogEntry(
      transactionId: map['transactionId'] as String?,
      dateTime: map['dateTime'] != null
          ? DateTime.tryParse(map['dateTime'] as String)
          : null,
      amount: map['amount'] as int?,
      tipAmount: map['tipAmount'] as int?,
      currencyCode: map['currencyCode'] as String?,
      transactionType: map['transactionType'] as String?,
      status: map['status'] as String?,
      rrn: map['rrn'] as String?,
      authCode: map['authCode'] as String?,
      maskedPan: map['maskedPan'] as String?,
      cardScheme: map['cardScheme'] as String?,
      merchantId: map['merchantId'] as String?,
      terminalId: map['terminalId'] as String?,
      referenceId: map['referenceId'] as String?,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'transactionId': transactionId,
      'dateTime': dateTime?.toIso8601String(),
      'amount': amount,
      'tipAmount': tipAmount,
      'currencyCode': currencyCode,
      'transactionType': transactionType,
      'status': status,
      'rrn': rrn,
      'authCode': authCode,
      'maskedPan': maskedPan,
      'cardScheme': cardScheme,
      'merchantId': merchantId,
      'terminalId': terminalId,
      'referenceId': referenceId,
    };
  }

  @override
  String toString() {
    return 'TransactionLogEntry(transactionId: $transactionId, '
        'dateTime: $dateTime, amount: $amount, status: $status)';
  }
}
