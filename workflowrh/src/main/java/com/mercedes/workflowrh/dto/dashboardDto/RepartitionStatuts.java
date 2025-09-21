package com.mercedes.workflowrh.dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public  class RepartitionStatuts {
    private long enCours;
    private long validees;
    private long refusees;
    private double pourcentageEnCours;
    private double pourcentageValidees;
    private double pourcentageRefusees;
}