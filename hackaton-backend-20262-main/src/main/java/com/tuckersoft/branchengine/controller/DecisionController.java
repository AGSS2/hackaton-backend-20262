package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.DecisionDtos.DecisionRequest;
import com.tuckersoft.branchengine.dto.DecisionDtos.DecisionResponse;
import com.tuckersoft.branchengine.dto.DecisionDtos.PageResponse;
import com.tuckersoft.branchengine.dto.DecisionDtos.RealityLogResponse;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.service.DecisionService;
import com.tuckersoft.branchengine.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/decisions")
public class DecisionController {

    private final DecisionService decisionService;
    private final UserService userService;

    public DecisionController(DecisionService decisionService, UserService userService) {
        this.decisionService = decisionService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<DecisionResponse> crear(@Valid @RequestBody DecisionRequest request,
                                                  @RequestHeader(value = "X-Bandersnatch-Simulate", required = false) String simulate,
                                                  Authentication auth) {
        User usuario = userService.obtenerPorEmail(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(decisionService.crear(request, usuario, simulate));
    }

    @GetMapping("/{id}")
    public DecisionResponse obtener(@PathVariable Long id, Authentication auth) {
        User usuario = userService.obtenerPorEmail(auth.getName());
        return decisionService.obtener(id, usuario);
    }

    @GetMapping("/{id}/reality-logs")
    public List<RealityLogResponse> realityLogs(@PathVariable Long id, Authentication auth) {
        User usuario = userService.obtenerPorEmail(auth.getName());
        return decisionService.realityLogs(id, usuario);
    }

    @GetMapping
    public PageResponse<DecisionResponse> listar(
            @RequestParam(required = false) String branchType,
            @RequestParam(required = false) String impactLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long playthroughId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        User usuario = userService.obtenerPorEmail(auth.getName());
        return decisionService.listar(branchType, impactLevel, status, playthroughId, page, size, usuario);
    }
}