package com.mercedes.workflowrh.dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public  class VueEnsembleDemandes {
    private long totalDemandes;
    private long totalConges;
    private long totalAutorisations;
    private long totalOrdresMission;
    private double pourcentageConges;
    private double pourcentageAutorisations;
    private double pourcentageOrdresMission;
}