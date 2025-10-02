package com.mercedes.workflowrh.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "soldes_conges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoldeConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employe_id", nullable = false)
    private Employe employe;

    private Integer annee;
    private Float soldeAu2012;
    private Float droitAnnuel;
    private Float droitN;
    private Float congesAcquisN;
    private Float retardsN;
    private Float autorisationsN;
    private Float soldeActuel;

    private Float report; // Solde reporté de l'année précédente
}