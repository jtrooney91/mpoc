/// Result of an EMV transaction
class TransactionResult {
  /// Whether the transaction was successful
  final bool isSuccessful;

  /// Status code returned by the payment processor
  final String? statusCode;

  /// Human-readable reason for the transaction result
  final String? reason;

  /// Reference number for the transaction
  final String? referenceNumber;

  /// Retrieval Reference Number (RRN)
  final String? rrn;

  /// Authorization code
  final String? authCode;

  /// Merchant ID
  final String? merchantId;

  /// Terminal ID
  final String? terminalId;

  /// Card scheme (Visa, MasterCard, etc.)
  final String? cardScheme;

  /// Masked PAN (e.g., **** **** **** 1234)
  final String? maskedPan;

  /// Cardholder name
  final String? cardholderName;

  /// Card expiry date
  final String? expiryDate;

  /// Transaction amount in cents
  final int? amount;

  /// Tip amount in cents
  final int? tipAmount;

  /// Currency code (e.g., EUR, USD)
  final String? currencyCode;

  /// Transaction date and time
  final DateTime? transactionDateTime;

  /// Card issuer name
  final String? issuer;

  /// Application ID (AID)
  final String? aid;

  /// Application label
  final String? applicationLabel;

  /// Transaction cryptogram
  final String? cryptogram;

  /// Additional session data as key-value pairs
  final Map<String, dynamic>? sessionData;

  TransactionResult({
    required this.isSuccessful,
    this.statusCode,
    this.reason,
    this.referenceNumber,
    this.rrn,
    this.authCode,
    this.merchantId,
    this.terminalId,
    this.cardScheme,
    this.maskedPan,
    this.cardholderName,
    this.expiryDate,
    this.amount,
    this.tipAmount,
    this.currencyCode,
    this.transactionDateTime,
    this.issuer,
    this.aid,
    this.applicationLabel,
    this.cryptogram,
    this.sessionData,
  });

  factory TransactionResult.fromMap(Map<String, dynamic> map) {
    return TransactionResult(
      isSuccessful: map['isSuccessful'] as bool? ?? false,
      statusCode: map['statusCode'] as String?,
      reason: map['reason'] as String?,
      referenceNumber: map['referenceNumber'] as String?,
      rrn: map['rrn'] as String?,
      authCode: map['authCode'] as String?,
      merchantId: map['merchantId'] as String?,
      terminalId: map['terminalId'] as String?,
      cardScheme: map['cardScheme'] as String?,
      maskedPan: map['maskedPan'] as String?,
      cardholderName: map['cardholderName'] as String?,
      expiryDate: map['expiryDate'] as String?,
      amount: map['amount'] as int?,
      tipAmount: map['tipAmount'] as int?,
      currencyCode: map['currencyCode'] as String?,
      transactionDateTime: map['transactionDateTime'] != null
          ? DateTime.tryParse(map['transactionDateTime'] as String)
          : null,
      issuer: map['issuer'] as String?,
      aid: map['aid'] as String?,
      applicationLabel: map['applicationLabel'] as String?,
      cryptogram: map['cryptogram'] as String?,
      sessionData: map['sessionData'] as Map<String, dynamic>?,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'isSuccessful': isSuccessful,
      'statusCode': statusCode,
      'reason': reason,
      'referenceNumber': referenceNumber,
      'rrn': rrn,
      'authCode': authCode,
      'merchantId': merchantId,
      'terminalId': terminalId,
      'cardScheme': cardScheme,
      'maskedPan': maskedPan,
      'cardholderName': cardholderName,
      'expiryDate': expiryDate,
      'amount': amount,
      'tipAmount': tipAmount,
      'currencyCode': currencyCode,
      'transactionDateTime': transactionDateTime?.toIso8601String(),
      'issuer': issuer,
      'aid': aid,
      'applicationLabel': applicationLabel,
      'cryptogram': cryptogram,
      'sessionData': sessionData,
    };
  }

  @override
  String toString() {
    return 'TransactionResult(isSuccessful: $isSuccessful, statusCode: $statusCode, '
        'reason: $reason, rrn: $rrn, maskedPan: $maskedPan, amount: $amount)';
  }
}
