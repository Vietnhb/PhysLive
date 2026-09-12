import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/theme/app_theme.dart';
import '../../viewmodels/user_viewmodel.dart';

class AdminUserView extends StatefulWidget {
  const AdminUserView({super.key});

  @override
  State<AdminUserView> createState() => _AdminUserViewState();
}

class _AdminUserViewState extends State<AdminUserView> {
  final _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<UserViewModel>().searchUsers();
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _onSearch() {
    context.read<UserViewModel>().searchUsers(_searchController.text);
  }

  @override
  Widget build(BuildContext context) {
    final userViewModel = context.watch<UserViewModel>();
    final users = userViewModel.users;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Quản trị Người dùng (Role 1)'),
      ),
      body: Column(
        children: [
          // Search box
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _searchController,
                    style: const TextStyle(color: AppTheme.textColor),
                    decoration: InputDecoration(
                      hintText: 'Tìm theo từ khoá email (vd: teacher, gmail)...',
                      prefixIcon: const Icon(Icons.search, color: AppTheme.textMuted),
                      suffixIcon: _searchController.text.isNotEmpty
                          ? IconButton(
                              icon: const Icon(Icons.clear, color: AppTheme.textMuted),
                              onPressed: () {
                                _searchController.clear();
                                _onSearch();
                              },
                            )
                          : null,
                    ),
                    onSubmitted: (_) => _onSearch(),
                  ),
                ),
                const SizedBox(width: 8),
                ElevatedButton(
                  onPressed: _onSearch,
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                  ),
                  child: const Text('Tìm'),
                ),
              ],
            ),
          ),

          // User list
          Expanded(
            child: userViewModel.isLoading
                ? const Center(child: CircularProgressIndicator())
                : users.isEmpty
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            const Icon(Icons.people_outline, size: 64, color: AppTheme.textMuted),
                            const SizedBox(height: 12),
                            Text(
                              _searchController.text.isEmpty
                                  ? 'Chưa có người dùng nào.'
                                  : 'Không tìm thấy người dùng khớp với "${_searchController.text}"',
                              style: const TextStyle(color: AppTheme.textMuted),
                            ),
                          ],
                        ),
                      )
                    : RefreshIndicator(
                        onRefresh: () => userViewModel.searchUsers(_searchController.text),
                        child: ListView.builder(
                          padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
                          itemCount: users.length,
                          itemBuilder: (context, index) {
                            final u = users[index];
                            return Card(
                              color: AppTheme.surfaceColor,
                              margin: const EdgeInsets.only(bottom: 12),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                                side: BorderSide(
                                  color: u.enabled ? Colors.white.withOpacity(0.06) : AppTheme.errorRed.withOpacity(0.3),
                                ),
                              ),
                              child: Padding(
                                padding: const EdgeInsets.all(14.0),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Row(
                                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                      children: [
                                        Expanded(
                                          child: Text(
                                            u.fullName,
                                            style: const TextStyle(
                                              fontSize: 16,
                                              fontWeight: FontWeight.bold,
                                              color: AppTheme.textColor,
                                            ),
                                          ),
                                        ),
                                        Container(
                                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                                          decoration: BoxDecoration(
                                            color: AppTheme.primaryBlue.withOpacity(0.2),
                                            borderRadius: BorderRadius.circular(6),
                                            border: Border.all(color: AppTheme.primaryBlue.withOpacity(0.5)),
                                          ),
                                          child: Text(
                                            '${u.role ?? 'GUEST'} (ID: ${u.roleId ?? 5})',
                                            style: const TextStyle(fontSize: 11, color: AppTheme.secondaryTeal, fontWeight: FontWeight.bold),
                                          ),
                                        ),
                                      ],
                                    ),
                                    const SizedBox(height: 6),
                                    Text(
                                      u.email,
                                      style: const TextStyle(fontSize: 13, color: AppTheme.textMuted),
                                    ),
                                    const Divider(color: Color(0xFF334155), height: 20),
                                    Row(
                                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                      children: [
                                        Row(
                                          children: [
                                            Text(
                                              u.enabled ? 'Đang hoạt động' : 'Đã khoá',
                                              style: TextStyle(
                                                fontSize: 13,
                                                fontWeight: FontWeight.w600,
                                                color: u.enabled ? AppTheme.successGreen : AppTheme.errorRed,
                                              ),
                                            ),
                                            const SizedBox(width: 8),
                                            Switch(
                                              value: u.enabled,
                                              activeColor: AppTheme.successGreen,
                                              onChanged: (val) {
                                                userViewModel.toggleAccountState(u.id, u.enabled);
                                              },
                                            ),
                                          ],
                                        ),
                                        IconButton(
                                          icon: const Icon(Icons.delete_outline, color: AppTheme.errorRed),
                                          tooltip: 'Xoá tài khoản',
                                          onPressed: () {
                                            showDialog(
                                              context: context,
                                              builder: (ctx) => AlertDialog(
                                                backgroundColor: AppTheme.surfaceColor,
                                                title: const Text('Xác nhận xoá', style: TextStyle(color: AppTheme.textColor)),
                                                content: Text(
                                                  'Bạn có chắc muốn xoá hoặc vô hiệu hoá người dùng "${u.email}"?',
                                                  style: const TextStyle(color: AppTheme.textMuted),
                                                ),
                                                actions: [
                                                  TextButton(
                                                    onPressed: () => Navigator.pop(ctx),
                                                    child: const Text('Huỷ', style: TextStyle(color: AppTheme.textMuted)),
                                                  ),
                                                  ElevatedButton(
                                                    style: ElevatedButton.styleFrom(backgroundColor: AppTheme.errorRed),
                                                    onPressed: () async {
                                                      Navigator.pop(ctx);
                                                      final ok = await userViewModel.deleteUser(u.id);
                                                      if (ok && context.mounted) {
                                                        ScaffoldMessenger.of(context).showSnackBar(
                                                          const SnackBar(
                                                            content: Text('Xoá thành công'),
                                                            backgroundColor: AppTheme.successGreen,
                                                          ),
                                                        );
                                                      }
                                                    },
                                                    child: const Text('Xoá'),
                                                  ),
                                                ],
                                              ),
                                            );
                                          },
                                        ),
                                      ],
                                    ),
                                  ],
                                ),
                              ),
                            );
                          },
                        ),
                      ),
          ),
        ],
      ),
    );
  }
}
