package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.DecisionDtos.DecisionRequest;
import com.tuckersoft.branchengine.dto.DecisionDtos.DecisionResponse;
import com.tuckersoft.branchengine.dto.DecisionDtos.PageResponse;
import com.tuckersoft.branchengine.dto.DecisionDtos.RealityLogResponse;
import com.tuckersoft.branchengine.entity.Decision;
import com.tuckersoft.branchengine.entity.Playthrough;
import com.tuckersoft.branchengine.entity.StoryNode;
import com.tuckersoft.branchengine.entity.User;
import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.exception.ApiException;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class DecisionService {

    private static final Set<String> IMPACTOS_VALIDOS = Set.of("LEVE", "MODERADO", "GRAVE", "CRITICO");
    private static final Pattern LETRA = Pattern.compile("[a-z]");

    private final DecisionRepository decisionRepository;
    private final RealityLogRepository realityLogRepository;
    private final PlaythroughService playthroughService;
    private final StoryNodeRepository nodeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DecisionService(DecisionRepository decisionRepository,
                           RealityLogRepository realityLogRepository,
                           PlaythroughService playthroughService,
                           StoryNodeRepository nodeRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.decisionRepository = decisionRepository;
        this.realityLogRepository = realityLogRepository;
        this.playthroughService = playthroughService;
        this.nodeRepository = nodeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public DecisionResponse crear(DecisionRequest request, User actual, String simulate) {
        Playthrough p = playthroughService.buscar(request.playthroughId());

        if (!p.getUser().getId().equals(actual.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Esta partida no es tuya");
        }
        if (!"ACTIVA".equals(p.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "PLAYTHROUGH_FINISHED", "Esa partida ya termino");
        }
        if (!IMPACTOS_VALIDOS.contains(request.impactLevel())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMPACT", "impactLevel debe ser LEVE, MODERADO, GRAVE o CRITICO");
        }

        StoryNode origen = p.getCurrentNode();
        String branchType = clasificar(request.rawInput());
        String handlerUnit = unidad(branchType);
        String outcomeCode = consecuencia(branchType);
        Instant ahora = Instant.now();

        Decision decision = new Decision();
        decision.setPlaythrough(p);
        decision.setNode(origen);
        decision.setRawInput(request.rawInput());
        decision.setBranchType(branchType);
        decision.setImpactLevel(request.impactLevel());
        decision.setHandlerUnit(handlerUnit);
        decision.setOutcomeCode(outcomeCode);
        decision.setCreatedAt(ahora);
        decision.setUpdatedAt(ahora);

        if ("ENTRADA_CORRUPTA".equals(branchType)) {
            decision.setResolvedNodeCode(null);
            decision.setStatus("ERROR");
            decisionRepository.save(decision);
            return aDto(decision, p);
        }

        aplicarStats(p, request.impactLevel());

        String destino = "RUPTURA_CUARTA_PARED".equals(branchType) || "CRITICO".equals(request.impactLevel())
                ? origen.getGlitchBranchCode()
                : origen.getPrimaryBranchCode();
        decision.setResolvedNodeCode(destino);

        resolverFinal(p, destino);
        p.setUpdatedAt(ahora);
        playthroughService.guardar(p);

        decision.setStatus("REGISTRADA");
        decisionRepository.save(decision);

        eventPublisher.publishEvent(new DecisionCommittedEvent(
                decision.getId(), p.getUser().getEmail(), p.getUser().getDisplayName(), p.getPlayerTag(),
                branchType, request.impactLevel(), handlerUnit, outcomeCode, origen.getNodeCode(), destino,
                p.getStatus(), p.getLucidity(), p.getControlLevel(), p.getEndingCode(), request.rawInput(),
                ahora, "MAIL_FAILURE".equals(simulate)
        ));

        return aDto(decision, p);
    }

    private void aplicarStats(Playthrough p, String impacto) {
        int deltaLucidez;
        int deltaControl;
        switch (impacto) {
            case "LEVE" -> { deltaLucidez = -5; deltaControl = 5; }
            case "MODERADO" -> { deltaLucidez = -15; deltaControl = 10; }
            case "GRAVE" -> { deltaLucidez = -30; deltaControl = 20; }
            default -> { deltaLucidez = -40; deltaControl = 45; }
        }
        p.setLucidity(Math.max(0, Math.min(100, p.getLucidity() + deltaLucidez)));
        p.setControlLevel(Math.max(0, Math.min(100, p.getControlLevel() + deltaControl)));
    }

    private void resolverFinal(Playthrough p, String destino) {
        if (p.getControlLevel() >= 100) {
            p.setStatus("FINALIZADA");
            p.setEndingCode("ENDING_PAC_SYMBOL");
            return;
        }
        if (p.getLucidity() <= 0) {
            p.setStatus("FINALIZADA");
            p.setEndingCode("ENDING_WHITE_BEAR");
            return;
        }
        StoryNode siguiente = destino == null ? null : nodeRepository.findByNodeCode(destino).orElse(null);
        if (siguiente == null) {
            p.setStatus("FINALIZADA");
            p.setEndingCode("ENDING_NETFLIX_CUT");
            return;
        }
        p.setStatus("ACTIVA");
        p.setCurrentNode(siguiente);
    }

    private String clasificar(String rawInput) {
        String normalizado = Normalizer.normalize(rawInput, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();

        if (!LETRA.matcher(normalizado).find()) return "ENTRADA_CORRUPTA";
        if (contieneAlguna(normalizado, "netflix", "camara", "espectador", "videojuego")) return "RUPTURA_CUARTA_PARED";
        if (contieneAlguna(normalizado, "vigilan", "simbolo", "conspiracion")) return "SOSPECHA";
        if (contieneAlguna(normalizado, "rechaza", "destruye", "desobedece", "renuncia")) return "REBELDIA";
        return "OBEDIENCIA";
    }

    private boolean contieneAlguna(String texto, String... palabras) {
        for (String palabra : palabras) {
            if (texto.contains(palabra)) return true;
        }
        return false;
    }

    private String unidad(String branchType) {
        return switch (branchType) {
            case "OBEDIENCIA" -> "Mesa de Guion";
            case "REBELDIA" -> "Control de Continuidad";
            case "SOSPECHA" -> "Oficina de Seguridad";
            case "RUPTURA_CUARTA_PARED" -> "Departamento Netflix";
            default -> "Archivo de Errores";
        };
    }

    private String consecuencia(String branchType) {
        return switch (branchType) {
            case "OBEDIENCIA" -> "ADVANCE_MAIN_PATH";
            case "REBELDIA" -> "FORK_TIMELINE";
            case "SOSPECHA" -> "INJECT_WHITE_BEAR_SYMBOL";
            case "RUPTURA_CUARTA_PARED" -> "BREAK_FOURTH_WALL";
            default -> "DISCARD_INPUT";
        };
    }

    public DecisionResponse obtener(Long id, User actual) {
        Decision d = buscar(id);
        exigirAcceso(d, actual);
        return aDto(d, d.getPlaythrough());
    }

    public List<RealityLogResponse> realityLogs(Long id, User actual) {
        Decision d = buscar(id);
        exigirAcceso(d, actual);
        return realityLogRepository.findByDecisionOrderByCreatedAtAsc(d).stream()
                .map(log -> new RealityLogResponse(log.getId(), d.getId(), log.getRecipientEmail(),
                        log.getSubject(), log.getLogStatus(), log.getErrorMessage(), log.getSentAt(), log.getCreatedAt()))
                .toList();
    }

    public PageResponse<DecisionResponse> listar(String branchType, String impactLevel, String status,
                                                 Long playthroughId, int page, int size, User actual) {
        boolean esAdmin = "ROLE_ADMIN".equals(actual.getRole());

        Specification<Decision> spec = (root, query, cb) -> cb.conjunction();
        if (!esAdmin) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("playthrough").get("user").get("id"), actual.getId()));
        }
        if (branchType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("branchType"), branchType));
        }
        if (impactLevel != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("impactLevel"), impactLevel));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (playthroughId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("playthrough").get("id"), playthroughId));
        }

        Page<Decision> pagina = decisionRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<DecisionResponse> contenido = pagina.getContent().stream()
                .map(d -> aDto(d, d.getPlaythrough()))
                .toList();

        return new PageResponse<>(contenido, pagina.getTotalElements(), pagina.getTotalPages(), page, size);
    }

    private Decision buscar(Long id) {
        return decisionRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DECISION_NOT_FOUND", "Decision no encontrada"));
    }

    private void exigirAcceso(Decision d, User actual) {
        if ("ROLE_ADMIN".equals(actual.getRole())) return;
        if (!d.getPlaythrough().getUser().getId().equals(actual.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Esta decision no es tuya");
        }
    }

    private DecisionResponse aDto(Decision d, Playthrough p) {
        return new DecisionResponse(
                d.getId(), p.getId(), p.getPlayerTag(), d.getNode().getNodeCode(), d.getResolvedNodeCode(),
                d.getRawInput(), d.getBranchType(), d.getImpactLevel(), d.getHandlerUnit(), d.getOutcomeCode(),
                d.getStatus(), p.getStatus(), p.getLucidity(), p.getControlLevel(), p.getEndingCode(),
                d.getCreatedAt(), d.getUpdatedAt()
        );
    }
}