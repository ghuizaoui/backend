package com.mercedes.workflowrh.dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public  class DemandeRecente {
    private Long id;
    private String categorie;
    private String typeDemande;
    private String statut;
    private String dateCreation;
    private String dateDebut;
    private String dateFin;
}