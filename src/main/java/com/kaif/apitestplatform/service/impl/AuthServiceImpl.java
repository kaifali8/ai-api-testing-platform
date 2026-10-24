package com.kaif.apitestplatform.service.impl;

import com.kaif.apitestplatform.dto.request.LoginRequest;
import com.kaif.apitestplatform.dto.request.RegisterRequest;
import com.kaif.apitestplatform.dto.response.AuthResponse;
import com.kaif.apitestplatform.entity.User;
import com.kaif.apitestplatform.enums.Role;
import com.kaif.apitestplatform.exception.EmailAlreadyExistsException;
import com.kaif.apitestplatform.repository.UserRepository;
import com.kaif.apitestplatform.security.JwtUtil;
import com.kaif.apitestplatform.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthResponse register(RegisterRequest request) throws AccessDeniedException {
        if (request.getRole() == Role.ADMIN){
            throw new AccessDeniedException("Cannot self register as ADMIN");
        }

        if (userRepository.existsByEmail(request.getEmail())){
            throw new EmailAlreadyExistsException(
                    "Email already registered: "+request.getEmail()
            );
        }

        User user= User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        User user=userRepository.findByEmail(request.getEmail()).orElseThrow();

        String token=jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
