// DashboardChefServiceImpl.java
package com.mercedes.workflowrh.service.impl;

import com.mercedes.workflowrh.dto.dashboardDto.*;
import com.mercedes.workflowrh.entity.*;
import com.mercedes.workflowrh.repository.DemandeRepository;
import com.mercedes.workflowrh.repository.EmployeRepository;
import com.mercedes.workflowrh.repository.SoldeCongeRepository;
import com.mercedes.workflowrh.service.DashboardChefService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
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
        // Validate dates
        if (dateDebut.isAfter(dateFin)) {
            throw new IllegalArgumentException("La date de début ne peut pas être après la date de fin");
        }

        // Récupérer le service du chef
        Employe chef = employeRepository.findByMatricule(matriculeChef)
                .orElseThrow(() -> new RuntimeException("Chef non trouvé"));
        String serviceChef = chef.getService();

        // Récupérer tous les employés du service
        List<Employe> employesService = employeRepository.findByService(serviceChef);

        if (employesService.isEmpty()) {
            return createEmptyDashboard();
        }

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

    private DashboardChefDTO createEmptyDashboard() {
        return DashboardChefDTO.builder()
                .vueEnsembleDemandes(VueEnsembleDemandes.builder()
                        .totalDemandes(0L)
                        .totalConges(0L)
                        .totalAutorisations(0L)
                        .totalOrdresMission(0L)
                        .pourcentageConges(0.0)
                        .pourcentageAutorisations(0.0)
                        .pourcentageOrdresMission(0.0)
                        .build())
                .repartitionStatuts(RepartitionStatuts.builder()
                        .enCours(0L)
                        .validees(0L)
                        .refusees(0L)
                        .pourcentageEnCours(0.0)
                        .pourcentageValidees(0.0)
                        .pourcentageRefusees(0.0)
                        .build())
                .joursCongesPris(JoursCongesPris.builder()
                        .joursPris(0.0)
                        .soldeTotal(0.0)
                        .pourcentagePris(0.0)
                        .soldeRestant(0.0)
                        .evolutionParMois(new ArrayList<>())
                        .build())
                .demandesAccepteesServices(new ArrayList<>())
                .soldesEmployes(new ArrayList<>())
                .build();
    }

    private VueEnsembleDemandes calculerVueEnsembleDemandes(List<String> matriculesService, LocalDateTime dateDebut, LocalDateTime dateFin) {
        // Total des demandes créées dans la période
        long totalDemandes = demandeRepository.countByEmployeMatriculeInAndDateCreationBetween(matriculesService, dateDebut, dateFin);

        // Congés (STANDARD + EXCEPTIONNEL)
        long totalConges = demandeRepository.countByEmployeMatriculeInAndCategorieInAndDateCreationBetween(
                matriculesService,
                Arrays.asList(CategorieDemande.CONGE_STANDARD, CategorieDemande.CONGE_EXCEPTIONNEL),
                dateDebut, dateFin
        );

        // Autorisations
        long totalAutorisations = demandeRepository.countByEmployeMatriculeInAndCategorieAndDateCreationBetween(
                matriculesService, CategorieDemande.AUTORISATION, dateDebut, dateFin
        );

        // Ordres de mission
        long totalOrdresMission = demandeRepository.countByEmployeMatriculeInAndCategorieAndDateCreationBetween(
                matriculesService, CategorieDemande.ORDRE_MISSION, dateDebut, dateFin
        );

        // Vérification de cohérence
        long sumCategories = totalConges + totalAutorisations + totalOrdresMission;
        if (totalDemandes != sumCategories) {
            // Log warning ou ajustement selon la logique métier
            totalDemandes = Math.max(totalDemandes, sumCategories);
        }

        // Calcul des pourcentages
        double pourcentageConges = totalDemandes > 0 ? (double) totalConges / totalDemandes * 100 : 0;
        double pourcentageAutorisations = totalDemandes > 0 ? (double) totalAutorisations / totalDemandes * 100 : 0;
        double pourcentageOrdresMission = totalDemandes > 0 ? (double) totalOrdresMission / totalDemandes * 100 : 0;

        return VueEnsembleDemandes.builder()
                .totalDemandes(totalDemandes)
                .totalConges(totalConges)
                .totalAutorisations(totalAutorisations)
                .totalOrdresMission(totalOrdresMission)
                .pourcentageConges(arrondirDeuxDecimales(pourcentageConges))
                .pourcentageAutorisations(arrondirDeuxDecimales(pourcentageAutorisations))
                .pourcentageOrdresMission(arrondirDeuxDecimales(pourcentageOrdresMission))
                .build();
    }

    private RepartitionStatuts calculerRepartitionStatuts(List<String> matriculesService, LocalDateTime dateDebut, LocalDateTime dateFin) {
        // Compter par statut
        long enCours = demandeRepository.countByEmployeMatriculeInAndStatutAndDateCreationBetween(
                matriculesService, StatutDemande.EN_COURS, dateDebut, dateFin
        );
        long validees = demandeRepository.countByEmployeMatriculeInAndStatutAndDateCreationBetween(
                matriculesService, StatutDemande.VALIDEE, dateDebut, dateFin
        );
        long refusees = demandeRepository.countByEmployeMatriculeInAndStatutAndDateCreationBetween(
                matriculesService, StatutDemande.REFUSEE, dateDebut, dateFin
        );


        long total = enCours + validees + refusees ;

        // Calcul des pourcentages
        double pourcentageEnCours = total > 0 ? (double) enCours / total * 100 : 0;
        double pourcentageValidees = total > 0 ? (double) validees / total * 100 : 0;
        double pourcentageRefusees = total > 0 ? (double) refusees / total * 100 : 0;

        return RepartitionStatuts.builder()
                .enCours(enCours)
                .validees(validees)
                .refusees(refusees)

                .pourcentageEnCours(arrondirDeuxDecimales(pourcentageEnCours))
                .pourcentageValidees(arrondirDeuxDecimales(pourcentageValidees))
                .pourcentageRefusees(arrondirDeuxDecimales(pourcentageRefusees))
                .build();
    }


    private JoursCongesPris calculerJoursCongesPris(List<String> matriculesService, LocalDateTime dateDebut, LocalDateTime dateFin) {
        // DEBUG: Add logging to see what's happening
        System.out.println("=== DEBUG JoursCongesPris ===");
        System.out.println("Date range: " + dateDebut + " to " + dateFin);
        System.out.println("Employees in service: " + matriculesService.size());

        // Get ALL validated leaves first (without date filter)
        List<Demande> allCongesValides = demandeRepository.findByEmployeMatriculeInAndStatutAndCategorieIn(
                matriculesService,
                StatutDemande.VALIDEE,
                Arrays.asList(CategorieDemande.CONGE_STANDARD, CategorieDemande.CONGE_EXCEPTIONNEL)
        );

        // Filter manually by ACTUAL LEAVE DATES instead of creation dates
        List<Demande> congesValides = allCongesValides.stream()
                .filter(demande -> demande.getCongeDateDebut() != null && demande.getCongeDateFin() != null)
                .filter(demande -> {
                    LocalDateTime congeDebut = demande.getCongeDateDebut().atStartOfDay();
                    LocalDateTime congeFin = demande.getCongeDateFin().atStartOfDay();
                    // Check if the leave period overlaps with the requested period
                    return (congeDebut.isBefore(dateFin) || congeDebut.isEqual(dateFin)) &&
                            (congeFin.isAfter(dateDebut) || congeFin.isEqual(dateDebut));
                })
                .collect(Collectors.toList());

        System.out.println("DEBUG: Congés valides trouvés: " + congesValides.size());

        // Filtrer seulement les types qui déduisent du solde
        List<Demande> filteredConges = congesValides.stream()
                .filter(demande -> demande.getTypeDemande() != null && demande.getTypeDemande().deductsFromSolde())
                .filter(demande -> demande.getCongeDateDebut() != null && demande.getCongeDateFin() != null)
                .collect(Collectors.toList());

        System.out.println("DEBUG: Congés après filtrage: " + filteredConges.size());

        // Calculer les jours pris (jours ouvrables uniquement)
        double joursPris = filteredConges.stream()
                .mapToDouble(demande -> {
                    LocalDate debut = demande.getCongeDateDebut();
                    LocalDate fin = demande.getCongeDateFin();

                    // Calculate only the portion within the requested period
                    LocalDate debutPeriode = debut.isBefore(dateDebut.toLocalDate()) ?
                            dateDebut.toLocalDate() : debut;
                    LocalDate finPeriode = fin.isAfter(dateFin.toLocalDate()) ?
                            dateFin.toLocalDate() : fin;

                    double jours = calculerJoursOuvrables(debutPeriode, finPeriode);
                    System.out.println("DEBUG - Congé: " + debut + " to " + fin + " = " + jours + " jours dans la période");
                    return jours;
                })
                .sum();

        // Calculer le solde total du service (solde actuel de tous les employés)
        double soldeTotal = matriculesService.stream()
                .mapToDouble(matricule -> {
                    Optional<SoldeConge> soldeOpt = soldeCongeRepository.findTopByEmployeMatriculeOrderByAnneeDesc(matricule);
                    double solde = soldeOpt.map(SoldeConge::getSoldeActuel).orElse(0.0f);
                    return solde;
                })
                .sum();

        System.out.println("DEBUG - Jours pris totaux: " + joursPris);
        System.out.println("DEBUG - Solde total service: " + soldeTotal);

        // Calculs dérivés
        double pourcentagePris = soldeTotal > 0 ? (joursPris / soldeTotal) * 100 : 0;
        double soldeRestant = Math.max(0, soldeTotal - joursPris);

        System.out.println("DEBUG - Pourcentage: " + pourcentagePris + "%");
        System.out.println("DEBUG - Solde restant: " + soldeRestant);

        // Calculer l'évolution par mois
        List<EvolutionConges> evolutionParMois = calculerEvolutionCongesParMois(filteredConges, dateDebut, dateFin);

        return JoursCongesPris.builder()
                .joursPris(arrondirDeuxDecimales(joursPris))
                .soldeTotal(arrondirDeuxDecimales(soldeTotal))
                .pourcentagePris(arrondirDeuxDecimales(pourcentagePris))
                .soldeRestant(arrondirDeuxDecimales(soldeRestant))
                .evolutionParMois(evolutionParMois)
                .build();
    }
    /**
     * Calcule les jours ouvrables entre deux dates (exclut les weekends)
     */
    private double calculerJoursOuvrables(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null || dateDebut.isAfter(dateFin)) {
            return 0;
        }

        long joursOuvrables = 0;
        LocalDate currentDate = dateDebut;

        while (!currentDate.isAfter(dateFin)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                joursOuvrables++;
            }
            currentDate = currentDate.plusDays(1);
        }

        return joursOuvrables;
    }

    private List<EvolutionConges> calculerEvolutionCongesParMois(List<Demande> congesValides, LocalDateTime dateDebut, LocalDateTime dateFin) {
        Map<String, Double> joursParMois = new TreeMap<>();

        // Initialiser tous les mois de la période
        LocalDateTime current = dateDebut.withDayOfMonth(1);
        LocalDateTime end = dateFin.withDayOfMonth(1);

        while (!current.isAfter(end)) {
            String moisKey = current.getMonth().toString() + " " + current.getYear();
            joursParMois.put(moisKey, 0.0);
            current = current.plusMonths(1);
        }

        // Compter les jours par mois pour chaque congé
        for (Demande demande : congesValides) {
            if (demande.getCongeDateDebut() != null && demande.getCongeDateFin() != null) {
                LocalDate debutConge = demande.getCongeDateDebut();
                LocalDate finConge = demande.getCongeDateFin();

                // Répartir les jours par mois
                LocalDate currentMonth = debutConge.withDayOfMonth(1);
                LocalDate lastMonth = finConge.withDayOfMonth(1);

                while (!currentMonth.isAfter(lastMonth)) {
                    String moisKey = currentMonth.getMonth().toString() + " " + currentMonth.getYear();

                    // Déterminer la période pour ce mois
                    LocalDate debutMois = currentMonth;
                    LocalDate finMois = currentMonth.plusMonths(1).minusDays(1);

                    LocalDate debutPeriode = debutConge.isAfter(debutMois) ? debutConge : debutMois;
                    LocalDate finPeriode = finConge.isBefore(finMois) ? finConge : finMois;

                    if (!debutPeriode.isAfter(finPeriode)) {
                        double joursMois = calculerJoursOuvrables(debutPeriode, finPeriode);
                        joursParMois.put(moisKey, joursParMois.get(moisKey) + joursMois);
                    }

                    currentMonth = currentMonth.plusMonths(1);
                }
            }
        }

        return joursParMois.entrySet().stream()
                .map(entry -> EvolutionConges.builder()
                        .mois(entry.getKey())
                        .joursPris(arrondirDeuxDecimales(entry.getValue()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<DemandesAccepteesService> calculerDemandesAccepteesServices(String serviceChef, String directionChef, LocalDateTime dateDebut, LocalDateTime dateFin) {
        try {
            List<Object[]> results = demandeRepository.countAcceptedDemandesByServiceAndDirectionAndDateRange(
                    directionChef, dateDebut, dateFin);

            return results.stream()
                    .map(result -> DemandesAccepteesService.builder()
                            .service((String) result[0])
                            .direction((String) result[1])
                            .demandesAcceptees((Long) result[2])
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Retourner une liste vide en cas d'erreur
            return new ArrayList<>();
        }
    }

    private List<SoldeEmploye> calculerSoldesEmployes(List<Employe> employesService) {
        List<SoldeEmploye> soldes = new ArrayList<>();

        if (employesService.isEmpty()) {
            return soldes;
        }

        // Première passe: calculer les soldes et trouver le maximum
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

        // Deuxième passe: construire la liste des soldes
        for (Employe employe : employesService) {
            double solde = soldeMap.getOrDefault(employe.getMatricule(), 0.0);

            soldes.add(SoldeEmploye.builder()
                    .matricule(employe.getMatricule())
                    .nom(employe.getNom())
                    .prenom(employe.getPrenom())
                    .solde(arrondirDeuxDecimales(solde))
                    .plusGrandSolde(Math.abs(solde - maxSolde) < 0.001) // Tolérance pour les comparaisons de float/double
                    .build());
        }

        // Trier par solde décroissant
        soldes.sort((s1, s2) -> Double.compare(s2.getSolde(), s1.getSolde()));

        return soldes;
    }

    /**
     * Méthode utilitaire pour arrondir à 2 décimales
     */
    private double arrondirDeuxDecimales(double valeur) {
        return Math.round(valeur * 100.0) / 100.0;
    }
}