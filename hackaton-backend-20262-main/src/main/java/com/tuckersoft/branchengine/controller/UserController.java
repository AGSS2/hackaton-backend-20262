package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.UserDtos.RoleUpdateRequest;
import com.tuckersoft.branchengine.dto.UserDtos.UserResponse;
import com.tuckersoft.branchengine.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        return userService.me(auth.getName());
    }

    @GetMapping
    public List<UserResponse> listar() {
        return userService.listar();
    }

    @PatchMapping("/{id}/role")
    public UserResponse cambiarRol(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request, Authentication auth) {
        return userService.cambiarRol(id, request, auth.getName());
    }
}