// src/main/java/com/mercedes/workflowrh/controller/NotificationController.java
package com.mercedes.workflowrh.controller;

import com.mercedes.workflowrh.dto.NotificationPayload;
import com.mercedes.workflowrh.entity.Demande;
import com.mercedes.workflowrh.entity.Notification;
import com.mercedes.workflowrh.entity.StatutNotification;
import com.mercedes.workflowrh.repository.NotificationRepository;
import com.mercedes.workflowrh.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository repo;
    private final NotificationService notificationService;

    // ---- LISTE PAGINÉE (facultatif filtre par statut) ----
    @GetMapping
    public Page<NotificationPayload> list(
            @RequestParam(value = "statut", required = false) StatutNotification statut,
            @PageableDefault(sort = "dateCreation", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth
    ) {
        String matricule = auth.getName();
        Page<Notification> page = (statut == null)
                ? repo.findByDestinataireMatriculeOrderByDateCreationDesc(matricule, pageable)
                : repo.findByDestinataireMatriculeAndStatutOrderByDateCreationDesc(matricule, statut, pageable);
        return page.map(this::toPayload);
    }

    // ---- COMPTEUR NON-LUS (badge) ----
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Authentication auth) {
        long count = repo.countByDestinataireMatriculeAndStatut(auth.getName(), StatutNotification.NON_LU);
        return Map.of("count", count);
    }

    // ---- MARQUER 1 NOTIF LU ----
    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationPayload> markRead(@PathVariable Long id, Authentication auth) {
        String me = auth.getName();
        Notification n = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!me.equals(n.getDestinataire().getMatricule())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (n.getStatut() != StatutNotification.LU) {
            n.setStatut(StatutNotification.LU);
            n = repo.save(n);
        }
        return ResponseEntity.ok(toPayload(n));
    }

    // ---- MARQUER TOUT LU ----
    @PostMapping("/read-all")
    @Transactional
    public Map<String, Integer> markAllRead(Authentication auth) {
        int updated = repo.markAllReadForUser(auth.getName());
        return Map.of("updated", updated);
    }

    @DeleteMapping
    @Transactional
    public Map<String, Integer> deleteAll(Authentication auth) {
        int deleted = repo.deleteByDestinataireMatricule(auth.getName());
        return Map.of("deleted", deleted);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteOne(@PathVariable Long id, Authentication auth) {
        Notification n = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!auth.getName().equals(n.getDestinataire().getMatricule())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        repo.delete(n);
        return ResponseEntity.noContent().build();
    }

    // ---- mapper Entity -> NotificationPayload ----
    private NotificationPayload toPayload(Notification n) {
        Demande d = n.getDemande();
        return NotificationPayload.builder()
                .id(n.getId())
                .demandeId(d != null ? d.getId() : null)
                .type(typeFor(d)) // même logique que côté service WS
                .subject(n.getSubject())
                .message(n.getMessage())
                .statut(n.getStatut().name())
                .dateCreation(n.getDateCreation())
                .dateValidation(d != null ? d.getDateValidation() : null)
                .motifRefus(d != null ? d.getCommentaireRefus() : null)
                .categorie(d != null && d.getCategorie()!=null ? d.getCategorie().name() : null)
                .typeDemande(d != null && d.getTypeDemande()!=null ? d.getTypeDemande().name() : null)
                .auteurMatricule(d != null && d.getEmploye()!=null ? d.getEmploye().getMatricule() : null)
                .destinataire(n.getDestinataire()!=null ? n.getDestinataire().getMatricule() : null)
                .build();
    }

    private String typeFor(Demande d) {
        if (d == null || d.getStatut() == null) return "DEMANDE_UPDATED";
        switch (d.getStatut()) {
            case EN_COURS: return "DEMANDE_CREATED";
            case VALIDEE : return "DEMANDE_VALIDATED";
            case REFUSEE : return "DEMANDE_REFUSED";
            default      : return "DEMANDE_UPDATED";
        }
    }

}
