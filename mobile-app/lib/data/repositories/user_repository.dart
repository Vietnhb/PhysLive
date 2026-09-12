import '../../core/constants/api_constants.dart';
import '../../core/network/api_client.dart';
import '../models/user_model.dart';

class UserRepository {
  final ApiClient _apiClient = ApiClient();

  Future<UserModel> getMe() async {
    final response = await _apiClient.get(ApiConstants.userMe);
    return UserModel.fromJson(response as Map<String, dynamic>);
  }

  Future<UserModel> updateMyProfile({
    String? fullName,
    String? institutionId,
    String? avatarUrl,
    String? password,
  }) async {
    final Map<String, dynamic> body = {};
    if (fullName != null) body['fullName'] = fullName.trim();
    if (institutionId != null) body['institutionId'] = institutionId.trim();
    if (avatarUrl != null) body['avatarUrl'] = avatarUrl.trim();
    if (password != null && password.isNotEmpty) body['password'] = password;

    final response = await _apiClient.put(ApiConstants.userMe, body);
    return UserModel.fromJson(response as Map<String, dynamic>);
  }

  // Admin methods
  Future<List<UserModel>> getUsers({String? searchEmail}) async {
    String url = ApiConstants.adminUsers;
    if (searchEmail != null && searchEmail.trim().isNotEmpty) {
      url += '?email=${Uri.encodeComponent(searchEmail.trim())}';
    }

    final response = await _apiClient.get(url);
    final list = response as List<dynamic>;
    return list.map((item) => UserModel.fromJson(item as Map<String, dynamic>)).toList();
  }

  Future<UserModel> getUserById(int id) async {
    final response = await _apiClient.get(ApiConstants.adminUserById(id));
    return UserModel.fromJson(response as Map<String, dynamic>);
  }

  Future<UserModel> updateUser({
    required int id,
    String? fullName,
    String? institutionId,
    String? avatarUrl,
    String? password,
    int? roleId,
    bool? enabled,
  }) async {
    final Map<String, dynamic> body = {};
    if (fullName != null) body['fullName'] = fullName.trim();
    if (institutionId != null) body['institutionId'] = institutionId.trim();
    if (avatarUrl != null) body['avatarUrl'] = avatarUrl.trim();
    if (password != null && password.isNotEmpty) body['password'] = password;
    if (roleId != null) body['roleId'] = roleId;
    if (enabled != null) body['enabled'] = enabled;

    final response = await _apiClient.put(ApiConstants.adminUserById(id), body);
    return UserModel.fromJson(response as Map<String, dynamic>);
  }

  Future<UserModel> updateAccountState(int id, bool enabled) async {
    final response = await _apiClient.patch(
      ApiConstants.adminUserState(id),
      {'enabled': enabled},
    );
    return UserModel.fromJson(response as Map<String, dynamic>);
  }

  Future<void> deleteUser(int id) async {
    await _apiClient.delete(ApiConstants.adminUserById(id));
  }
}
