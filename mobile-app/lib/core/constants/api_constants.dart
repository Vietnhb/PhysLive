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

  // User endpoints
  static String get userMe => '$baseUrl/api/user/me';
  static String get users => '$baseUrl/api/user';
  static String userById(int id) => '$baseUrl/api/user/$id';
  static String userState(int id) => '$baseUrl/api/user/$id/state';
}
