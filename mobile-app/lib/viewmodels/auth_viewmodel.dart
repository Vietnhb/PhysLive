import '../data/models/auth_response.dart';
import '../data/models/user_model.dart';
import '../data/repositories/auth_repository.dart';
import 'base_viewmodel.dart';

class AuthViewModel extends BaseViewModel {
  final AuthRepository _authRepository = AuthRepository();

  UserModel? _currentUser;
  bool _isAuthenticated = false;

  UserModel? get currentUser => _currentUser;
  bool get isAuthenticated => _isAuthenticated;

  // Validate email format with @ and valid domain
  bool isValidEmail(String email) {
    final emailRegex = RegExp(r'^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');
    return emailRegex.hasMatch(email.trim());
  }

  Future<bool> login(String email, String password) async {
    if (!isValidEmail(email)) {
      setError('Email không đúng định dạng (phải chứa ký tự @ và tên miền hợp lệ)');
      return false;
    }
    if (password.isEmpty) {
      setError('Mật khẩu không được để trống');
      return false;
    }

    setState(ViewState.loading);
    try {
      final AuthResponseModel response = await _authRepository.login(email, password);
      _currentUser = response.user;
      _isAuthenticated = true;
      setState(ViewState.success);
      return true;
    } catch (e) {
      setError(e.toString());
      return false;
    }
  }

  Future<bool> signup(String email, String fullName, String password) async {
    if (!isValidEmail(email)) {
      setError('Email không đúng định dạng (phải chứa ký tự @ và tên miền hợp lệ)');
      return false;
    }
    if (fullName.trim().isEmpty) {
      setError('Họ và tên không được để trống');
      return false;
    }
    if (password.length < 6) {
      setError('Mật khẩu phải có ít nhất 6 ký tự');
      return false;
    }

    setState(ViewState.loading);
    try {
      await _authRepository.signup(email, fullName, password);
      setState(ViewState.success);
      return true;
    } catch (e) {
      setError(e.toString());
      return false;
    }
  }

  Future<void> checkAuthStatus() async {
    final loggedIn = await _authRepository.isLoggedIn();
    _isAuthenticated = loggedIn;
    notifyListeners();
  }

  Future<void> logout() async {
    await _authRepository.logout();
    _currentUser = null;
    _isAuthenticated = false;
    setState(ViewState.initial);
  }
}
