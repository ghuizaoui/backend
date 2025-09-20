package com.mercedes.workflowrh.dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public  class KPIData {
    private Long totalDemandes;
    private Long demandesEnCours;
    private Long demandesValidees;
    private Long demandesRefusees;
    private Long autorisationsAujourdhui; // Only for concierge
}