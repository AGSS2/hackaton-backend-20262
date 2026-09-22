package com.tuckersoft.branchengine.listener;

import com.tuckersoft.branchengine.entity.Decision;
import com.tuckersoft.branchengine.entity.RealityLog;
import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.RealityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Component
public class BranchNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(BranchNotificationListener.class);

    private final DecisionRepository decisionRepository;
    private final RealityLogRepository realityLogRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:qa@tuckersoft.test}")
    private String remitente;

    public BranchNotificationListener(DecisionRepository decisionRepository,
                                      RealityLogRepository realityLogRepository,
                                      JavaMailSender mailSender) {
        this.decisionRepository = decisionRepository;
        this.realityLogRepository = realityLogRepository;
        this.mailSender = mailSender;
    }

    @Async("branchExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCommit(DecisionCommittedEvent evento) {
        Decision decision = decisionRepository.findById(evento.decisionId()).orElse(null);
        if (decision == null) return;

        decision.setStatus("PROCESANDO");
        decision.setUpdatedAt(Instant.now());
        decisionRepository.save(decision);

        String subject = "[TUCKERSOFT] " + evento.branchType() + " en " + evento.playerTag()
                + " | Impacto " + evento.impactLevel();
        String body = cuerpo(evento);

        try {
            if (evento.simulateMailFailure()) {
                throw new RuntimeException("Fallo de correo simulado (X-Bandersnatch-Simulate: MAIL_FAILURE)");
            }
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(evento.recipientEmail());
            mensaje.setSubject(subject);
            mensaje.setText(body);
            mailSender.send(mensaje);

            decision.setStatus("ESTABILIZADA");
            decision.setUpdatedAt(Instant.now());
            decisionRepository.save(decision);

            RealityLog logExitoso = new RealityLog();
            logExitoso.setDecision(decision);
            logExitoso.setRecipientEmail(evento.recipientEmail());
            logExitoso.setSubject(subject);
            logExitoso.setLogStatus("SENT");
            logExitoso.setSentAt(Instant.now());
            logExitoso.setCreatedAt(Instant.now());
            realityLogRepository.save(logExitoso);

        } catch (Exception e) {
            decision.setStatus("ERROR");
            decision.setUpdatedAt(Instant.now());
            decisionRepository.save(decision);

            RealityLog logFallido = new RealityLog();
            logFallido.setDecision(decision);
            logFallido.setRecipientEmail(evento.recipientEmail());
            logFallido.setSubject(subject);
            logFallido.setLogStatus("FAILED");
            logFallido.setErrorMessage(e.getMessage());
            logFallido.setCreatedAt(Instant.now());
            realityLogRepository.save(logFallido);

            log.error("Fallo al enviar el Informe de Realidad de la decision {}", evento.decisionId(), e);
        }

        System.out.println("[BRANCH-LOG] Decision ID: " + evento.decisionId()
                + " | Player: " + evento.playerTag()
                + " | Branch: " + evento.branchType()
                + " | Impact: " + evento.impactLevel()
                + " | Unit: " + evento.handlerUnit()
                + " | Node: " + evento.sourceNodeCode() + " -> " + evento.resolvedNodeCode()
                + " | Thread: " + Thread.currentThread().getName()
                + " | Status: " + decision.getStatus());
    }

    private String cuerpo(DecisionCommittedEvent evento) {
        String endingCode = evento.endingCode() == null ? "-" : evento.endingCode();
        return "Hola " + evento.displayName() + ",\n\n"
                + "Una partida de prueba acaba de ramificarse.\n\n"
                + "-".repeat(36) + "\n"
                + "Decision ID      : #" + evento.decisionId() + "\n"
                + "Jugador          : " + evento.playerTag() + "\n"
                + "Rama             : " + evento.branchType() + "\n"
                + "Impacto          : " + evento.impactLevel() + "\n"
                + "Departamento     : " + evento.handlerUnit() + "\n"
                + "Consecuencia     : " + evento.outcomeCode() + "\n"
                + "Nodo origen      : " + evento.sourceNodeCode() + "\n"
                + "Nodo destino     : " + evento.resolvedNodeCode() + "\n"
                + "Estado partida   : " + evento.playthroughStatus() + "\n"
                + "Lucidez          : " + evento.lucidity() + "/100\n"
                + "Nivel de control : " + evento.controlLevel() + "/100\n"
                + "Final            : " + endingCode + "\n"
                + "Registrada       : " + evento.createdAt() + "\n"
                + "-".repeat(36) + "\n\n"
                + "Decision original del jugador:\n"
                + "\"" + evento.rawInput() + "\"\n\n"
                + "- Tuckersoft Branch Engine, 1984\n";
    }
}