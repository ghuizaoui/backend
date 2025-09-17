package com.mercedes.workflowrh.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class ValidationRequest {
    @NotNull(message = "Le statut de validation est obligatoire")
    private Boolean isValidee;

    private String commentaire;
}