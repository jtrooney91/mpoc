import '../enums/region.dart';

/// Configuration for the EMV SDK
class EmvConfig {
  /// API region to use
  final EmvRegion region;

  /// Authorization credentials (Base64 encoded)
  final String? authCredentials;

  /// Merchant name to display
  final String? merchantName;

  /// Whether to enable audio feedback
  final bool enableAudio;

  /// Whether to enable vibration feedback
  final bool enableVibration;

  /// Timeout for card read operations in milliseconds
  final int cardReadTimeoutMs;

  /// Timeout for online authorization in milliseconds
  final int onlineAuthTimeoutMs;

  EmvConfig({
    this.region = EmvRegion.euStaging,
    this.authCredentials,
    this.merchantName,
    this.enableAudio = true,
    this.enableVibration = true,
    this.cardReadTimeoutMs = 60000,
    this.onlineAuthTimeoutMs = 30000,
  });

  Map<String, dynamic> toMap() {
    return {
      'region': region.value,
      'baseUrl': region.baseUrl,
      'authCredentials': authCredentials,
      'merchantName': merchantName,
      'enableAudio': enableAudio,
      'enableVibration': enableVibration,
      'cardReadTimeoutMs': cardReadTimeoutMs,
      'onlineAuthTimeoutMs': onlineAuthTimeoutMs,
    };
  }

  factory EmvConfig.fromMap(Map<String, dynamic> map) {
    return EmvConfig(
      region: EmvRegionExtension.fromString(map['region'] as String? ?? ''),
      authCredentials: map['authCredentials'] as String?,
      merchantName: map['merchantName'] as String?,
      enableAudio: map['enableAudio'] as bool? ?? true,
      enableVibration: map['enableVibration'] as bool? ?? true,
      cardReadTimeoutMs: map['cardReadTimeoutMs'] as int? ?? 60000,
      onlineAuthTimeoutMs: map['onlineAuthTimeoutMs'] as int? ?? 30000,
    );
  }

  EmvConfig copyWith({
    EmvRegion? region,
    String? authCredentials,
    String? merchantName,
    bool? enableAudio,
    bool? enableVibration,
    int? cardReadTimeoutMs,
    int? onlineAuthTimeoutMs,
  }) {
    return EmvConfig(
      region: region ?? this.region,
      authCredentials: authCredentials ?? this.authCredentials,
      merchantName: merchantName ?? this.merchantName,
      enableAudio: enableAudio ?? this.enableAudio,
      enableVibration: enableVibration ?? this.enableVibration,
      cardReadTimeoutMs: cardReadTimeoutMs ?? this.cardReadTimeoutMs,
      onlineAuthTimeoutMs: onlineAuthTimeoutMs ?? this.onlineAuthTimeoutMs,
    );
  }

  @override
  String toString() {
    return 'EmvConfig(region: $region, merchantName: $merchantName, '
        'enableAudio: $enableAudio, enableVibration: $enableVibration)';
  }
}
