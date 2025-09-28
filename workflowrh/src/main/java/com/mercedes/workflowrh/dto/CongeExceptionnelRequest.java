package com.mercedes.workflowrh.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.mercedes.workflowrh.entity.TypeDemande;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CongeExceptionnelRequest {
    @NotNull
    private TypeDemande typeDemande;
    @NotNull
    private LocalDate dateDebut;
    @NotNull
    private LocalTime heureDebut;

    private LocalDate dateFin;

    private LocalTime heureFin;


    private  String interimaireMatricule;
}