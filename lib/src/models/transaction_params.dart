import '../enums/transaction_type.dart';

/// Parameters for initiating an EMV transaction
class TransactionParams {
  /// Transaction amount in cents
  final int amount;

  /// Type of transaction (purchase, refund, etc.)
  final TransactionType transactionType;

  /// Optional reference ID for the transaction
  final String? referenceId;

  /// Whether to acquire tip during transaction
  final bool acquireTip;

  /// Pre-set tip amount in cents (if not acquiring during transaction)
  final int? tipAmount;

  /// Currency code (e.g., "EUR", "USD", "ZAR")
  final String? currencyCode;

  /// Custom metadata to attach to the transaction
  final Map<String, dynamic>? metadata;

  /// Original transaction RRN (required for refunds/voids)
  final String? originalRrn;

  /// Original authorization code (required for refunds/voids)
  final String? originalAuthCode;

  TransactionParams({
    required this.amount,
    this.transactionType = TransactionType.purchase,
    this.referenceId,
    this.acquireTip = false,
    this.tipAmount,
    this.currencyCode,
    this.metadata,
    this.originalRrn,
    this.originalAuthCode,
  });

  Map<String, dynamic> toMap() {
    return {
      'amount': amount,
      'transactionType': transactionType.value,
      'referenceId': referenceId,
      'acquireTip': acquireTip,
      'tipAmount': tipAmount,
      'currencyCode': currencyCode,
      'metadata': metadata,
      'originalRrn': originalRrn,
      'originalAuthCode': originalAuthCode,
    };
  }

  factory TransactionParams.fromMap(Map<String, dynamic> map) {
    return TransactionParams(
      amount: map['amount'] as int,
      transactionType: TransactionTypeExtension.fromString(
        map['transactionType'] as String? ?? 'PURCHASE',
      ),
      referenceId: map['referenceId'] as String?,
      acquireTip: map['acquireTip'] as bool? ?? false,
      tipAmount: map['tipAmount'] as int?,
      currencyCode: map['currencyCode'] as String?,
      metadata: map['metadata'] as Map<String, dynamic>?,
      originalRrn: map['originalRrn'] as String?,
      originalAuthCode: map['originalAuthCode'] as String?,
    );
  }

  TransactionParams copyWith({
    int? amount,
    TransactionType? transactionType,
    String? referenceId,
    bool? acquireTip,
    int? tipAmount,
    String? currencyCode,
    Map<String, dynamic>? metadata,
    String? originalRrn,
    String? originalAuthCode,
  }) {
    return TransactionParams(
      amount: amount ?? this.amount,
      transactionType: transactionType ?? this.transactionType,
      referenceId: referenceId ?? this.referenceId,
      acquireTip: acquireTip ?? this.acquireTip,
      tipAmount: tipAmount ?? this.tipAmount,
      currencyCode: currencyCode ?? this.currencyCode,
      metadata: metadata ?? this.metadata,
      originalRrn: originalRrn ?? this.originalRrn,
      originalAuthCode: originalAuthCode ?? this.originalAuthCode,
    );
  }

  @override
  String toString() {
    return 'TransactionParams(amount: $amount, type: $transactionType, '
        'referenceId: $referenceId, acquireTip: $acquireTip)';
  }
}
