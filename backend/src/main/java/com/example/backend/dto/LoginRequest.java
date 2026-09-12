package com.example.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Email không đúng định dạng (phải chứa ký tự '@' và tên miền hợp lệ)")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
