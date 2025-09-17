// src/main/java/com/mercedes/workflowrh/dto/AutorisationRequest.java
package com.mercedes.workflowrh.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mercedes.workflowrh.entity.TypeDemande;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class AutorisationRequest {

    @NotNull
    private TypeDemande typeDemande; // doit appartenir à la catégorie AUTORISATION

    // --- PRÉVU (obligatoire) ---
    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateAutorisation;   // = autoDate

    @NotNull
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime heureDebut;         // = autoHeureDebut

    @NotNull
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime heureFin;           // = autoHeureFin

    // --- RÉEL (optionnel, peut être envoyé plus tard) ---
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateReelle;         // = autoDateReelle (souvent = dateAutorisation)

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime heureSortieReelle;  // = autoHeureSortieReelle

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime heureRetourReel;    // = autoHeureRetourReel

    // --- Saisie (optionnel) ---

}
