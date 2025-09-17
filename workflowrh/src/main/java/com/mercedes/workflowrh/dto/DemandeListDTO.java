// src/main/java/com/mercedes/workflowrh/dto/DemandeListItemDTO.java
package com.mercedes.workflowrh.dto;

import com.mercedes.workflowrh.entity.CategorieDemande;
import com.mercedes.workflowrh.entity.StatutDemande;
import com.mercedes.workflowrh.entity.TypeDemande;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** DTO pour le tableau principal des demandes */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeListDTO {

    private Long id;

    private String employeMatricule;
    private String employeNom;
    private String employePrenom;

    private CategorieDemande categorie;
    private TypeDemande typeDemande;

    // Seulement les dates (pas les heures)
    private LocalDate dateDebut;
    private LocalDate dateFin;

    private StatutDemande statut;
    private LocalDateTime dateCreation;
}
