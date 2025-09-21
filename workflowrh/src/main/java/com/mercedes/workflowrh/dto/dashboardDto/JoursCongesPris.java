package com.mercedes.workflowrh.dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public  class JoursCongesPris {
    private double joursPris;
    private double soldeTotal;
    private double pourcentagePris;
    private double soldeRestant;
    private List<EvolutionConges> evolutionParMois;
}