import 'dart:math';
import 'package:flutter/foundation.dart';
import 'base_viewmodel.dart';

class Point2D {
  final double x;
  final double y;
  Point2D(this.x, this.y);
}

class SimulationViewModel extends BaseViewModel {
  // Input parameters (Physics parameters)
  double _velocity = 25.0; // m/s (10 - 50)
  double _angle = 45.0;    // degrees (10 - 85)
  double _gravity = 9.8;   // m/s²

  // Animation state
  double _currentTime = 0.0;
  bool _isPlaying = false;

  // Getters
  double get velocity => _velocity;
  double get angle => _angle;
  double get gravity => _gravity;
  double get currentTime => _currentTime;
  bool get isPlaying => _isPlaying;

  // Physics Calculations
  double get angleInRadians => _angle * pi / 180.0;
  double get vx0 => _velocity * cos(angleInRadians);
  double get vy0 => _velocity * sin(angleInRadians);

  // Time of flight (T = 2 * vy0 / g)
  double get totalTime => (2 * vy0) / _gravity;

  // Maximum height (H = vy0² / (2g))
  double get maxHeight => (vy0 * vy0) / (2 * _gravity);

  // Range (L = vx0 * T)
  double get maxRange => vx0 * totalTime;

  // Current position (x, y) at _currentTime
  Point2D get currentPosition {
    final t = min(_currentTime, totalTime);
    final x = vx0 * t;
    final y = max(0.0, vy0 * t - 0.5 * _gravity * t * t);
    return Point2D(x, y);
  }

  // Trajectory points for complete arc
  List<Point2D> get trajectoryPoints {
    final points = <Point2D>[];
    const steps = 60;
    final dt = totalTime / steps;
    for (int i = 0; i <= steps; i++) {
      final t = i * dt;
      final x = vx0 * t;
      final y = max(0.0, vy0 * t - 0.5 * _gravity * t * t);
      points.add(Point2D(x, y));
    }
    return points;
  }

  void updateVelocity(double v) {
    _velocity = v;
    resetAnimation();
  }

  void updateAngle(double a) {
    _angle = a;
    resetAnimation();
  }

  void play() {
    _isPlaying = true;
    notifyListeners();
  }

  void pause() {
    _isPlaying = false;
    notifyListeners();
  }

  void stepForward(double dt) {
    if (!_isPlaying) return;
    _currentTime += dt;
    if (_currentTime >= totalTime) {
      _currentTime = totalTime;
      _isPlaying = false;
    }
    notifyListeners();
  }

  void resetAnimation() {
    _currentTime = 0.0;
    _isPlaying = false;
    notifyListeners();
  }
}
