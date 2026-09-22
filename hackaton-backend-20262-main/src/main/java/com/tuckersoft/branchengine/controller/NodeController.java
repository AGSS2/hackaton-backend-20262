package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.NodeDtos.NodeRequest;
import com.tuckersoft.branchengine.dto.NodeDtos.NodeResponse;
import com.tuckersoft.branchengine.service.NodeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nodes")
public class NodeController {

    private final NodeService nodeService;

    public NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @PostMapping
    public ResponseEntity<NodeResponse> crear(@Valid @RequestBody NodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nodeService.crear(request));
    }

    @GetMapping
    public List<NodeResponse> listar() {
        return nodeService.listar();
    }

    @GetMapping("/{id}")
    public NodeResponse obtener(@PathVariable Long id) {
        return nodeService.obtener(id);
    }
}