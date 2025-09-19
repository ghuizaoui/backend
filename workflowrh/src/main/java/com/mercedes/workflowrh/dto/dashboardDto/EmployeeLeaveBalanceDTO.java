package com.mercedes.workflowrh.dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeLeaveBalanceDTO {
    private String matricule;
    private String nom;
    private String prenom;
    private String service;
    private float soldeConges;
    private boolean hasHighestBalance;
}