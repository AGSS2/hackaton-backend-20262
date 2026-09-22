package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.PlaythroughDtos.PathResponse;
import com.tuckersoft.branchengine.dto.PlaythroughDtos.PlaythroughRequest;
import com.tuckersoft.branchengine.dto.PlaythroughDtos.PlaythroughResponse;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.service.PlaythroughService;
import com.tuckersoft.branchengine.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/playthroughs")
public class PlaythroughController {

    private final PlaythroughService playthroughService;
    private final UserService userService;

    public PlaythroughController(PlaythroughService playthroughService, UserService userService) {
        this.playthroughService = playthroughService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<PlaythroughResponse> crear(@Valid @RequestBody PlaythroughRequest request, Authentication auth) {
        User usuario = userService.obtenerPorEmail(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(playthroughService.crear(request, usuario));
    }

    @GetMapping
    public List<PlaythroughResponse> listar(Authentication auth) {
        User usuario = userService.obtenerPorEmail(auth.getName());
        return playthroughService.listar(usuario);
    }

    @GetMapping("/{id}")
    public PlaythroughResponse obtener(@PathVariable Long id, Authentication auth) {
        User usuario = userService.obtenerPorEmail(auth.getName());
        return playthroughService.obtener(id, usuario);
    }

    @GetMapping("/{id}/path")
    public PathResponse path(@PathVariable Long id, Authentication auth) {
        User usuario = userService.obtenerPorEmail(auth.getName());
        return playthroughService.path(id, usuario);
    }
}