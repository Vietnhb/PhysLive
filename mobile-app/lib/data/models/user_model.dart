class UserModel {
  final int id;
  final String email;
  final String fullName;
  final int? roleId;
  final String? role;
  final bool enabled;
  final bool? active;
  final String? institutionId;
  final DateTime? lastLogin;
  final String? avatarUrl;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  UserModel({
    required this.id,
    required this.email,
    required this.fullName,
    this.roleId,
    this.role,
    required this.enabled,
    this.active,
    this.institutionId,
    this.lastLogin,
    this.avatarUrl,
    this.createdAt,
    this.updatedAt,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'] as int,
      email: json['email'] as String? ?? '',
      fullName: json['fullName'] as String? ?? '',
      roleId: json['roleId'] as int?,
      role: json['role'] as String?,
      enabled: json['enabled'] as bool? ?? true,
      active: json['active'] as bool?,
      institutionId: json['institutionId'] as String?,
      lastLogin: json['lastLogin'] != null
          ? DateTime.tryParse(json['lastLogin'].toString())
          : null,
      avatarUrl: json['avatarUrl'] as String?,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
      updatedAt: json['updatedAt'] != null
          ? DateTime.tryParse(json['updatedAt'].toString())
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'email': email,
      'fullName': fullName,
      'roleId': roleId,
      'role': role,
      'enabled': enabled,
      'active': active,
      'institutionId': institutionId,
      'lastLogin': lastLogin?.toIso8601String(),
      'avatarUrl': avatarUrl,
      'createdAt': createdAt?.toIso8601String(),
      'updatedAt': updatedAt?.toIso8601String(),
    };
  }

  bool get isAdmin => role == 'ADMIN' || roleId == 1;
}
