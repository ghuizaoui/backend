package com.mercedes.workflowrh.dto.dashboardDto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public  class AutorisationAujourdhui {
    private Long demandeId;
    private String employeNom;
    private String employePrenom;
    private String employeMatricule;
    private String heureDebut;
    private String heureFin;
    private String service;
}