import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:webview_flutter_android/webview_flutter_android.dart';
import 'web_policy.dart';

void main() => runApp(const PhysLiveApp());

class PhysLiveApp extends StatelessWidget {
  const PhysLiveApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'PhysLive',
    debugShowCheckedModeBanner: false,
    theme: ThemeData(
      colorSchemeSeed: const Color(0xff3155d6),
      useMaterial3: true,
    ),
    home: const DeploymentPage(),
  );
}

class DeploymentPage extends StatefulWidget {
  const DeploymentPage({super.key});
  @override
  State<DeploymentPage> createState() => _DeploymentPageState();
}

class _DeploymentPageState extends State<DeploymentPage> {
  final address = TextEditingController(
    text: const String.fromEnvironment('PHYSLIVE_WEB_URL'),
  );
  String? error;
  @override
  void initState() {
    super.initState();
    restore();
  }

  Future<void> restore() async {
    final prefs = await SharedPreferences.getInstance();
    if (!mounted) return;
    final saved = prefs.getString('deployment');
    if (address.text.isEmpty && saved != null) address.text = saved;
    if (deploymentUrl(address.text) != null) await open();
  }

  Future<void> open() async {
    final uri = deploymentUrl(address.text);
    if (uri == null) {
      setState(() => error = 'Nhập địa chỉ HTTPS của PhysLive.');
      return;
    }
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('deployment', uri.toString());
    if (!mounted) return;
    await Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => WorkspacePage(deployment: uri)),
    );
  }

  @override
  void dispose() {
    address.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('PhysLive')),
    body: Center(
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 440),
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('Kết nối trường học', style: TextStyle(fontSize: 24)),
              const SizedBox(height: 16),
              TextField(
                controller: address,
                keyboardType: TextInputType.url,
                autocorrect: false,
                decoration: InputDecoration(
                  labelText: 'Địa chỉ PhysLive',
                  hintText: 'https://…',
                  errorText: error,
                ),
                onSubmitted: (_) => open(),
              ),
              const SizedBox(height: 16),
              FilledButton(onPressed: open, child: const Text('Tiếp tục')),
            ],
          ),
        ),
      ),
    ),
  );
}

class WorkspacePage extends StatefulWidget {
  final Uri deployment;
  const WorkspacePage({super.key, required this.deployment});
  @override
  State<WorkspacePage> createState() => _WorkspacePageState();
}

class _WorkspacePageState extends State<WorkspacePage>
    with WidgetsBindingObserver {
  late final WebViewController controller;
  int progress = 0;
  String? error;
  bool sharing = false, external = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(Colors.white)
      ..addJavaScriptChannel(
        'PhysLiveDownload',
        onMessageReceived: (message) => shareExport(message.message),
      )
      ..setNavigationDelegate(
        NavigationDelegate(
          onProgress: (value) {
            if (mounted) setState(() => progress = value);
          },
          onPageStarted: (_) {
            if (mounted) setState(() => error = null);
          },
          onWebResourceError: (failure) {
            if (failure.isForMainFrame == true && mounted) {
              setState(
                () => error = 'Không thể kết nối. Kiểm tra mạng rồi thử lại.',
              );
            }
          },
          onNavigationRequest: (request) {
            final target = Uri.tryParse(request.url);
            if (target == null) return NavigationDecision.prevent;
            if (!request.isMainFrame) {
              return {'about', 'blob', 'data'}.contains(target.scheme)
                  ? NavigationDecision.navigate
                  : NavigationDecision.prevent;
            }
            if (sameDeployment(widget.deployment, target)) {
              return NavigationDecision.navigate;
            }
            if (target.scheme == 'https') {
              external = true;
              launchUrl(target, mode: LaunchMode.externalApplication);
            }
            return NavigationDecision.prevent;
          },
        ),
      )
      ..loadRequest(widget.deployment);
    final platform = controller.platform;
    if (platform is AndroidWebViewController) {
      platform.setMediaPlaybackRequiresUserGesture(false);
      platform.setOnShowFileSelector(selectImage);
    }
  }

  Future<List<String>> selectImage(FileSelectorParams params) async {
    if (!mounted) return [];
    final source = params.isCaptureEnabled
        ? ImageSource.camera
        : await showModalBottomSheet<ImageSource>(
            context: context,
            builder: (context) => SafeArea(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  ListTile(
                    leading: const Icon(Icons.camera_alt),
                    title: const Text('Chụp ảnh'),
                    onTap: () => Navigator.pop(context, ImageSource.camera),
                  ),
                  ListTile(
                    leading: const Icon(Icons.photo),
                    title: const Text('Chọn ảnh'),
                    onTap: () => Navigator.pop(context, ImageSource.gallery),
                  ),
                ],
              ),
            ),
          );
    if (source == null) return [];
    try {
      final image = await ImagePicker().pickImage(source: source);
      return image == null ? [] : [Uri.file(image.path).toString()];
    } catch (_) {
      notice('Không thể mở ảnh. Vui lòng kiểm tra quyền truy cập.');
      return [];
    }
  }

  Future<void> shareExport(String message) async {
    if (sharing || message.length > 70 * 1024 * 1024) return;
    sharing = true;
    try {
      final current = Uri.tryParse(await controller.currentUrl() ?? '');
      if (current == null || !sameDeployment(widget.deployment, current)) {
        return;
      }
      final value = jsonDecode(message) as Map<String, dynamic>;
      final name = value['name'] as String,
          bytes = base64Decode(value['data'] as String);
      if (!allowedExport(name, bytes.length)) {
        throw const FormatException('Invalid export');
      }
      final folder = await getTemporaryDirectory();
      final file = File('${folder.path}/$name');
      await file.writeAsBytes(bytes, flush: true);
      await SharePlus.instance.share(
        ShareParams(files: [XFile(file.path)], title: 'PhysLive'),
      );
    } catch (_) {
      notice('Không thể chia sẻ file. Vui lòng thử lại.');
    } finally {
      sharing = false;
    }
  }

  void notice(String text) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(text)));
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && external) {
      external = false;
      controller.reload();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => PopScope(
    canPop: false,
    onPopInvokedWithResult: (popped, result) async {
      if (popped) return;
      if (await controller.canGoBack()) {
        await controller.goBack();
      } else if (context.mounted) {
        Navigator.of(context).pop();
      }
    },
    child: Scaffold(
      appBar: AppBar(
        title: const Text('PhysLive'),
        automaticallyImplyLeading: false,
        actions: [
          IconButton(
            tooltip: 'Đổi địa chỉ',
            icon: const Icon(Icons.home),
            onPressed: () => Navigator.of(context).pop(),
          ),
          IconButton(
            tooltip: 'Tải lại',
            icon: const Icon(Icons.refresh),
            onPressed: controller.reload,
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            if (progress < 100 && error == null)
              LinearProgressIndicator(value: progress / 100),
            if (error != null)
              MaterialBanner(
                content: Text(error!),
                actions: [
                  TextButton(
                    onPressed: controller.reload,
                    child: const Text('Thử lại'),
                  ),
                ],
              ),
            Expanded(child: WebViewWidget(controller: controller)),
          ],
        ),
      ),
    ),
  );
}
