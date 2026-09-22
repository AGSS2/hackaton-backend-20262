package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.AuthDtos.AuthResponse;
import com.tuckersoft.branchengine.dto.AuthDtos.LoginRequest;
import com.tuckersoft.branchengine.dto.AuthDtos.RegisterRequest;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.repository.UserRepository;
import com.tuckersoft.branchengine.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Ese email ya esta registrado");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setRole("ROLE_USER");
        user.setCreatedAt(Instant.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, "Bearer", user.getEmail(), user.getDisplayName(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email o contrasena incorrectos");
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email o contrasena incorrectos");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email o contrasena incorrectos"));

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, "Bearer", user.getEmail(), user.getDisplayName(), user.getRole());
    }
}