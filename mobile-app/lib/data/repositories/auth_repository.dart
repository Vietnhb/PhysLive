import '../../core/constants/api_constants.dart';
import '../../core/network/api_client.dart';
import '../../core/storage/secure_storage.dart';
import '../models/auth_response.dart';
import '../models/user_model.dart';

class AuthRepository {
  final ApiClient _apiClient = ApiClient();
  final SecureStorageService _storage = SecureStorageService();

  Future<AuthResponseModel> login(String email, String password) async {
    final response = await _apiClient.post(
      ApiConstants.login,
      {
        'email': email.trim(),
        'password': password,
      },
      withAuth: false,
    );

    final authResponse = AuthResponseModel.fromJson(response as Map<String, dynamic>);

    await _storage.saveAuthData(
      token: authResponse.token,
      email: authResponse.user.email,
      role: authResponse.user.role ?? 'GUEST',
    );

    return authResponse;
  }

  Future<void> signup(String email, String fullName, String password) async {
    await _apiClient.post(
      ApiConstants.signup,
      {
        'email': email.trim(),
        'fullName': fullName.trim(),
        'password': password,
      },
      withAuth: false,
    );
  }

  Future<bool> isLoggedIn() async {
    final token = await _storage.getToken();
    return token != null && token.isNotEmpty;
  }

  Future<void> logout() async {
    await _storage.clearAuthData();
  }

  Future<String?> getCurrentUserRole() async {
    return await _storage.getRole();
  }
}
