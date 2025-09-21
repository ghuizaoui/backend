// DashboardChefService.java
package com.mercedes.workflowrh.service;

import com.mercedes.workflowrh.dto.dashboardDto.DashboardChefDTO;
import com.mercedes.workflowrh.entity.CategorieDemande;
import com.mercedes.workflowrh.entity.StatutDemande;
import java.time.LocalDateTime;

public interface DashboardChefService {
    DashboardChefDTO getDashboardChef(String matriculeChef, LocalDateTime dateDebut, LocalDateTime dateFin);
    DashboardChefDTO getDashboardChef(String matriculeChef, String dateDebut, String dateFin);
}