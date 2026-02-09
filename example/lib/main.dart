import 'dart:async';
import 'package:flutter/material.dart';
import 'package:wizzit_emv_flutter/wizzit_emv_flutter.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Wizzit EMV Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: const EmvDemoPage(),
    );
  }
}

class EmvDemoPage extends StatefulWidget {
  const EmvDemoPage({super.key});

  @override
  State<EmvDemoPage> createState() => _EmvDemoPageState();
}

class _EmvDemoPageState extends State<EmvDemoPage> {
  final WizzitEmv _emv = WizzitEmv();
  final TextEditingController _amountController = TextEditingController(text: '1000');
  final TextEditingController _otpController = TextEditingController();

  bool _isInitialized = false;
  bool _isNfcAvailable = false;
  bool _isRegistered = false;
  bool _isProcessing = false;
  String _status = 'Not initialized';
  String _lastEvent = '';
  TransactionResult? _lastTransaction;

  StreamSubscription<EmvEventData>? _eventSubscription;

  @override
  void initState() {
    super.initState();
    _checkNfc();
  }

  @override
  void dispose() {
    _eventSubscription?.cancel();
    _amountController.dispose();
    _otpController.dispose();
    _emv.dispose();
    super.dispose();
  }

  Future<void> _checkNfc() async {
    final available = await _emv.isNfcAvailable();
    setState(() {
      _isNfcAvailable = available;
      _status = available ? 'NFC available' : 'NFC not available';
    });
  }

  Future<void> _initialize() async {
    setState(() {
      _isProcessing = true;
      _status = 'Initializing...';
    });

    try {
      // Subscribe to events
      _eventSubscription?.cancel();
      _eventSubscription = _emv.eventStream.listen((event) {
        setState(() {
          _lastEvent = '${event.event.name}: ${event.message ?? ''}';
        });
        debugPrint('EMV Event: ${event.event} - ${event.message}');
        
        // Handle OTP required event - show registration UI
        if (event.event == EmvEvent.otpRequired) {
          setState(() {
            _isInitialized = true;  // Allow access to registration UI
            _isRegistered = false;
            _status = 'OTP Required - Please enter OTP to register device';
          });
        }
      });

      // Initialize with staging configuration
      final success = await _emv.initialize(EmvConfig(
        region: EmvRegion.euStaging,
        // Add your auth credentials here
        // authCredentials: 'your-base64-credentials',
      ));

      if (success) {
        // Device is registered and adapter initialized
        setState(() {
          _isInitialized = true;
          _isRegistered = true;
          _status = 'Ready - Device registered';
        });
      } else {
        // Check if OTP is required (device not registered)
        // The event listener will handle otpRequired event
        setState(() {
          // Status will be updated by event listener if OTP is required
          if (_status == 'OTP Required - Please enter OTP to register device') {
            // Already handled by event listener
          } else {
            _status = 'Initialization failed - check device registration';
          }
        });
      }
    } catch (e) {
      setState(() {
        _status = 'Error: $e';
      });
    } finally {
      setState(() {
        _isProcessing = false;
      });
    }
  }

  Future<void> _registerDevice() async {
    final otp = _otpController.text.trim();
    if (otp.isEmpty) {
      _showSnackBar('Please enter OTP');
      return;
    }

    setState(() {
      _isProcessing = true;
      _status = 'Registering device...';
    });

    try {
      final success = await _emv.registerDevice(otp);
      setState(() {
        _isRegistered = success;
        _status = success ? 'Device registered successfully' : 'Registration failed';
      });
      _otpController.clear();
    } catch (e) {
      setState(() {
        _status = 'Registration error: $e';
      });
    } finally {
      setState(() {
        _isProcessing = false;
      });
    }
  }

  Future<void> _startTransaction() async {
    final amountText = _amountController.text.trim();
    final amount = int.tryParse(amountText);

    if (amount == null || amount <= 0) {
      _showSnackBar('Please enter a valid amount');
      return;
    }

    setState(() {
      _isProcessing = true;
      _status = 'Starting transaction...';
      _lastTransaction = null;
    });

    try {
      final result = await _emv.startTransaction(TransactionParams(
        amount: amount,
        transactionType: TransactionType.purchase,
      ));

      setState(() {
        _lastTransaction = result;
        _status = result.isSuccessful
            ? 'Transaction approved!'
            : 'Transaction declined: ${result.reason}';
      });
    } catch (e) {
      setState(() {
        _status = 'Transaction error: $e';
      });
    } finally {
      setState(() {
        _isProcessing = false;
      });
    }
  }

  Future<void> _cancelTransaction() async {
    await _emv.cancelTransaction();
    setState(() {
      _isProcessing = false;
      _status = 'Transaction cancelled';
    });
  }

  void _showSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Wizzit EMV Demo'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Status card
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Status',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 8),
                    Text(_status),
                    if (_lastEvent.isNotEmpty) ...[
                      const SizedBox(height: 8),
                      Text(
                        'Last event: $_lastEvent',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ],
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Icon(
                          _isNfcAvailable ? Icons.nfc : Icons.nfc_outlined,
                          color: _isNfcAvailable ? Colors.green : Colors.red,
                        ),
                        const SizedBox(width: 8),
                        Text(_isNfcAvailable ? 'NFC Available' : 'NFC Not Available'),
                      ],
                    ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 16),

            // Initialize button
            if (!_isInitialized)
              ElevatedButton(
                onPressed: _isProcessing ? null : _initialize,
                child: _isProcessing
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text('Initialize EMV'),
              ),

            // Registration section
            if (_isInitialized && !_isRegistered) ...[
              const SizedBox(height: 16),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Device Registration',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 8),
                      TextField(
                        controller: _otpController,
                        decoration: const InputDecoration(
                          labelText: 'OTP Code',
                          border: OutlineInputBorder(),
                        ),
                        keyboardType: TextInputType.number,
                      ),
                      const SizedBox(height: 8),
                      ElevatedButton(
                        onPressed: _isProcessing ? null : _registerDevice,
                        child: const Text('Register Device'),
                      ),
                    ],
                  ),
                ),
              ),
            ],

            // Transaction section
            if (_isInitialized && _isRegistered) ...[
              const SizedBox(height: 16),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'New Transaction',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 8),
                      TextField(
                        controller: _amountController,
                        decoration: const InputDecoration(
                          labelText: 'Amount (cents)',
                          border: OutlineInputBorder(),
                          helperText: 'Enter amount in cents (e.g., 1000 = \$10.00)',
                        ),
                        keyboardType: TextInputType.number,
                      ),
                      const SizedBox(height: 16),
                      Row(
                        children: [
                          Expanded(
                            child: ElevatedButton(
                              onPressed: _isProcessing ? null : _startTransaction,
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.green,
                                foregroundColor: Colors.white,
                              ),
                              child: _isProcessing
                                  ? const SizedBox(
                                      height: 20,
                                      width: 20,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2,
                                        color: Colors.white,
                                      ),
                                    )
                                  : const Text('Start Transaction'),
                            ),
                          ),
                          if (_isProcessing) ...[
                            const SizedBox(width: 8),
                            ElevatedButton(
                              onPressed: _cancelTransaction,
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.red,
                                foregroundColor: Colors.white,
                              ),
                              child: const Text('Cancel'),
                            ),
                          ],
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ],

            // Transaction result
            if (_lastTransaction != null) ...[
              const SizedBox(height: 16),
              Card(
                color: _lastTransaction!.isSuccessful
                    ? Colors.green.shade50
                    : Colors.red.shade50,
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Icon(
                            _lastTransaction!.isSuccessful
                                ? Icons.check_circle
                                : Icons.cancel,
                            color: _lastTransaction!.isSuccessful
                                ? Colors.green
                                : Colors.red,
                          ),
                          const SizedBox(width: 8),
                          Text(
                            _lastTransaction!.isSuccessful
                                ? 'Transaction Approved'
                                : 'Transaction Declined',
                            style: Theme.of(context).textTheme.titleMedium,
                          ),
                        ],
                      ),
                      const Divider(),
                      _buildResultRow('Amount', _formatAmount(_lastTransaction!.amount)),
                      _buildResultRow('Card', _lastTransaction!.maskedPan),
                      _buildResultRow('Scheme', _lastTransaction!.cardScheme),
                      _buildResultRow('RRN', _lastTransaction!.rrn),
                      _buildResultRow('Auth Code', _lastTransaction!.authCode),
                      _buildResultRow('Status Code', _lastTransaction!.statusCode),
                      if (_lastTransaction!.reason != null)
                        _buildResultRow('Reason', _lastTransaction!.reason),
                    ],
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildResultRow(String label, String? value) {
    if (value == null || value.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 100,
            child: Text(
              '$label:',
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          Expanded(child: Text(value)),
        ],
      ),
    );
  }

  String _formatAmount(int? cents) {
    if (cents == null) return 'N/A';
    final dollars = cents / 100;
    return '\$${dollars.toStringAsFixed(2)}';
  }
}
