import 'dart:async';
import 'package:flutter/services.dart';

import 'models/transaction_result.dart';
import 'models/transaction_params.dart';
import 'models/link2pay_response.dart';
import 'models/card_not_present_response.dart';
import 'models/supervisor_pin_response.dart';
import 'models/void_response.dart';
import 'models/transaction_log_entry.dart';
import 'models/emv_config.dart';
import 'enums/emv_event.dart';

/// Main class for interacting with the Wizzit EMV SDK
///
/// This plugin provides NFC-based EMV card payment processing capabilities
/// including contactless payments, refunds, voids, and transaction history.
///
/// Example usage:
/// ```dart
/// final emv = WizzitEmv();
///
/// // Initialize the adapter
/// await emv.initialize(EmvConfig(
///   region: EmvRegion.euStaging,
///   authCredentials: 'your-credentials',
/// ));
///
/// // Listen for events
/// emv.eventStream.listen((event) {
///   print('EMV Event: ${event.event}');
/// });
///
/// // Start a transaction
/// final result = await emv.startTransaction(TransactionParams(
///   amount: 1000, // $10.00
///   transactionType: TransactionType.purchase,
/// ));
/// ```
class WizzitEmv {
  static const MethodChannel _channel = MethodChannel('wizzit_emv_flutter');
  static const EventChannel _eventChannel =
      EventChannel('wizzit_emv_flutter/events');

  static WizzitEmv? _instance;

  /// Get the singleton instance of WizzitEmv
  factory WizzitEmv() {
    _instance ??= WizzitEmv._internal();
    return _instance!;
  }

  WizzitEmv._internal();

  Stream<EmvEventData>? _eventStream;

  /// Stream of EMV events during transaction processing
  ///
  /// Subscribe to this stream to receive real-time updates about
  /// the transaction state, card detection, and processing status.
  Stream<EmvEventData> get eventStream {
    _eventStream ??= _eventChannel.receiveBroadcastStream().map((event) {
      final map = Map<String, dynamic>.from(event as Map);
      return EmvEventData.fromMap(map);
    });
    return _eventStream!;
  }

  /// Initialize the EMV adapter with the given configuration
  ///
  /// This must be called before any other operations.
  /// Returns `true` if initialization was successful.
  Future<bool> initialize(EmvConfig config) async {
    final result = await _channel.invokeMethod<bool>(
      'initialize',
      config.toMap(),
    );
    return result ?? false;
  }

  /// Dispose of the EMV adapter and release resources
  Future<void> dispose() async {
    await _channel.invokeMethod('dispose');
  }

  /// Check if the device is registered with the payment processor
  ///
  /// Returns a map containing registration status and device information.
  Future<Map<String, dynamic>> checkDeviceRegistration() async {
    final result = await _channel.invokeMethod<Map>(
      'checkDeviceRegistration',
    );
    return Map<String, dynamic>.from(result ?? {});
  }

  /// Register the device using an OTP code
  ///
  /// [otp] - The one-time password received for device registration
  Future<bool> registerDevice(String otp) async {
    final result = await _channel.invokeMethod<bool>(
      'registerDevice',
      {'otp': otp},
    );
    return result ?? false;
  }

  /// Start a new payment transaction
  ///
  /// [params] - Transaction parameters including amount and type
  ///
  /// Returns a [TransactionResult] with the outcome of the transaction.
  /// Listen to [eventStream] for real-time updates during processing.
  Future<TransactionResult> startTransaction(TransactionParams params) async {
    final result = await _channel.invokeMethod<Map>(
      'startTransaction',
      params.toMap(),
    );
    return TransactionResult.fromMap(Map<String, dynamic>.from(result ?? {}));
  }

  /// Cancel an ongoing transaction
  ///
  /// Call this to abort a transaction that is in progress.
  Future<void> cancelTransaction() async {
    await _channel.invokeMethod('cancelTransaction');
  }

  /// Void a previous transaction
  ///
  /// [rrn] - The Retrieval Reference Number of the transaction to void
  /// [authCode] - The authorization code of the original transaction
  /// [supervisorPin] - Supervisor PIN if required by merchant configuration
  Future<VoidResponse> voidTransaction({
    required String rrn,
    required String authCode,
    String? supervisorPin,
  }) async {
    final result = await _channel.invokeMethod<Map>(
      'voidTransaction',
      {
        'rrn': rrn,
        'authCode': authCode,
        'supervisorPin': supervisorPin,
      },
    );
    return VoidResponse.fromMap(Map<String, dynamic>.from(result ?? {}));
  }

  /// Generate a Card Not Present (QR code) payment session
  ///
  /// [amount] - Transaction amount in cents
  /// [msisdn] - Customer phone number (optional)
  /// [ttlSeconds] - Time-to-live for the QR code in seconds
  Future<CardNotPresentResponse> generateQrPayment({
    required int amount,
    String? msisdn,
    int ttlSeconds = 300,
  }) async {
    final result = await _channel.invokeMethod<Map>(
      'generateQrPayment',
      {
        'amount': amount,
        'msisdn': msisdn,
        'ttlSeconds': ttlSeconds,
      },
    );
    return CardNotPresentResponse.fromMap(
        Map<String, dynamic>.from(result ?? {}));
  }

  /// Retrieve a Link2Pay session from a URL
  ///
  /// [url] - The Link2Pay URL containing session information
  Future<Link2PayResponse> getLink2PaySession(String url) async {
    final result = await _channel.invokeMethod<Map>(
      'getLink2PaySession',
      {'url': url},
    );
    return Link2PayResponse.fromMap(Map<String, dynamic>.from(result ?? {}));
  }

  /// Check if supervisor PIN is required for this merchant
  Future<SupervisorPinResponse> checkSupervisorPinRequired() async {
    final result = await _channel.invokeMethod<Map>(
      'checkSupervisorPinRequired',
    );
    return SupervisorPinResponse.fromMap(
        Map<String, dynamic>.from(result ?? {}));
  }

  /// Set the supervisor PIN for the merchant
  ///
  /// [pin] - The supervisor PIN to set
  Future<SupervisorPinResponse> setSupervisorPin(String pin) async {
    final result = await _channel.invokeMethod<Map>(
      'setSupervisorPin',
      {'pin': pin},
    );
    return SupervisorPinResponse.fromMap(
        Map<String, dynamic>.from(result ?? {}));
  }

  /// Verify a supervisor PIN
  ///
  /// [pin] - The supervisor PIN to verify
  Future<SupervisorPinResponse> verifySupervisorPin(String pin) async {
    final result = await _channel.invokeMethod<Map>(
      'verifySupervisorPin',
      {'pin': pin},
    );
    return SupervisorPinResponse.fromMap(
        Map<String, dynamic>.from(result ?? {}));
  }

  /// Get transaction history/log
  ///
  /// [startDate] - Start date for the query
  /// [endDate] - End date for the query
  /// [limit] - Maximum number of entries to return
  /// [offset] - Offset for pagination
  Future<List<TransactionLogEntry>> getTransactionHistory({
    DateTime? startDate,
    DateTime? endDate,
    int limit = 50,
    int offset = 0,
  }) async {
    final result = await _channel.invokeMethod<List>(
      'getTransactionHistory',
      {
        'startDate': startDate?.toIso8601String(),
        'endDate': endDate?.toIso8601String(),
        'limit': limit,
        'offset': offset,
      },
    );
    return (result ?? [])
        .map((e) => TransactionLogEntry.fromMap(Map<String, dynamic>.from(e)))
        .toList();
  }

  /// Send a receipt via email
  ///
  /// [email] - Recipient email address
  /// [transactionId] - ID of the transaction for the receipt
  Future<bool> sendEmailReceipt({
    required String email,
    required String transactionId,
  }) async {
    final result = await _channel.invokeMethod<bool>(
      'sendEmailReceipt',
      {
        'email': email,
        'transactionId': transactionId,
      },
    );
    return result ?? false;
  }

  /// Send a receipt via SMS
  ///
  /// [phoneNumber] - Recipient phone number
  /// [transactionId] - ID of the transaction for the receipt
  Future<bool> sendSmsReceipt({
    required String phoneNumber,
    required String transactionId,
  }) async {
    final result = await _channel.invokeMethod<bool>(
      'sendSmsReceipt',
      {
        'phoneNumber': phoneNumber,
        'transactionId': transactionId,
      },
    );
    return result ?? false;
  }

  /// Get device information
  ///
  /// Returns device ID, app version, and other device-specific information.
  Future<Map<String, dynamic>> getDeviceInfo() async {
    final result = await _channel.invokeMethod<Map>('getDeviceInfo');
    return Map<String, dynamic>.from(result ?? {});
  }

  /// Update the EMV configuration
  ///
  /// [config] - New configuration to apply
  Future<bool> updateConfig(EmvConfig config) async {
    final result = await _channel.invokeMethod<bool>(
      'updateConfig',
      config.toMap(),
    );
    return result ?? false;
  }

  /// Check if NFC is available and enabled on the device
  Future<bool> isNfcAvailable() async {
    final result = await _channel.invokeMethod<bool>('isNfcAvailable');
    return result ?? false;
  }

  /// Check if the adapter is initialized and ready
  Future<bool> isReady() async {
    final result = await _channel.invokeMethod<bool>('isReady');
    return result ?? false;
  }
}

/// Data class for EMV events
class EmvEventData {
  /// The type of event
  final EmvEvent event;

  /// Optional message associated with the event
  final String? message;

  /// Optional data payload
  final Map<String, dynamic>? data;

  EmvEventData({
    required this.event,
    this.message,
    this.data,
  });

  factory EmvEventData.fromMap(Map<String, dynamic> map) {
    return EmvEventData(
      event: EmvEventExtension.fromString(map['event'] as String? ?? ''),
      message: map['message'] as String?,
      data: map['data'] != null
          ? Map<String, dynamic>.from(map['data'] as Map)
          : null,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'event': event.value,
      'message': message,
      'data': data,
    };
  }

  @override
  String toString() {
    return 'EmvEventData(event: $event, message: $message)';
  }
}
