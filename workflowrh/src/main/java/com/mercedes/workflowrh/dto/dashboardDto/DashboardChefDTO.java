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
public class DashboardChefDTO {
    private VueEnsembleDemandes vueEnsembleDemandes;
    private RepartitionStatuts repartitionStatuts;
    private JoursCongesPris joursCongesPris;
    private List<DemandesAccepteesService> demandesAccepteesServices;
    private List<SoldeEmploye> soldesEmployes;
}