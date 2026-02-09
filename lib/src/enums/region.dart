/// API regions supported by the EMV SDK
enum EmvRegion {
  /// European Union staging environment
  euStaging,

  /// South Africa staging environment
  saStaging,

  /// Middle East staging environment
  meStaging,

  /// European Union production environment
  euProduction,

  /// South Africa production environment
  saProduction,

  /// Middle East production environment
  meProduction,
}

extension EmvRegionExtension on EmvRegion {
  String get baseUrl {
    switch (this) {
      case EmvRegion.euStaging:
        return 'https://staging.wizzitdigital.com';
      case EmvRegion.saStaging:
        return 'https://sa-staging.wizzitdigital.com';
      case EmvRegion.meStaging:
        return 'https://me-staging.wizzitdigital.com';
      case EmvRegion.euProduction:
        return 'https://api.wizzitdigital.com';
      case EmvRegion.saProduction:
        return 'https://sa-api.wizzitdigital.com';
      case EmvRegion.meProduction:
        return 'https://me-api.wizzitdigital.com';
    }
  }

  String get value {
    return name;
  }

  static EmvRegion fromString(String value) {
    try {
      return EmvRegion.values.firstWhere(
        (e) => e.name.toLowerCase() == value.toLowerCase(),
        orElse: () => EmvRegion.euStaging,
      );
    } catch (_) {
      return EmvRegion.euStaging;
    }
  }
}
