import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_theme.dart';
import '../../viewmodels/user_viewmodel.dart';

class ProfileView extends StatelessWidget {
  const ProfileView({super.key});

  void _showEditDialog(BuildContext context) {
    final userViewModel = context.read<UserViewModel>();
    final user = userViewModel.profile;

    final nameController = TextEditingController(text: user?.fullName ?? '');
    final instController = TextEditingController(text: user?.institutionId ?? '');
    final passController = TextEditingController();

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.surfaceColor,
        title: const Text('Cập nhật thông tin', style: TextStyle(color: AppTheme.textColor)),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: nameController,
                style: const TextStyle(color: AppTheme.textColor),
                decoration: const InputDecoration(labelText: 'Họ và tên'),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: instController,
                style: const TextStyle(color: AppTheme.textColor),
                decoration: const InputDecoration(labelText: 'Trường / Tổ chức'),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: passController,
                obscureText: true,
                style: const TextStyle(color: AppTheme.textColor),
                decoration: const InputDecoration(
                  labelText: 'Mật khẩu mới (để trống nếu không đổi)',
                ),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Huỷ', style: TextStyle(color: AppTheme.textMuted)),
          ),
          ElevatedButton(
            onPressed: () async {
              final success = await userViewModel.updateProfile(
                fullName: nameController.text.trim(),
                institutionId: instController.text.trim(),
                password: passController.text.isNotEmpty ? passController.text : null,
              );
              if (success && ctx.mounted) {
                Navigator.pop(ctx);
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Cập nhật thông tin thành công!'),
                    backgroundColor: AppTheme.successGreen,
                  ),
                );
              }
            },
            child: const Text('Lưu'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final userViewModel = context.watch<UserViewModel>();
    final user = userViewModel.profile;
    final dateFormat = DateFormat('dd/MM/yyyy HH:mm:ss');

    return Scaffold(
      appBar: AppBar(
        title: const Text('Hồ sơ tài khoản'),
        actions: [
          IconButton(
            icon: const Icon(Icons.edit_outlined),
            tooltip: 'Chỉnh sửa',
            onPressed: () => _showEditDialog(context),
          ),
        ],
      ),
      body: user == null
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(20.0),
              children: [
                Center(
                  child: CircleAvatar(
                    radius: 40,
                    backgroundColor: AppTheme.primaryBlue,
                    child: Text(
                      user.fullName.isNotEmpty ? user.fullName[0].toUpperCase() : 'U',
                      style: const TextStyle(fontSize: 32, fontWeight: FontWeight.bold, color: Colors.white),
                    ),
                  ),
                ),
                const SizedBox(height: 14),
                Center(
                  child: Text(
                    user.fullName,
                    style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppTheme.textColor),
                  ),
                ),
                Center(
                  child: Text(
                    user.email,
                    style: const TextStyle(fontSize: 14, color: AppTheme.textMuted),
                  ),
                ),
                const SizedBox(height: 24),

                _buildInfoCard([
                  _buildRow('ID', user.id.toString()),
                  _buildRow('Vai trò (Role)', user.role ?? 'GUEST'),
                  _buildRow('Mã vai trò (RoleId)', user.roleId?.toString() ?? 'N/A'),
                  _buildRow('Trường học (Institution)', user.institutionId ?? 'Chưa gán'),
                  _buildRow(
                    'Trạng thái tài khoản',
                    user.enabled ? 'Đang hoạt động (Enabled)' : 'Đã vô hiệu hoá (Disabled)',
                    valueColor: user.enabled ? AppTheme.successGreen : AppTheme.errorRed,
                  ),
                  _buildRow(
                    'Đăng nhập gần nhất',
                    user.lastLogin != null ? dateFormat.format(user.lastLogin!.toLocal()) : 'Chưa đăng nhập',
                  ),
                ]),
              ],
            ),
    );
  }

  Widget _buildInfoCard(List<Widget> rows) {
    return Card(
      color: AppTheme.surfaceColor,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: BorderSide(color: Colors.white.withOpacity(0.08)),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 12.0),
        child: Column(children: rows),
      ),
    );
  }

  Widget _buildRow(String label, String value, {Color? valueColor}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(fontSize: 14, color: AppTheme.textMuted)),
          const SizedBox(width: 16),
          Expanded(
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: valueColor ?? AppTheme.textColor,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
