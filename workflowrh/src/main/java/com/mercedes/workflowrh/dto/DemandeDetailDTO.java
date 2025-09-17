// src/main/java/com/mercedes/workflowrh/dto/DemandeDetailDTO.java
package com.mercedes.workflowrh.dto;

import com.mercedes.workflowrh.entity.CategorieDemande;
import com.mercedes.workflowrh.entity.StatutDemande;
import com.mercedes.workflowrh.entity.TypeDemande;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** DTO pour afficher le détail complet d'une demande */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeDetailDTO {

    private Long id;

    // Employé
    private String employeMatricule;
    private String employeNom;
    private String employePrenom;
    private String employeEmail;

    // Métadonnées
    private CategorieDemande categorie;
    private TypeDemande typeDemande;
    private StatutDemande statut;
    private String commentaireRefus;
    private LocalDateTime dateCreation;
    private LocalDateTime dateValidation;

    // CONGÉS
    private LocalDate congeDateDebut;
    private LocalTime congeHeureDebut;
    private LocalDate congeDateFin;
    private LocalTime congeHeureFin;

    // AUTORISATION
    private LocalDate autoDate;
    private LocalTime autoHeureDebut;
    private LocalTime autoHeureFin;
    private LocalDate autoDateReelle;
    private LocalTime autoHeureSortieReelle;
    private LocalTime autoHeureRetourReel;

    // ORDRE DE MISSION
    private LocalDate missionDateDebut;
    private LocalTime missionHeureDebut;
    private LocalDate missionDateFin;
    private LocalTime missionHeureFin;
    private String missionObjet;
}
