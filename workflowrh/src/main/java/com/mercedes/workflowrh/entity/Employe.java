package com.mercedes.workflowrh.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Table(name = "employes")
@Builder
@Entity
@Data
@ToString(exclude = {"demandes", "soldesConges", "motDePasse"})
@JsonIgnoreProperties({"demandes", "soldesConges", "motDePasse"})
public class Employe {

    @Id
    private String matricule;

    private String nom;
    private String prenom;
    private Integer grade;
    private String service;

    private String chefHierarchique1Matricule;
    private String chefHierarchique2Matricule;

    private String email;

    @Column(nullable = false)
    @JsonIgnore
    private String motDePasse;

    private String direction;
    private LocalDate dateEmbauche;

    @Enumerated(EnumType.STRING)
    private TypeContrat typeContrat;

    @Enumerated(EnumType.STRING)
    private Role role;

    private Boolean premiereConnexion = true;

    @OneToMany(mappedBy = "employe")
    @JsonIgnore
    private List<Demande> demandes;

    @OneToMany(mappedBy = "employe")
    @JsonIgnore
    private List<SoldeConge> soldesConges;
}
