package com.mercedes.workflowrh.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String matricule;
    private String motDePasse;
}