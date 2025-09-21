package com.mercedes.workflowrh.dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// SoldeEmploye.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoldeEmploye {
    private String matricule;
    private String nom;
    private String prenom;
    private double solde;
    private boolean plusGrandSolde;
}