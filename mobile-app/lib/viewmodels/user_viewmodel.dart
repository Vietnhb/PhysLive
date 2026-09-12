import '../data/models/user_model.dart';
import '../data/repositories/user_repository.dart';
import 'base_viewmodel.dart';

class UserViewModel extends BaseViewModel {
  final UserRepository _userRepository = UserRepository();

  UserModel? _profile;
  List<UserModel> _users = [];
  String _searchKeyword = '';

  UserModel? get profile => _profile;
  List<UserModel> get users => _users;
  String get searchKeyword => _searchKeyword;

  // Load profile of current user (/api/user/me)
  Future<void> loadProfile() async {
    setState(ViewState.loading);
    try {
      _profile = await _userRepository.getMe();
      setState(ViewState.success);
    } catch (e) {
      setError(e.toString());
    }
  }

  // Update current user's profile
  Future<bool> updateProfile({
    String? fullName,
    String? institutionId,
    String? avatarUrl,
    String? password,
  }) async {
    if (_profile == null) return false;

    setState(ViewState.loading);
    try {
      _profile = await _userRepository.updateMyProfile(
        fullName: fullName,
        institutionId: institutionId,
        avatarUrl: avatarUrl,
        password: password,
      );
      setState(ViewState.success);
      return true;
    } catch (e) {
      setError(e.toString());
      return false;
    }
  }

  // ADMIN: Load or search users by email keyword
  Future<void> searchUsers([String? keyword]) async {
    _searchKeyword = keyword ?? '';
    setState(ViewState.loading);
    try {
      _users = await _userRepository.getUsers(searchEmail: _searchKeyword);
      setState(ViewState.success);
    } catch (e) {
      setError(e.toString());
    }
  }

  // ADMIN: Toggle account state (enabled)
  Future<bool> toggleAccountState(int userId, bool currentEnabled) async {
    try {
      final updated = await _userRepository.updateAccountState(userId, !currentEnabled);
      final index = _users.indexWhere((u) => u.id == userId);
      if (index != -1) {
        _users[index] = updated;
        notifyListeners();
      }
      return true;
    } catch (e) {
      setError(e.toString());
      return false;
    }
  }

  // ADMIN: Delete user
  Future<bool> deleteUser(int userId) async {
    try {
      await _userRepository.deleteUser(userId);
      _users.removeWhere((u) => u.id == userId);
      notifyListeners();
      return true;
    } catch (e) {
      setError(e.toString());
      return false;
    }
  }
}
