package com.splitpay.service;

import com.splitpay.dto.auth.AuthResponse;
import com.splitpay.dto.auth.LoginRequest;
import com.splitpay.dto.auth.RegisterRequest;
import com.splitpay.dto.auth.UserSummaryDto;
import com.splitpay.entity.Role;
import com.splitpay.entity.User;
import com.splitpay.exception.DuplicateResourceException;
import com.splitpay.exception.ResourceNotFoundException;
import com.splitpay.repository.UserRepository;
import com.splitpay.security.JwtTokenProvider;
import com.splitpay.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("An account with email " + normalizedEmail + " already exists");
        }

        Role role = request.getRole() != null ? request.getRole() : Role.USER;

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(role)
                .businessName(request.getBusinessName() != null ? request.getBusinessName().trim() : null)
                .defaultUpiId(request.getDefaultUpiId() != null ? request.getDefaultUpiId().trim() : null)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user: [id={}, email={}, role={}]", savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        UserPrincipal principal = UserPrincipal.create(savedUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        String jwt = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .token(jwt)
                .expiresInMs(tokenProvider.getTokenExpirationMs())
                .user(toSummaryDto(savedUser))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        log.info("User logged in successfully: [id={}, email={}]", user.getId(), user.getEmail());

        return AuthResponse.builder()
                .token(jwt)
                .expiresInMs(tokenProvider.getTokenExpirationMs())
                .user(toSummaryDto(user))
                .build();
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return toSummaryDto(user);
    }

    private UserSummaryDto toSummaryDto(User user) {
        return UserSummaryDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .businessName(user.getBusinessName())
                .defaultUpiId(user.getDefaultUpiId())
                .build();
    }
}
