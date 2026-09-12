import 'package:flutter/foundation.dart';

enum ViewState { initial, loading, success, error }

class BaseViewModel extends ChangeNotifier {
  ViewState _state = ViewState.initial;
  String? _errorMessage;

  ViewState get state => _state;
  String? get errorMessage => _errorMessage;

  bool get isLoading => _state == ViewState.loading;
  bool get hasError => _state == ViewState.error;
  bool get isSuccess => _state == ViewState.success;

  void setState(ViewState newState) {
    _state = newState;
    notifyListeners();
  }

  void setError(String message) {
    _errorMessage = message;
    _state = ViewState.error;
    notifyListeners();
  }

  void clearError() {
    _errorMessage = null;
    if (_state == ViewState.error) {
      _state = ViewState.initial;
    }
    notifyListeners();
  }
}
