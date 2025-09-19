package com.mercedes.workflowrh.dto.dashboardDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceDTO {
    private double joursPris;
    private double soldeTotal;
    private double percentagePris;
    private double soldeRestant;
}