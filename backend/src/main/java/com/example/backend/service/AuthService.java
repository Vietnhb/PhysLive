package com.example.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.backend.dto.LoginResponse;
import com.example.backend.dto.SignupRequest;
import com.example.backend.dto.UserResponse;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtUtil;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUntil;

    public LoginResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Không tìm thấy email"));

        if (!password.equals(user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Sai password");
        }
        String token = jwtUntil.generateToken(user.getEmail(), user.getRole().getName());
        UserResponse userInfor = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().getName());

        return new LoginResponse(token, userInfor);
    }

    public void signup(SignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email đã tồn tại");
        }
        Role role = roleRepository.findByName("GUEST")
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi server"));
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPassword(request.getPassword());
        user.setRole(role);
        userRepository.save(user);
    }
}
