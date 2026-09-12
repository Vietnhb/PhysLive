import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_theme.dart';
import '../../viewmodels/simulation_viewmodel.dart';

class SimulationPlayerView extends StatefulWidget {
  const SimulationPlayerView({super.key});

  @override
  State<SimulationPlayerView> createState() => _SimulationPlayerViewState();
}

class _SimulationPlayerViewState extends State<SimulationPlayerView> {
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    // 30 FPS tick loop for simulation animation
    _timer = Timer.periodic(const Duration(milliseconds: 33), (_) {
      final sim = context.read<SimulationViewModel>();
      if (sim.isPlaying) {
        sim.stepForward(0.05);
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final sim = context.watch<SimulationViewModel>();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Mô phỏng Ném xiên'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Đặt lại',
            onPressed: () => sim.resetAnimation(),
          ),
        ],
      ),
      body: Column(
        children: [
          // Canvas rendering area
          Expanded(
            flex: 5,
            child: Container(
              margin: const EdgeInsets.all(16.0),
              decoration: BoxDecoration(
                color: const Color(0xFF0F172A),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: AppTheme.primaryBlue.withOpacity(0.3)),
              ),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(16),
                child: CustomPaint(
                  painter: ProjectilePainter(
                    trajectory: sim.trajectoryPoints,
                    currentPos: sim.currentPosition,
                    maxRange: sim.maxRange,
                    maxHeight: sim.maxHeight,
                  ),
                  child: Container(),
                ),
              ),
            ),
          ),

          // Real-time Physics Metrics Banner
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16.0),
            child: Row(
              children: [
                _buildMetricCard('Tầm xa (L)', '${sim.maxRange.toStringAsFixed(1)} m'),
                const SizedBox(width: 8),
                _buildMetricCard('Độ cao (H)', '${sim.maxHeight.toStringAsFixed(1)} m'),
                const SizedBox(width: 8),
                _buildMetricCard('Thời gian (T)', '${sim.totalTime.toStringAsFixed(1)} s'),
                const SizedBox(width: 8),
                _buildMetricCard(
                  'Toạ độ (x, y)',
                  '(${sim.currentPosition.x.toStringAsFixed(0)}, ${sim.currentPosition.y.toStringAsFixed(0)})',
                ),
              ],
            ),
          ),

          // Interactive Sliders & Animation Controls
          Expanded(
            flex: 4,
            child: Container(
              padding: const EdgeInsets.all(16.0),
              decoration: const BoxDecoration(
                color: AppTheme.surfaceColor,
                borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
              ),
              child: ListView(
                children: [
                  // Play / Pause / Reset buttons
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      ElevatedButton.icon(
                        onPressed: () {
                          if (sim.isPlaying) {
                            sim.pause();
                          } else {
                            sim.play();
                          }
                        },
                        icon: Icon(sim.isPlaying ? Icons.pause : Icons.play_arrow),
                        label: Text(sim.isPlaying ? 'Tạm dừng' : 'Bắt đầu'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: sim.isPlaying ? Colors.amber[700] : AppTheme.primaryBlue,
                        ),
                      ),
                      const SizedBox(width: 14),
                      OutlinedButton.icon(
                        onPressed: () => sim.resetAnimation(),
                        icon: const Icon(Icons.replay),
                        label: const Text('Đặt lại'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 14),

                  // Slider 1: Velocity (v0)
                  Text(
                    'Vận tốc ban đầu v₀: ${sim.velocity.toStringAsFixed(0)} m/s',
                    style: const TextStyle(fontWeight: FontWeight.w600, color: AppTheme.textColor),
                  ),
                  Slider(
                    value: sim.velocity,
                    min: 10,
                    max: 50,
                    divisions: 40,
                    activeColor: AppTheme.secondaryTeal,
                    onChanged: (val) => sim.updateVelocity(val),
                  ),

                  // Slider 2: Angle (alpha)
                  Text(
                    'Góc ném α: ${sim.angle.toStringAsFixed(0)}°',
                    style: const TextStyle(fontWeight: FontWeight.w600, color: AppTheme.textColor),
                  ),
                  Slider(
                    value: sim.angle,
                    min: 15,
                    max: 85,
                    divisions: 70,
                    activeColor: AppTheme.primaryBlue,
                    onChanged: (val) => sim.updateAngle(val),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMetricCard(String title, String value) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 6),
        decoration: BoxDecoration(
          color: AppTheme.surfaceColor,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.white.withOpacity(0.06)),
        ),
        child: Column(
          children: [
            Text(title, style: const TextStyle(fontSize: 10, color: AppTheme.textMuted)),
            const SizedBox(height: 2),
            Text(
              value,
              style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppTheme.secondaryTeal),
            ),
          ],
        ),
      ),
    );
  }
}

// ------------------------------------------------------------------ //
// CustomPainter for Canvas Physics Trajectory
// ------------------------------------------------------------------ //

class ProjectilePainter extends CustomPainter {
  final List<Point2D> trajectory;
  final Point2D currentPos;
  final double maxRange;
  final double maxHeight;

  ProjectilePainter({
    required this.trajectory,
    required this.currentPos,
    required this.maxRange,
    required this.maxHeight,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final padding = 30.0;
    final w = size.width - 2 * padding;
    final h = size.height - 2 * padding;

    // Scaling factors to fit canvas
    final scaleX = maxRange > 0 ? w / (maxRange * 1.1) : 1.0;
    final scaleY = maxHeight > 0 ? h / (maxHeight * 1.3) : 1.0;

    // Coordinate translation: origin at bottom-left
    double toCanvasX(double x) => padding + x * scaleX;
    double toCanvasY(double y) => size.height - padding - y * scaleY;

    // 1. Draw Grid axes
    final axisPaint = Paint()
      ..color = const Color(0xFF334155)
      ..strokeWidth = 1.5;

    // X-axis (ground)
    canvas.drawLine(
      Offset(padding - 10, size.height - padding),
      Offset(size.width - padding + 10, size.height - padding),
      axisPaint,
    );
    // Y-axis (height)
    canvas.drawLine(
      Offset(padding, size.height - padding + 10),
      Offset(padding, padding - 10),
      axisPaint,
    );

    // 2. Draw Trajectory Path
    if (trajectory.isNotEmpty) {
      final pathPaint = Paint()
        ..color = const Color(0xFF38BDF8)
        ..strokeWidth = 2.0
        ..style = PaintingStyle.stroke;

      final path = Path();
      path.moveTo(toCanvasX(trajectory.first.x), toCanvasY(trajectory.first.y));
      for (final pt in trajectory) {
        path.lineTo(toCanvasX(pt.x), toCanvasY(pt.y));
      }
      canvas.drawPath(path, pathPaint);
    }

    // 3. Draw Moving Projectile (Ball)
    final ballX = toCanvasX(currentPos.x);
    final ballY = toCanvasY(currentPos.y);

    final ballGlow = Paint()
      ..color = const Color(0xFFF59E0B).withOpacity(0.35)
      ..style = PaintingStyle.fill;
    canvas.drawCircle(Offset(ballX, ballY), 12, ballGlow);

    final ballPaint = Paint()
      ..color = const Color(0xFFF59E0B)
      ..style = PaintingStyle.fill;
    canvas.drawCircle(Offset(ballX, ballY), 7, ballPaint);
  }

  @override
  bool shouldRepaint(covariant ProjectilePainter oldDelegate) => true;
}
