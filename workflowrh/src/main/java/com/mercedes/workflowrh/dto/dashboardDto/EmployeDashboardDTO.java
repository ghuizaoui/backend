package com.mercedes.workflowrh.dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeDashboardDTO {
    private KPIData kpiData;
    private List<DemandeRecente> demandesRecentes;
    private Map<String, Long> statutDistribution;
    private Map<String, Long> categorieDistribution;
    private List<AutorisationAujourdhui> autorisationsAujourdhui; // Only for concierge
}




