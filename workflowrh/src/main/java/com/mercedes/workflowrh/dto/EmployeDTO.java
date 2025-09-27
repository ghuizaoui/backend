package com.mercedes.workflowrh.dto;


import com.mercedes.workflowrh.entity.Role;
import com.mercedes.workflowrh.entity.TypeContrat;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Data
@Getter
@Setter
public class EmployeDTO {
    private String nom;
    private String prenom;
    private String email;
    private String direction;
    private String service;
    private int grade;
    private LocalDate dateEmbauche;
    private TypeContrat typeContrat;
    private Role role;
    private Integer chefLevel;
}