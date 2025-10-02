package com.mercedes.workflowrh.dto;



import lombok.Data;

@Data
public class LiberationRequest {
    private Long demandeId;
    private String commentaire; // Optional comment from concierge
}