package com.mercedes.workflowrh.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mercedes.workflowrh.entity.TypeDemande;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public  class CongeRequest {
    @NotNull(message = "typeDemande est obligatoire")
    private TypeDemande typeDemande; // CONGE_* (standard ou exceptionnel)

    @NotNull @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateDebut;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime heureDebut; // optionnel pour congés (demi-journée)

    @NotNull @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateFin;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime heureFin;   // optionnel pour congés


    private String interimaireMatricule;
    private boolean pasDInterim;
    private boolean validationManuelleDRH;
}