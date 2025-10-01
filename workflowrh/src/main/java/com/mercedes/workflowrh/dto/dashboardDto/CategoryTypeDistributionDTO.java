package com.mercedes.workflowrh.dto.dashboardDto;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTypeDistributionDTO {
    private String categorie;
    private String typeDemande;
    private long count;
    private double percentage;
}