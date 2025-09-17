package com.mercedes.workflowrh.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class RefusRequest {
    private String commentaire;
}
