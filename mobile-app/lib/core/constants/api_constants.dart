import 'dart:io';
import 'package:flutter/foundation.dart';

class ApiConstants {
  // Base URL resolution:
  // - Android Emulator: 10.0.2.2 points to host machine localhost
  // - iOS Simulator / Web / Desktop: localhost
  // - Physical Device: replace with your local Wi-Fi IP (e.g., http://192.168.1.10:8080)
  static String get baseUrl {
    if (kIsWeb) {
      return 'http://localhost:8080';
    }
    if (Platform.isAndroid) {
      return 'http://10.0.2.2:8080';
    }
    return 'http://localhost:8080';
  }

  // Auth endpoints
  static String get login => '$baseUrl/api/auth/login';
  static String get signup => '$baseUrl/api/auth/signup';

  // User self-service endpoints
  static String get userMe => '$baseUrl/api/user/me';

  // Admin user management endpoints
  static String get adminUsers => '$baseUrl/api/admin/users';
  static String adminUserById(int id) => '$baseUrl/api/admin/users/$id';
  static String adminUserState(int id) => '$baseUrl/api/admin/users/$id/state';
}
