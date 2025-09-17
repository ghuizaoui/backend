// src/main/java/com/mercedes/workflowrh/dto/NotificationPayload.java
package com.mercedes.workflowrh.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationPayload {
    private Long id;
    private Long demandeId;
    private String type;
    private String subject;
    private String message;
    private String statut;

    private LocalDateTime dateCreation;
    private LocalDateTime dateValidation;
    private String motifRefus;

    private String categorie;     // e.g. "CONGE_STANDARD"
    private String typeDemande;   // e.g. "CONGE_ANNUEL"

    // ➕ pour affichage front
    private LocalDateTime periodeDebut;
    private LocalDateTime periodeFin;
    private String heureDebut;    // "HH:mm" (autorisation)
    private String heureFin;

    private String auteurMatricule;
    private String destinataire;
}
