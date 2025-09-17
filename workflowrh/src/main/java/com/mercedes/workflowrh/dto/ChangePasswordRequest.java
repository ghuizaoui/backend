package com.mercedes.workflowrh.dto;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String matricule;
    private String nouveauMotDePasse;
}