package com.mercedes.workflowrh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;


import java.time.LocalDate;
import java.time.LocalTime;
@Data
@Getter
@Setter
public  class OrdreMissionRequest {
    @NotNull
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateDebut;

    @NotNull @JsonFormat(pattern = "HH:mm")
    private LocalTime heureDebut;

    @NotNull @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate dateFin;

    @NotNull @JsonFormat(pattern = "HH:mm")
    private LocalTime heureFin;

    @NotBlank
    private String missionObjet;
}