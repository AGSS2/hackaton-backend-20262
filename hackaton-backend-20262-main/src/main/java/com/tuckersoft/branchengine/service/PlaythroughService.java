package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.PlaythroughDtos.PathResponse;
import com.tuckersoft.branchengine.dto.PlaythroughDtos.PathStep;
import com.tuckersoft.branchengine.dto.PlaythroughDtos.PlaythroughRequest;
import com.tuckersoft.branchengine.dto.PlaythroughDtos.PlaythroughResponse;
import com.tuckersoft.branchengine.entity.Decision;
import com.tuckersoft.branchengine.entity.Playthrough;
import com.tuckersoft.branchengine.entity.StoryNode;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlaythroughService {

    private final PlaythroughRepository playthroughRepository;
    private final DecisionRepository decisionRepository;
    private final NodeService nodeService;

    public PlaythroughService(PlaythroughRepository playthroughRepository,
                              DecisionRepository decisionRepository,
                              NodeService nodeService) {
        this.playthroughRepository = playthroughRepository;
        this.decisionRepository = decisionRepository;
        this.nodeService = nodeService;
    }

    @Transactional
    public PlaythroughResponse crear(PlaythroughRequest request, User usuario) {
        StoryNode nodo = nodeService.buscarPorCodigo(request.startNodeCode());

        if (playthroughRepository.existsByPlayerTag(request.playerTag())) {
            throw new ApiException(HttpStatus.CONFLICT, "PLAYER_TAG_TAKEN", "Ese playerTag ya existe");
        }

        if (nodo.getCurrentBranches() >= nodo.getBranchCapacity()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NODE_FULL", "Ese nodo ya no tiene ramas disponibles");
        }

        Playthrough p = new Playthrough();
        p.setPlayerTag(request.playerTag());
        p.setUser(usuario);
        p.setStartNodeCode(nodo.getNodeCode());
        p.setCurrentNode(nodo);
        p.setLucidity(100);
        p.setControlLevel(0);
        p.setStatus("ACTIVA");
        p.setEndingCode(null);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());

        nodo.setCurrentBranches(nodo.getCurrentBranches() + 1);

        playthroughRepository.save(p);
        return aDto(p);
    }

    public List<PlaythroughResponse> listar(User actual) {
        List<Playthrough> lista = esAdmin(actual)
                ? playthroughRepository.findAllByOrderByCreatedAtDesc()
                : playthroughRepository.findByUserOrderByCreatedAtDesc(actual);
        return lista.stream().map(this::aDto).toList();
    }

    public Playthrough buscar(Long id) {
        return playthroughRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PLAYTHROUGH_NOT_FOUND", "Partida no encontrada"));
    }

    public PlaythroughResponse obtener(Long id, User actual) {
        Playthrough p = buscar(id);
        exigirAcceso(p, actual);
        return aDto(p);
    }

    public PathResponse path(Long id, User actual) {
        Playthrough p = buscar(id);
        exigirAcceso(p, actual);

        List<Decision> decisiones = decisionRepository.findByPlaythroughOrderByCreatedAtAsc(p);
        List<PathStep> pasos = new ArrayList<>();
        int orden = 1;
        for (Decision d : decisiones) {
            if (d.getResolvedNodeCode() == null) continue;
            pasos.add(new PathStep(orden++, d.getId(), d.getNode().getNodeCode(), d.getResolvedNodeCode(),
                    d.getBranchType(), d.getImpactLevel(), d.getCreatedAt()));
        }

        return new PathResponse(p.getId(), p.getPlayerTag(), p.getStatus(), p.getEndingCode(),
                p.getStartNodeCode(), p.getCurrentNode().getNodeCode(), pasos);
    }

    public void exigirAcceso(Playthrough p, User actual) {
        if (esAdmin(actual)) return;
        if (!p.getUser().getId().equals(actual.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Esta partida no es tuya");
        }
    }

    public void guardar(Playthrough p) {
        playthroughRepository.save(p);
    }

    private boolean esAdmin(User u) {
        return "ROLE_ADMIN".equals(u.getRole());
    }

    private PlaythroughResponse aDto(Playthrough p) {
        return new PlaythroughResponse(p.getId(), p.getPlayerTag(), p.getUser().getEmail(),
                p.getStartNodeCode(), p.getCurrentNode().getNodeCode(), p.getLucidity(), p.getControlLevel(),
                p.getStatus(), p.getEndingCode(), p.getCreatedAt(), p.getUpdatedAt());
    }
}