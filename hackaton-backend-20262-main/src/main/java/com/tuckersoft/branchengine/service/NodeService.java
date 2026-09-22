package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.NodeDtos.NodeRequest;
import com.tuckersoft.branchengine.dto.NodeDtos.NodeResponse;
import com.tuckersoft.branchengine.entity.StoryNode;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class NodeService {

    private final StoryNodeRepository nodeRepository;

    public NodeService(StoryNodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    public NodeResponse crear(NodeRequest request) {
        if (nodeRepository.existsByNodeCode(request.nodeCode())) {
            throw new ApiException(HttpStatus.CONFLICT, "NODE_CODE_TAKEN", "Ese nodeCode ya existe");
        }

        StoryNode node = new StoryNode();
        node.setNodeCode(request.nodeCode());
        node.setTitle(request.title());
        node.setSceneText(request.sceneText());
        node.setBranchCapacity(request.branchCapacity());
        node.setCurrentBranches(0);
        node.setPrimaryBranchCode(request.primaryBranchCode());
        node.setGlitchBranchCode(request.glitchBranchCode());
        node.setCreatedAt(Instant.now());
        nodeRepository.save(node);

        return aDto(node);
    }

    public NodeResponse obtener(Long id) {
        return aDto(buscar(id));
    }

    public List<NodeResponse> listar() {
        return nodeRepository.findAll().stream().map(this::aDto).toList();
    }

    public StoryNode buscar(Long id) {
        return nodeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NODE_NOT_FOUND", "Nodo no encontrado"));
    }

    public StoryNode buscarPorCodigo(String nodeCode) {
        return nodeRepository.findByNodeCode(nodeCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NODE_NOT_FOUND", "Nodo no encontrado"));
    }

    private NodeResponse aDto(StoryNode node) {
        return new NodeResponse(node.getId(), node.getNodeCode(), node.getTitle(), node.getSceneText(),
                node.getBranchCapacity(), node.getCurrentBranches(), node.getPrimaryBranchCode(),
                node.getGlitchBranchCode(), node.getCreatedAt());
    }
}