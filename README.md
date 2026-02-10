# Wizzit EMV Flutter Plugin

A Flutter plugin for Wizzit EMV NFC card payment processing. This plugin provides contactless payment capabilities, transaction management, and receipt delivery.

## Features

- **NFC Contactless Payments** - Process EMV card payments via NFC
- **Transaction Types** - Purchase, Refund, and Void operations
- **Device Registration** - OTP-based device registration
- **QR Code Payments** - Generate Card-Not-Present payment sessions
- **Link2Pay** - Handle payment sessions from deep links
- **Transaction History** - Query and filter past transactions
- **Receipt Delivery** - Send receipts via Email and SMS
- **Supervisor PIN** - PIN-based authorization for refunds/voids

## Installation

Add the plugin to your `pubspec.yaml`:

```yaml
dependencies:
  wizzit_emv_flutter:
    path: ../wizzit_emv_flutter  # or use git/pub reference
```

## Android Setup

### Permissions

The plugin automatically requests these permissions:
- `android.permission.NFC`
- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`

### NFC Feature

Add this to your `AndroidManifest.xml` if you want to require NFC:

```xml
<uses-feature
    android:name="android.hardware.nfc"
    android:required="true" />
```

### NFC Tech Filter

Create `res/xml/nfc_tech_filter.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <tech-list>
        <tech>android.nfc.tech.IsoDep</tech>
    </tech-list>
    <tech-list>
        <tech>android.nfc.tech.NfcA</tech>
    </tech-list>
    <tech-list>
        <tech>android.nfc.tech.NfcB</tech>
    </tech-list>
</resources>
```

## Usage

### Initialize the Plugin

```dart
import 'package:wizzit_emv_flutter/wizzit_emv_flutter.dart';

final emv = WizzitEmv();

// Check NFC availability
final nfcAvailable = await emv.isNfcAvailable();

// Initialize with configuration
await emv.initialize(EmvConfig(
  region: EmvRegion.euStaging,
  authCredentials: 'your-base64-credentials',
  merchantName: 'My Store',
));
```

### Listen for Events

```dart
emv.eventStream.listen((event) {
  print('Event: ${event.event}');
  print('Message: ${event.message}');

  switch (event.event) {
    case EmvEvent.cardDetected:
      print('Card detected!');
      break;
    case EmvEvent.transactionApproved:
      print('Transaction approved!');
      break;
    case EmvEvent.transactionDeclined:
      print('Transaction declined: ${event.message}');
      break;
    // Handle other events...
  }
});
```

### Check Device Registration

```dart
final regStatus = await emv.checkDeviceRegistration();
if (regStatus['isRegistered'] == true) {
  print('Device is registered');
} else {
  // Need to register with OTP
  await emv.registerDevice('123456');
}
```

### Start a Transaction

```dart
final result = await emv.startTransaction(TransactionParams(
  amount: 1000, // $10.00 in cents
  transactionType: TransactionType.purchase,
  referenceId: 'ORDER-123',
  acquireTip: false,
));

if (result.isSuccessful) {
  print('Approved! RRN: ${result.rrn}');
  print('Auth Code: ${result.authCode}');
  print('Card: ${result.maskedPan}');
} else {
  print('Declined: ${result.reason}');
}
```

### Cancel a Transaction

```dart
await emv.cancelTransaction();
```

### Void a Transaction

```dart
final voidResult = await emv.voidTransaction(
  rrn: 'original-rrn',
  authCode: 'original-auth-code',
  supervisorPin: '1234', // if required
);
```

### Generate QR Payment

```dart
final qrResponse = await emv.generateQrPayment(
  amount: 2500,
  msisdn: '+1234567890', // optional
  ttlSeconds: 300,
);

if (qrResponse.isSuccessful) {
  // Display QR code from qrResponse.paymentUrl
}
```

### Get Transaction History

```dart
final transactions = await emv.getTransactionHistory(
  startDate: DateTime.now().subtract(Duration(days: 7)),
  endDate: DateTime.now(),
  limit: 50,
);

for (final tx in transactions) {
  print('${tx.dateTime}: ${tx.amount} - ${tx.status}');
}
```

### Send Receipt

```dart
// Email receipt
await emv.sendEmailReceipt(
  email: 'customer@example.com',
  transactionId: 'tx-id',
);

// SMS receipt
await emv.sendSmsReceipt(
  phoneNumber: '+1234567890',
  transactionId: 'tx-id',
);
```

## Models

### TransactionParams

```dart
TransactionParams(
  amount: int,              // Amount in cents
  transactionType: TransactionType,
  referenceId: String?,
  acquireTip: bool,
  tipAmount: int?,
  currencyCode: String?,
  metadata: Map<String, dynamic>?,
  originalRrn: String?,     // For refunds/voids
  originalAuthCode: String?,
)
```

### TransactionResult

```dart
TransactionResult(
  isSuccessful: bool,
  statusCode: String?,
  reason: String?,
  referenceNumber: String?,
  rrn: String?,
  authCode: String?,
  merchantId: String?,
  terminalId: String?,
  cardScheme: String?,
  maskedPan: String?,
  cardholderName: String?,
  expiryDate: String?,
  amount: int?,
  tipAmount: int?,
  currencyCode: String?,
  transactionDateTime: DateTime?,
  issuer: String?,
  aid: String?,
  applicationLabel: String?,
  cryptogram: String?,
  sessionData: Map<String, dynamic>?,
)
```

### EmvConfig

```dart
EmvConfig(
  region: EmvRegion,
  authCredentials: String?,
  merchantName: String?,
  enableAudio: bool,
  enableVibration: bool,
  cardReadTimeoutMs: int,
  onlineAuthTimeoutMs: int,
)
```

## Regions

- `EmvRegion.euStaging` - EU Staging
- `EmvRegion.saStaging` - South Africa Staging
- `EmvRegion.meStaging` - Middle East Staging
- `EmvRegion.euProduction` - EU Production
- `EmvRegion.saProduction` - South Africa Production
- `EmvRegion.meProduction` - Middle East Production

## Events

The `EmvEvent` enum includes:

- `adapterInitialized` / `adapterInitializationFailed`
- `deviceRegistered` / `deviceNotRegistered`
- `otpRequired` / `otpVerified` / `otpFailed`
- `sessionInitialized`
- `cardDetected` / `readingCard` / `cardReadSuccess` / `cardReadFailed`
- `processingTransaction` / `onlineAuthorization`
- `transactionApproved` / `transactionDeclined` / `transactionCancelled` / `transactionError`
- `pinEntryRequired` / `signatureRequired`
- `waitingForCardRemoval` / `cardRemoved`
- `sessionEnded`

## Requirements

- Flutter 3.3.0+
- Android SDK 26+ (Android 8.0 Oreo)
- NFC-enabled Android device

## License

Proprietary - Wizzit Digital
