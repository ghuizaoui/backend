package com.mercedes.workflowrh.dto.dashboardDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptedRequestsDTO {
    private String service;
    private long demandesAcceptees;
}