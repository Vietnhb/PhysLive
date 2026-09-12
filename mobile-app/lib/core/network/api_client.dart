import 'dart:convert';
import 'package:http/http.dart' as http;
import '../storage/secure_storage.dart';

class ApiException implements Exception {
  final int statusCode;
  final String message;

  ApiException({required this.statusCode, required this.message});

  @override
  String toString() => message;
}

class ApiClient {
  final SecureStorageService _storage = SecureStorageService();

  Future<Map<String, String>> _getHeaders({bool withAuth = true}) async {
    final headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };
    if (withAuth) {
      final token = await _storage.getToken();
      if (token != null && token.isNotEmpty) {
        headers['Authorization'] = 'Bearer $token';
      }
    }
    return headers;
  }

  Future<dynamic> get(String url, {bool withAuth = true}) async {
    final headers = await _getHeaders(withAuth: withAuth);
    final response = await http.get(Uri.parse(url), headers: headers);
    return _handleResponse(response);
  }

  Future<dynamic> post(String url, Map<String, dynamic> body, {bool withAuth = true}) async {
    final headers = await _getHeaders(withAuth: withAuth);
    final response = await http.post(
      Uri.parse(url),
      headers: headers,
      body: jsonEncode(body),
    );
    return _handleResponse(response);
  }

  Future<dynamic> put(String url, Map<String, dynamic> body, {bool withAuth = true}) async {
    final headers = await _getHeaders(withAuth: withAuth);
    final response = await http.put(
      Uri.parse(url),
      headers: headers,
      body: jsonEncode(body),
    );
    return _handleResponse(response);
  }

  Future<dynamic> patch(String url, Map<String, dynamic> body, {bool withAuth = true}) async {
    final headers = await _getHeaders(withAuth: withAuth);
    final response = await http.patch(
      Uri.parse(url),
      headers: headers,
      body: jsonEncode(body),
    );
    return _handleResponse(response);
  }

  Future<dynamic> delete(String url, {bool withAuth = true}) async {
    final headers = await _getHeaders(withAuth: withAuth);
    final response = await http.delete(Uri.parse(url), headers: headers);
    return _handleResponse(response);
  }

  dynamic _handleResponse(http.Response response) {
    final statusCode = response.statusCode;
    final bodyString = utf8.decode(response.bodyBytes);

    if (statusCode >= 200 && statusCode < 300) {
      if (bodyString.isEmpty) return null;
      try {
        return jsonDecode(bodyString);
      } catch (_) {
        return bodyString;
      }
    }

    // Try parsing backend JSON ErrorResponse
    String errorMessage = 'Yêu cầu thất bại (Mã lỗi $statusCode)';
    try {
      final json = jsonDecode(bodyString);
      if (json is Map && json.containsKey('message')) {
        errorMessage = json['message'].toString();
      }
    } catch (_) {
      if (bodyString.isNotEmpty) {
        errorMessage = bodyString;
      }
    }

    throw ApiException(statusCode: statusCode, message: errorMessage);
  }
}
