package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.UserDtos.RoleUpdateRequest;
import com.tuckersoft.branchengine.dto.UserDtos.UserResponse;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private static final Set<String> ROLES_VALIDOS = Set.of("ROLE_USER", "ROLE_ADMIN");

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User obtenerPorEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Usuario no encontrado"));
    }

    public UserResponse me(String email) {
        return aDto(obtenerPorEmail(email));
    }

    public List<UserResponse> listar() {
        return userRepository.findAll().stream().map(this::aDto).toList();
    }

    public UserResponse cambiarRol(Long id, RoleUpdateRequest request, String emailDelAdminActual) {
        if (!ROLES_VALIDOS.contains(request.role())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROLE", "El rol debe ser ROLE_USER o ROLE_ADMIN");
        }

        User objetivo = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuario no encontrado"));

        User actual = obtenerPorEmail(emailDelAdminActual);
        if (actual.getId().equals(objetivo.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SELF_ROLE_CHANGE", "No puedes cambiar tu propio rol");
        }

        objetivo.setRole(request.role());
        userRepository.save(objetivo);
        return aDto(objetivo);
    }

    private UserResponse aDto(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getCreatedAt());
    }
}