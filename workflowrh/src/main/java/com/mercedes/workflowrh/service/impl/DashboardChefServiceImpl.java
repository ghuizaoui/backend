// DashboardChefServiceImpl.java (updated calculerJoursCongesPris with filter)
package com.mercedes.workflowrh.service.impl;

import com.mercedes.workflowrh.dto.dashboardDto.*;
import com.mercedes.workflowrh.entity.*;
import com.mercedes.workflowrh.repository.DemandeRepository;
import com.mercedes.workflowrh.repository.EmployeRepository;
import com.mercedes.workflowrh.repository.SoldeCongeRepository;
import com.mercedes.workflowrh.service.DashboardChefService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardChefServiceImpl implements DashboardChefService {

    private final DemandeRepository demandeRepository;
    private final EmployeRepository employeRepository;
    private final SoldeCongeRepository soldeCongeRepository;

    @Override
    public DashboardChefDTO getDashboardChef(String matriculeChef, LocalDateTime dateDebut, LocalDateTime dateFin) {
        // Récupérer le service du chef
        Employe chef = employeRepository.findByMatricule(matriculeChef)
                .orElseThrow(() -> new RuntimeException("Chef non trouvé"));
        String serviceChef = chef.getService();

        // Récupérer tous les employés du service
        List<Employe> employesService = employeRepository.findByService(serviceChef);
        List<String> matriculesService = employesService.stream()
                .map(Employe::getMatricule)
                .collect(Collectors.toList());

        // Construire le dashboard
        return DashboardChefDTO.builder()
                .vueEnsembleDemandes(calculerVueEnsembleDemandes(matriculesService, dateDebut, dateFin))
                .repartitionStatuts(calculerRepartitionStatuts(matriculesService, dateDebut, dateFin))
                .joursCongesPris(calculerJoursCongesPris(matriculesService, dateDebut, dateFin))
                .demandesAccepteesServices(calculerDemandesAccepteesServices(serviceChef, chef.getDirection(), dateDebut, dateFin))
                .soldesEmployes(calculerSoldesEmployes(employesService))
                .build();
    }

    @Override
    public DashboardChefDTO getDashboardChef(String matriculeChef, String dateDebut, String dateFin) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime start = LocalDateTime.parse(dateDebut + " 00:00:00", formatter);
        LocalDateTime end = LocalDateTime.parse(dateFin + " 23:59:59", formatter);

        return getDashboardChef(matriculeChef, start, end);
    }

    private VueEnsembleDemandes calculerVueEnsembleDemandes(List<String> matriculesService, LocalDateTime dateDebut, LocalDateTime dateFin) {
        long totalDemandes = demandeRepository.countByEmployeMatriculeInAndDateCreationBetween(matriculesService, dateDebut, dateFin);
        long totalConges = demandeRepository.countByEmployeMatriculeInAndCategorieInAndDateCreationBetween(
                matriculesService,
                Arrays.asList(CategorieDemande.CONGE_STANDARD, CategorieDemande.CONGE_EXCEPTIONNEL),
                dateDebut, dateFin
        );
        long totalAutorisations = demandeRepository.countByEmployeMatriculeInAndCategorieAndDateCreationBetween(
                matriculesService, CategorieDemande.AUTORISATION, dateDebut, dateFin
        );
        long totalOrdresMission = demandeRepository.countByEmployeMatriculeInAndCategorieAndDateCreationBetween(
                matriculesService, CategorieDemande.ORDRE_MISSION, dateDebut, dateFin
        );

        double pourcentageConges = totalDemandes > 0 ? (double) totalConges / totalDemandes * 100 : 0;
        double pourcentageAutorisations = totalDemandes > 0 ? (double) totalAutorisations / totalDemandes * 100 : 0;
        double pourcentageOrdresMission = totalDemandes > 0 ? (double) totalOrdresMission / totalDemandes * 100 : 0;

        return VueEnsembleDemandes.builder()
                .totalDemandes(totalDemandes)
                .totalConges(totalConges)
                .totalAutorisations(totalAutorisations)
                .totalOrdresMission(totalOrdresMission)
                .pourcentageConges(Math.round(pourcentageConges * 100.0) / 100.0)
                .pourcentageAutorisations(Math.round(pourcentageAutorisations * 100.0) / 100.0)
                .pourcentageOrdresMission(Math.round(pourcentageOrdresMission * 100.0) / 100.0)
                .build();
    }

    private RepartitionStatuts calculerRepartitionStatuts(List<String> matriculesService, LocalDateTime dateDebut, LocalDateTime dateFin) {
        long enCours = demandeRepository.countByEmployeMatriculeInAndStatutAndDateCreationBetween(
                matriculesService, StatutDemande.EN_COURS, dateDebut, dateFin
        );
        long validees = demandeRepository.countByEmployeMatriculeInAndStatutAndDateCreationBetween(
                matriculesService, StatutDemande.VALIDEE, dateDebut, dateFin
        );
        long refusees = demandeRepository.countByEmployeMatriculeInAndStatutAndDateCreationBetween(
                matriculesService, StatutDemande.REFUSEE, dateDebut, dateFin
        );

        long total = enCours + validees + refusees;

        double pourcentageEnCours = total > 0 ? (double) enCours / total * 100 : 0;
        double pourcentageValidees = total > 0 ? (double) validees / total * 100 : 0;
        double pourcentageRefusees = total > 0 ? (double) refusees / total * 100 : 0;

        return RepartitionStatuts.builder()
                .enCours(enCours)
                .validees(validees)
                .refusees(refusees)
                .pourcentageEnCours(Math.round(pourcentageEnCours * 100.0) / 100.0)
                .pourcentageValidees(Math.round(pourcentageValidees * 100.0) / 100.0)
                .pourcentageRefusees(Math.round(pourcentageRefusees * 100.0) / 100.0)
                .build();
    }

    private JoursCongesPris calculerJoursCongesPris(List<String> matriculesService, LocalDateTime dateDebut, LocalDateTime dateFin) {
        // Récupérer les congés validés
        List<Demande> congesValides = demandeRepository.findByEmployeMatriculeInAndStatutAndCategorieInAndDateCreationBetween(
                matriculesService,
                StatutDemande.VALIDEE,
                Arrays.asList(CategorieDemande.CONGE_STANDARD, CategorieDemande.CONGE_EXCEPTIONNEL),
                dateDebut, dateFin
        );

        // Filtrer seulement les types qui déduisent du solde
        List<Demande> filteredConges = congesValides.stream()
                .filter(demande -> demande.getTypeDemande() != null && demande.getTypeDemande().deductsFromSolde())
                .collect(Collectors.toList());

        // Calculer les jours pris
        double joursPris = filteredConges.stream()
                .mapToDouble(demande -> {
                    if (demande.getCongeDateDebut() != null && demande.getCongeDateFin() != null) {
                        return ChronoUnit.DAYS.between(demande.getCongeDateDebut(), demande.getCongeDateFin()) + 1;
                    }
                    return 0;
                })
                .sum();

        // Calculer le solde total du service
        double soldeTotal = matriculesService.stream()
                .mapToDouble(matricule -> {
                    Optional<SoldeConge> soldeOpt = soldeCongeRepository.findTopByEmployeMatriculeOrderByAnneeDesc(matricule);
                    return soldeOpt.map(SoldeConge::getSoldeActuel).orElse(0f);
                })
                .sum();

        double pourcentagePris = soldeTotal > 0 ? (joursPris / soldeTotal) * 100 : 0;
        double soldeRestant = soldeTotal - joursPris;

        // Calculer l'évolution par mois
        List<EvolutionConges> evolutionParMois = calculerEvolutionCongesParMois(filteredConges, dateDebut, dateFin);

        return JoursCongesPris.builder()
                .joursPris(joursPris)
                .soldeTotal(soldeTotal)
                .pourcentagePris(Math.round(pourcentagePris * 100.0) / 100.0)
                .soldeRestant(soldeRestant)
                .evolutionParMois(evolutionParMois)
                .build();
    }

    private List<EvolutionConges> calculerEvolutionCongesParMois(List<Demande> congesValides, LocalDateTime dateDebut, LocalDateTime dateFin) {
        // Implémentation simplifiée - à adapter selon vos besoins
        List<EvolutionConges> evolution = new ArrayList<>();

        // Exemple: regrouper par mois
        Map<String, Double> joursParMois = new HashMap<>();

        for (Demande demande : congesValides) {
            if (demande.getCongeDateDebut() != null) {
                String mois = demande.getCongeDateDebut().getMonth().toString() + " " + demande.getCongeDateDebut().getYear();
                double jours = ChronoUnit.DAYS.between(demande.getCongeDateDebut(), demande.getCongeDateFin()) + 1;

                joursParMois.put(mois, joursParMois.getOrDefault(mois, 0.0) + jours);
            }
        }

        for (Map.Entry<String, Double> entry : joursParMois.entrySet()) {
            evolution.add(EvolutionConges.builder()
                    .mois(entry.getKey())
                    .joursPris(entry.getValue())
                    .build());
        }

        return evolution;
    }

    private List<DemandesAccepteesService> calculerDemandesAccepteesServices(String serviceChef, String directionChef, LocalDateTime dateDebut, LocalDateTime dateFin) {
        List<Object[]> results = demandeRepository.countAcceptedDemandesByServiceAndDirectionAndDateRange(
                directionChef, dateDebut, dateFin);

        return results.stream()
                .map(result -> DemandesAccepteesService.builder()
                        .service((String) result[0])
                        .direction((String) result[1])
                        .demandesAcceptees((Long) result[2])
                        .build())
                .collect(Collectors.toList());
    }

    private List<SoldeEmploye> calculerSoldesEmployes(List<Employe> employesService) {
        List<SoldeEmploye> soldes = new ArrayList<>();

        // Trouver le solde maximum
        double maxSolde = 0;
        Map<String, Double> soldeMap = new HashMap<>();

        for (Employe employe : employesService) {
            Optional<SoldeConge> soldeOpt = soldeCongeRepository.findTopByEmployeMatriculeOrderByAnneeDesc(employe.getMatricule());
            double solde = soldeOpt.map(SoldeConge::getSoldeActuel).orElse(0.0f);
            soldeMap.put(employe.getMatricule(), solde);

            if (solde > maxSolde) {
                maxSolde = solde;
            }
        }

        // Construire la liste des soldes
        for (Employe employe : employesService) {
            double solde = soldeMap.getOrDefault(employe.getMatricule(), 0.0);

            soldes.add(SoldeEmploye.builder()
                    .matricule(employe.getMatricule())
                    .nom(employe.getNom())
                    .prenom(employe.getPrenom())
                    .solde(solde)
                    .plusGrandSolde(solde == maxSolde)
                    .build());
        }

        return soldes;
    }
}