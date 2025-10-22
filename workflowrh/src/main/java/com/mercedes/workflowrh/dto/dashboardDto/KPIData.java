package com.mercedes.workflowrh.dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class KPIData {
    private Long totalDemandes;
    private Long demandesEnCours;
    private Long demandesValidees;
    private Long demandesRefusees;

    // NEW: Category-specific counts
    private Long congesStandard;
    private Long congesExceptionnel;
    private Long autorisations;
    private Long ordresMission;

    // For concierge - validated autorisations for today only
    private Long autorisationsAujourdhui;
}