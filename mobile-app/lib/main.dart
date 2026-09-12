import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'core/theme/app_theme.dart';
import 'viewmodels/auth_viewmodel.dart';
import 'viewmodels/simulation_viewmodel.dart';
import 'viewmodels/user_viewmodel.dart';
import 'views/auth/login_view.dart';
import 'views/home/home_view.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final authViewModel = AuthViewModel();
  await authViewModel.checkAuthStatus();

  runApp(
    MultiProvider(
      providers: [
        ChangeNotifierProvider<AuthViewModel>.value(value: authViewModel),
        ChangeNotifierProvider<UserViewModel>(create: (_) => UserViewModel()),
        ChangeNotifierProvider<SimulationViewModel>(create: (_) => SimulationViewModel()),
      ],
      child: const PhysLiveApp(),
    ),
  );
}

class PhysLiveApp extends StatelessWidget {
  const PhysLiveApp({super.key});

  @override
  Widget build(BuildContext context) {
    final authViewModel = context.watch<AuthViewModel>();

    return MaterialApp(
      title: 'PhysLive Mobile',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.darkTheme,
      home: authViewModel.isAuthenticated ? const HomeView() : const LoginView(),
    );
  }
}
