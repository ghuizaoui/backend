package com.mercedes.workflowrh.dto;

import lombok.*;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EmployeSoldeDto {
    private String nom;
    private String prenom;
    private Integer grade; //solde
    private String service;
    private Integer annee;
    private Float soldeAu2012;
    private Float droitAnnuel;
    private Float droitN;
    private Float congesAcquisN;
    private Float retardsN;
    private Float autorisationsN;
    private Float soldeActuel;


}
