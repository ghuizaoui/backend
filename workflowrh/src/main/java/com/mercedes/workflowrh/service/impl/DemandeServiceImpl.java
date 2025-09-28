package com.mercedes.workflowrh.service.impl;

import com.mercedes.workflowrh.dto.DemandeDetailDTO;
import com.mercedes.workflowrh.dto.DemandeListDTO;
import com.mercedes.workflowrh.dto.dashboardDto.*;
import com.mercedes.workflowrh.entity.*;
import com.mercedes.workflowrh.repository.DemandeRepository;
import com.mercedes.workflowrh.repository.EmployeRepository;
import com.mercedes.workflowrh.repository.HistoriqueDemandeRepository;
import com.mercedes.workflowrh.service.DemandeService;
import com.mercedes.workflowrh.service.MailService;
import com.mercedes.workflowrh.service.NotificationService;
import com.mercedes.workflowrh.service.SoldeCongeService;
import com.mercedes.workflowrh.service.impl.FileUploadService;
import com.mercedes.workflowrh.service.impl.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j

@Service
@RequiredArgsConstructor
public class DemandeServiceImpl implements DemandeService {

    private final DemandeRepository demandeRepository;
    private final EmployeRepository employeRepository;
    private final HistoriqueDemandeRepository historiqueDemandeRepository;
    private final NotificationService notificationService;
    private final MailService mailService;
    private final SoldeCongeService soldeCongeService;
    private final FileUploadService fileUploadService;
    private final HolidayService holidayService;

    @Override
    @Transactional
    public Demande createCongeStandard(
            TypeDemande typeDemande,
            LocalDate dateDebut, LocalTime heureDebut,
            LocalDate dateFin, LocalTime heureFin) {

        assertType(typeDemande, CategorieDemande.CONGE_STANDARD);
        Employe employe = currentEmployeOr404();
        validateDates(dateDebut, dateFin);

        // Check solde if deducts (though for standard, always true)
        long joursOuvres = holidayService.calculateWorkingDays(dateDebut, dateFin);
        float soldeActuel = soldeCongeService.getSoldeActuel(employe.getMatricule()).orElseThrow().getSoldeActuel();
        if (soldeActuel < joursOuvres) {
            throw bad("Solde insuffisant pour ce congé.");
        }

        Demande d = Demande.builder()
                .employe(employe)
                .statut(StatutDemande.EN_COURS)
                .categorie(typeDemande.getCategorie())
                .typeDemande(typeDemande)
                .congeDateDebut(dateDebut)
                .congeHeureDebut(heureDebut)
                .congeDateFin(dateFin)
                .congeHeureFin(heureFin)
                .workflowId(UUID.randomUUID().toString())
                .dateCreation(LocalDateTime.now())
                .build();

        Demande saved = demandeRepository.save(d); // Save only the Demande
        notificationService.notifyManagerOfNewDemand(saved,employe.getService()); // Notify manager
        return saved;
    }
    @Override
    public Employe getEmployeByMatricule(String matricule) {
        if (matricule == null || matricule.isBlank()) {
            throw new IllegalArgumentException("Le matricule ne peut pas être vide");
        }

        return employeRepository.findByMatricule(matricule)
                .orElseThrow(() -> new RuntimeException(
                        "Aucun employé trouvé avec le matricule : " + matricule
                ));
    }

    @Override
    public List<Demande> getAll() {
        return demandeRepository.findAll();
    }

    // -------------------- KPI Dashboard --------------------

    @Override
    public long countByStatut(StatutDemande statut) {
        return demandeRepository.countByStatut(statut);
    }

    @Override
    public long countByCategorie(CategorieDemande categorie) {
        return demandeRepository.countByCategorie(categorie);
    }

    @Override
    public long countByDateCreationBetween(LocalDateTime start, LocalDateTime end) {
        return demandeRepository.countByDateCreationBetween(start, end);
    }

    @Override
    public long countByStatutAndDateCreationBetween(StatutDemande statut, LocalDateTime start, LocalDateTime end) {
        return demandeRepository.countByStatutAndDateCreationBetween(statut, start, end);
    }

    @Override
    public List<Object[]> countDemandesByEmploye() {
        return demandeRepository.countDemandesByEmploye();
    }

    @Override
    public List<Object[]> countDemandesByService() {
        return demandeRepository.countDemandesByService();
    }

    @Override
    public List<Object[]> countDemandesByCategorie() {
        return demandeRepository.countDemandesByCategorie();
    }

    @Override
    public Double averageValidationTimeSeconds() {
        return demandeRepository.averageValidationTimeSeconds();
    }

    @Override
    public List<Object[]> countByStatutGrouped() {
        return demandeRepository.countByStatutGrouped();
    }

    // -------------------- Chart / Time-series --------------------
    @Override
    public List<Object[]> countDemandesPerMonth(LocalDateTime start, LocalDateTime end) {
        return demandeRepository.countDemandesPerMonth(start, end);
    }

    @Override
    public List<Object[]> countDemandesPerMonthAndYear() {
        return demandeRepository.countDemandesPerMonthAndYear();
    }

    @Override
    public List<Object[]> countDemandesByCategoriePerMonth(LocalDateTime start, LocalDateTime end) {
        return demandeRepository.countDemandesByCategoriePerMonth(start, end);
    }

    @Override
    public List<Object[]> countDemandesByType() {
        return demandeRepository.countDemandesByType();
    }

    // -------------------- Search / Filter --------------------
    @Override
    public List<Demande> findByEmployeAndStatut(String matricule, StatutDemande statut) {
        Employe e = employeRepository.findByMatricule(matricule)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employé non trouvé"));
        return demandeRepository.findByEmployeAndStatut(e, statut);
    }

    @Override
    public List<Demande> findByTypeDemande(TypeDemande typeDemande) {
        return demandeRepository.findByTypeDemande(typeDemande);
    }

    @Override
    public List<Demande> findByDateCreationBetween(LocalDateTime start, LocalDateTime end) {
        return demandeRepository.findByDateCreationBetween(start, end);
    }

    @Override
    public long countByEmployeAndDateRange(String matricule, LocalDateTime start, LocalDateTime end) {
        Employe e = employeRepository.findByMatricule(matricule)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employé non trouvé"));
        return demandeRepository.countByEmployeAndDateCreationBetween(e, start, end);
    }

    @Override
    public void delete(Demande demande) {
        demandeRepository.delete(demande);
    }

    @Override
    public Demande getById(long demandeId) {
        return demandeRepository.findById(demandeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande non trouvée"));
    }

    //-----------------------------------------------------------------------------------------------------

    @Override
    @Transactional
    public Demande  createCongeExceptionnel(
            TypeDemande typeDemande,
            LocalDate dateDebut, LocalTime heureDebut,
            LocalDate dateFin, LocalTime heureFin,String interimaireMatricule,
            MultipartFile file) { // Added file parameter

        assertType(typeDemande, CategorieDemande.CONGE_EXCEPTIONNEL);
        Employe employe = currentEmployeOr404();
        validateDates(dateDebut, dateFin);

        // Check solde if deducts
        if (typeDemande.deductsFromSolde()) {
            long joursOuvres = holidayService.calculateWorkingDays(dateDebut, dateFin);
            float soldeActuel = soldeCongeService.getSoldeActuel(employe.getMatricule()).orElseThrow().getSoldeActuel();
            if (soldeActuel < joursOuvres) {
                throw bad("Solde insuffisant pour ce congé.");
            }
        }

        // Process the uploaded file
        byte[] fileData = fileUploadService.processFile(file);

        Demande d = Demande.builder()
                .employe(employe)
                .statut(StatutDemande.EN_COURS)
                .categorie(typeDemande.getCategorie())
                .typeDemande(typeDemande)
                .congeDateDebut(dateDebut)
                .congeHeureDebut(heureDebut)
                .congeDateFin(dateFin)
                .congeHeureFin(heureFin)
                .file(fileData) // Store the file data
                .workflowId(UUID.randomUUID().toString())
                .dateCreation(LocalDateTime.now())
                .build();

        Demande saved = demandeRepository.save(d); // Save only the Demande
        notificationService.notifyManagerOfNewDemand(saved,employe.getService()); // Notify manager
        notificationService.notifyInterimaire(interimaireMatricule,d);
        return saved;
    }
    @Override
    @Transactional
    public Demande createAutorisation(
            TypeDemande typeDemande,
            // PRÉVU
            LocalDate dateAutorisation,
            LocalTime heureDebut,
            LocalTime heureFin,
            // RÉEL (optionnel)
            LocalDate dateReelle,
            LocalTime heureSortieReelle,
            LocalTime heureRetourReel) {

        assertType(typeDemande, CategorieDemande.AUTORISATION);
        Employe employe = currentEmployeOr404();

        // --- validations PRÉVU ---
        if (dateAutorisation == null || heureDebut == null || heureFin == null) {
            throw bad("Jour et heures prévues obligatoires.");
        }
        if (heureDebut.isAfter(heureFin)) {
            throw bad("Plage prévue invalide (début > fin).");
        }

        // --- validations RÉEL (si fourni) ---
        boolean anyRealProvided = (dateReelle != null) || (heureSortieReelle != null) || (heureRetourReel != null);
        if (anyRealProvided) {
            // si un champ réel est donné, les 3 doivent l'être
            if (dateReelle == null || heureSortieReelle == null || heureRetourReel == null) {
                throw bad("Si vous renseignez le réel, fournissez date réelle, heure de sortie réelle et heure de retour réelle.");
            }
            // par règle métier : réel sur le même jour (ou lever l'exception si différent)
            if (!dateAutorisation.equals(dateReelle)) {
                throw bad("L'autorisation est journalière : la date réelle doit être égale au jour prévu.");
            }
            // Log the input values for debugging
            if (heureSortieReelle.isAfter(heureRetourReel)) {
                throw bad("Plage réelle invalide (sortie > retour).");
            }
        }

        Demande d = Demande.builder()
                .employe(employe)
                .statut(StatutDemande.EN_COURS)
                .categorie(typeDemande.getCategorie()) // AUTORISATION
                .typeDemande(typeDemande)
                // PRÉVU
                .autoDate(dateAutorisation)
                .autoHeureDebut(heureDebut)
                .autoHeureFin(heureFin)
                // RÉEL (optionnel)
                .autoDateReelle(dateReelle)                   // peut être null
                .autoHeureSortieReelle(heureSortieReelle)     // peut être null
                .autoHeureRetourReel(heureRetourReel)         // peut être null
                .workflowId(UUID.randomUUID().toString())
                .dateCreation(LocalDateTime.now())
                .build();

        Demande saved = demandeRepository.save(d); // Save only the Demande
        notificationService.notifyManagerOfNewDemand(saved,employe.getService()); // Notify manager

        return saved;
    }
    @Override
    @Transactional
    public Demande createOrdreMission(
            LocalDate dateDebut, LocalTime heureDebut,
            LocalDate dateFin, LocalTime heureFin,
            String missionObjet) {

        Employe employe = currentEmployeOr404();

        if (dateDebut == null || dateFin == null || heureDebut == null || heureFin == null)
            throw bad("Dates/heures obligatoires.");
        if (invalidInterval(dateDebut, heureDebut, dateFin, heureFin))
            throw bad("Intervalle invalide.");
        if (missionObjet == null || missionObjet.isBlank())
            throw bad("Objet obligatoire.");

        Demande d = Demande.builder()
                .employe(employe)
                .statut(StatutDemande.EN_COURS)
                .categorie(CategorieDemande.ORDRE_MISSION)
                .typeDemande(null)
                .missionDateDebut(dateDebut)
                .missionHeureDebut(heureDebut)
                .missionDateFin(dateFin)
                .missionHeureFin(heureFin)
                .missionObjet(missionObjet)
                .workflowId(UUID.randomUUID().toString())
                .dateCreation(LocalDateTime.now())
                .build();

        Demande saved = demandeRepository.save(d); // Save only the Demande
        notificationService.notifyManagerOfNewDemand(saved, employe.getService()); // Notify manager

        return saved;
    }
    @Override
    public DemandeDetailDTO findDetail(Long id) {
        Demande d = demandeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande non trouvée"));
        return toDetail(d);
    }





    @Override
    @Transactional(readOnly = true)
    public List<DemandeListDTO> findAllForChef(String matriculeChef, long chefLevel, String service) {
        log.info(">>> findAllForChef called with matriculeChef={}, chefLevel={}, service={}",
                matriculeChef, chefLevel, service);

        if ((chefLevel != 1 && chefLevel != 2) || service == null || service.isBlank()) {
            log.warn(">>> Invalid parameters: chefLevel={}, service={}", chefLevel, service);
            return Collections.emptyList();
        }

        // 🔹 Récupérer toutes les demandes du service
        List<Demande> demandes = demandeRepository.findDemandesByService(service);

        // 🔹 Filtrer selon le rôle (chef1 ou chef2)
        List<Demande> filteredDemandes = demandes.stream()
                .filter(d -> {
                    String matriculeEmploye = d.getEmploye().getMatricule();

                    // ✅ Exclure toujours les demandes du chef lui-même
                    if (matriculeEmploye.equals(matriculeChef)) {
                        return false;
                    }

                    if (chefLevel == 1) {
                        // Chef1 : exclure aussi les demandes du chef2
                        return !matriculeEmploye.equals(d.getEmploye().getChefHierarchique2Matricule());
                    } else {
                        // Chef2 : voir uniquement les demandes des employés "simples"
                        // (donc exclure celles du chef1 et du chef2)
                        return !matriculeEmploye.equals(d.getEmploye().getChefHierarchique1Matricule())
                                && !matriculeEmploye.equals(d.getEmploye().getChefHierarchique2Matricule());
                    }
                })
                .collect(Collectors.toList());

        log.info(">>> {} demandes retained after filtering for chefLevel={} matricule={}",
                filteredDemandes.size(), chefLevel, matriculeChef);

        return filteredDemandes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    private DemandeListDTO convertToDTO(Demande demande) {
        return DemandeListDTO.builder()
                .id(demande.getId())
                .employeMatricule(demande.getEmploye().getMatricule())
                .employeNom(demande.getEmploye().getNom())
                .employePrenom(demande.getEmploye().getPrenom())
                .categorie(demande.getCategorie())
                .typeDemande(demande.getTypeDemande())

                .statut(demande.getStatut())
                .dateCreation(demande.getDateCreation())
                .build();
    }


    @Override
    public List<DemandeListDTO> findAllForDrh() {
        // Optionnel : vérifier que l'utilisateur connecté est DRH dans le controller
        return demandeRepository.findAllForDrhValidation()
                .stream()
                .map(this::toListItem)
                .toList();
    }

    private DemandeListDTO toListItem(Demande d) {
        LocalDate dDebut = null, dFin = null;

        switch (d.getCategorie()) {
            case AUTORISATION -> {
                dDebut = d.getAutoDate();
                dFin = d.getAutoDate();
            }
            case ORDRE_MISSION -> {
                dDebut = d.getMissionDateDebut();
                dFin = d.getMissionDateFin();
            }
            case CONGE_STANDARD, CONGE_EXCEPTIONNEL -> {
                dDebut = d.getCongeDateDebut();
                dFin = d.getCongeDateFin();
            }
        }

        return DemandeListDTO.builder()
                .id(d.getId())
                .employeMatricule(d.getEmploye() != null ? d.getEmploye().getMatricule() : null)
                .employeNom(d.getEmploye() != null ? d.getEmploye().getNom() : null)
                .employePrenom(d.getEmploye() != null ? d.getEmploye().getPrenom() : null)
                .categorie(d.getCategorie())
                .typeDemande(d.getTypeDemande())
                .dateDebut(dDebut)
                .dateFin(dFin)
                .statut(d.getStatut())
                .dateCreation(d.getDateCreation())
                .build();
    }

    private DemandeDetailDTO toDetail(Demande d) {
        return DemandeDetailDTO.builder()
                .id(d.getId())
                .employeMatricule(d.getEmploye() != null ? d.getEmploye().getMatricule() : null)
                .employeNom(d.getEmploye() != null ? d.getEmploye().getNom() : null)
                .employePrenom(d.getEmploye() != null ? d.getEmploye().getPrenom() : null)
                .employeEmail(d.getEmploye() != null ? d.getEmploye().getEmail() : null)
                .categorie(d.getCategorie())
                .typeDemande(d.getTypeDemande())
                .statut(d.getStatut())
                .commentaireRefus(d.getCommentaireRefus())
                .dateCreation(d.getDateCreation())
                .dateValidation(d.getDateValidation())
                .congeDateDebut(d.getCongeDateDebut())
                .congeHeureDebut(d.getCongeHeureDebut())
                .congeDateFin(d.getCongeDateFin())
                .congeHeureFin(d.getCongeHeureFin())
                .autoDate(d.getAutoDate())
                .autoHeureDebut(d.getAutoHeureDebut())
                .autoHeureFin(d.getAutoHeureFin())
                .autoDateReelle(d.getAutoDateReelle())
                .autoHeureSortieReelle(d.getAutoHeureSortieReelle())
                .autoHeureRetourReel(d.getAutoHeureRetourReel())
                .missionDateDebut(d.getMissionDateDebut())
                .missionHeureDebut(d.getMissionHeureDebut())
                .missionDateFin(d.getMissionDateFin())
                .missionHeureFin(d.getMissionHeureFin())
                .missionObjet(d.getMissionObjet())
                .build();
    }

    // -------------------- Helpers sécurité/validation --------------------
    @Override
    @Transactional
    public Demande validerDemande(Long demandeId, String matriculeValidateur) {
        Demande d = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande non trouvée"));

        Employe validateur = employeRepository.findByMatricule(matriculeValidateur)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Validateur non trouvé"));

//        if (!estValidateurAutorise(d, validateur)) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorisé.");
//        }

        d.setStatut(StatutDemande.VALIDEE);
        d.setValidateur(validateur);
        d.setCommentaireRefus(null);
        d.setDateValidation(LocalDateTime.now());

        HistoriqueDemande h = HistoriqueDemande.creerHistoriqueValidation(d);
        historiqueDemandeRepository.save(h);

        Demande saved = demandeRepository.save(d);
        notificationService.notifyEmployeeOnValidation(saved);

        // Deduct solde if applicable
        Employe employe = saved.getEmploye();
        if (employe != null) {
            double joursPris = 0;
            try {
                if (saved.isCongeStandard() || saved.isCongeExceptionnel()) {
                    if (saved.getTypeDemande() != null && saved.getTypeDemande().deductsFromSolde()) {
                        if (saved.getCongeDateDebut() != null && saved.getCongeDateFin() != null) {
                            joursPris = holidayService.calculateWorkingDays(saved.getCongeDateDebut(), saved.getCongeDateFin());
                        }
                    }
                } else if (saved.isAutorisation()) {
                    LocalTime debut = saved.getAutoHeureSortieReelle() != null ? saved.getAutoHeureSortieReelle() : saved.getAutoHeureDebut();
                    LocalTime fin = saved.getAutoHeureRetourReel() != null ? saved.getAutoHeureRetourReel() : saved.getAutoHeureFin();
                    if (debut != null && fin != null && saved.getAutoDate() != null && !holidayService.isFreeDay(saved.getAutoDate())) {
                        long minutes = ChronoUnit.MINUTES.between(debut, fin);
                        joursPris = (minutes / 240.0) * 0.5;
                    }
                }

                if (joursPris > 0) {
                    soldeCongeService.debiterSoldeConge(employe, joursPris, saved.getCategorie());
                }
            } catch (RuntimeException e) {
                System.err.println("Erreur lors du débit du solde pour la demande ID " + demandeId + ": " + e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Erreur lors de la mise à jour du solde de congé: " + e.getMessage());
            }
        }

        return saved;
    }
    @Override
    @Transactional
    public Demande refuserDemande(Long demandeId, String matriculeValidateur, String commentaire) {
        Demande d = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande non trouvée"));

        Employe validateur = employeRepository.findByMatricule(matriculeValidateur)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Validateur non trouvé"));

        if (!estValidateurAutorise(d, validateur)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorisé.");
        }

        d.setStatut(StatutDemande.REFUSEE);
        d.setValidateur(validateur);
        d.setCommentaireRefus(commentaire);
        d.setDateValidation(LocalDateTime.now());

        HistoriqueDemande h = HistoriqueDemande.creerHistoriqueRefus(d, commentaire);
        historiqueDemandeRepository.save(h);

        Demande saved = demandeRepository.save(d);

        // ✅ Notif WebSocket + e-mail à l’employé créateur (avec motif)
        notificationService.notifyEmployeeOnRefuse(saved);

        // Pas de mise à jour de solde en cas de refus (logique métier)

        return saved;
    }

    private boolean estValidateurAutorise(Demande demande, Employe validateur) {
        Role roleCreat = demande.getEmploye() != null ? demande.getEmploye().getRole() : null;
        if (roleCreat == null) return false;

        boolean chefPeut = (validateur.getRole() == Role.CHEF) && (roleCreat == Role.EMPLOYE);
        boolean drhPeut = (validateur.getRole() == Role.DRH) && (roleCreat == Role.CHEF);

        return chefPeut || drhPeut;
    }

    private Employe currentEmployeOr404() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getName() == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String matricule = a.getName();
        return employeRepository.findById(matricule)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void assertType(TypeDemande type, CategorieDemande expected) {
        if (type == null || type.getCategorie() != expected)
            throw bad("Type incompatible.");
    }

    private void validateDates(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null) throw bad("Dates obligatoires.");
        if (debut.isAfter(fin)) throw bad("Début > fin.");
    }

    private boolean invalidInterval(LocalDate d1, LocalTime t1, LocalDate d2, LocalTime t2) {
        return d1.isAfter(d2) || (d1.isEqual(d2) && t1.isAfter(t2));
    }

    private ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }

    // -------------------- Historisation (corrigé) --------------------

    private void historiserCreation(Demande saved) {
        HistoriqueDemande h = HistoriqueDemande.creerHistoriqueCreation(saved);
        historiqueDemandeRepository.save(h);
    }

    private void historiserModification(Demande saved) {
        HistoriqueDemande h = HistoriqueDemande.creerHistoriqueModification(saved);
        historiqueDemandeRepository.save(h);
    }

    private void historiserSuppression(Demande saved) {
        HistoriqueDemande h = HistoriqueDemande.creerHistoriqueSuppression(saved);
        historiqueDemandeRepository.save(h);
    }

    private void historiserValidation(Demande saved) {
        HistoriqueDemande h = HistoriqueDemande.creerHistoriqueValidation(saved);
        historiqueDemandeRepository.save(h);
    }

    private void historiserRefus(Demande saved, String motif) {
        HistoriqueDemande h = HistoriqueDemande.creerHistoriqueRefus(saved, motif);
        historiqueDemandeRepository.save(h);
    }

    @Override
    public List<Demande> getHistoriqueDemandes(String matricule) {
        Employe employe = employeRepository.findByMatricule(matricule)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employé non trouvé"));

        return demandeRepository.findByEmployeOrderByDateCreationDesc(employe);
    }

    /**
     * Récupère toutes les demandes en attente pour un chef donné.
     *
     * @param matriculeChef Le matricule du chef.
     * @return La liste des demandes en attente.
     */
    @Override
    public List<Demande> getDemandesEnAttente(String matriculeChef) {
        // On récupère toutes les demandes en attente pour ce chef
        // Notez que cela suppose que l'entité Demande a une relation avec l'employé
        // et que l'employé a un chef1 ou chef2
        List<Demande> allDemandes = demandeRepository.findByStatut(StatutDemande.EN_COURS);

        // On filtre la liste pour ne garder que les demandes des employés supervisés par ce chef
        return allDemandes.stream()
                .filter(demande -> {
                    Employe employe = demande.getEmploye();
                    return employe != null && (matriculeChef.equals(employe.getChefHierarchique1Matricule()) || matriculeChef.equals(employe.getChefHierarchique2Matricule()));
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Demande> getHistoriqueSubordonnes(String matriculeChef) {
        // On récupère toutes les demandes
        List<Demande> allDemandes = demandeRepository.findAll();

        // On garde uniquement celles des employés dont le chef1 ou chef2 correspond au matriculeChef
        return allDemandes.stream()
                .filter(demande -> {
                    Employe employe = demande.getEmploye();
                    return employe != null && (
                            matriculeChef.equals(employe.getChefHierarchique1Matricule()) ||
                                    matriculeChef.equals(employe.getChefHierarchique2Matricule())
                    );
                })
                .collect(Collectors.toList());
    }

    //*************************************************************** to update the dashboard

    // Add these methods to your DemandeServiceImpl class

    @Override
    public DashboardOverviewDTO getDashboardOverview(String startDate, String endDate) {
        LocalDateTime start = parseDateTime(startDate);
        LocalDateTime end = parseDateTime(endDate);

        long totalDemandes = countByDateCreationBetween(start, end);

        // For "total possible", we need to define what this means
        // This could be based on number of employees * working days in period
        // For simplicity, let's assume it's calculated differently
        long totalPossible = calculateTotalPossibleDemands(start, end);

        double percentage = totalPossible > 0 ? (double) totalDemandes / totalPossible * 100 : 0;

        return DashboardOverviewDTO.builder()
                .totalDemandes(totalDemandes)
                .totalPossible(totalPossible)
                .percentage(Math.round(percentage * 100.0) / 100.0)
                .build();
    }

    @Override
    public List<StatusDistributionDTO> getStatusDistribution(String startDate, String endDate) {
        LocalDateTime start = parseDateTime(startDate);
        LocalDateTime end = parseDateTime(endDate);

        long total = countByDateCreationBetween(start, end);

        List<StatusDistributionDTO> distribution = new ArrayList<>();

        for (StatutDemande status : StatutDemande.values()) {
            long count = countByStatutAndDateCreationBetween(status, start, end);
            double percentage = total > 0 ? (double) count / total * 100 : 0;

            distribution.add(StatusDistributionDTO.builder()
                    .status(status.name())
                    .count(count)
                    .percentage(Math.round(percentage * 100.0) / 100.0)
                    .build());
        }

        return distribution;
    }

    @Override
    public LeaveBalanceDTO getLeaveBalanceOverview(String startDate, String endDate) {
        // This requires calculating taken leaves and total balance
        // Implementation depends on how you track taken leaves

        LocalDateTime start = parseDateTime(startDate);
        LocalDateTime end = parseDateTime(endDate);

        List<Demande> congesValides = demandeRepository.findByStatutAndCategorieInAndDateCreationBetween(
                StatutDemande.VALIDEE,
                Arrays.asList(CategorieDemande.CONGE_STANDARD, CategorieDemande.CONGE_EXCEPTIONNEL),
                start, end
        );

        double joursPris = congesValides.stream()
                .filter(demande -> demande.getTypeDemande() != null && demande.getTypeDemande().deductsFromSolde())
                .mapToDouble(demande -> {
                    if (demande.getCongeDateDebut() != null && demande.getCongeDateFin() != null) {
                        return ChronoUnit.DAYS.between(demande.getCongeDateDebut(), demande.getCongeDateFin()) + 1;
                    }
                    return 0;
                })
                .sum();

        // Get total balance from all employees
        double soldeTotal = calculateTotalBalance();

        double percentagePris = soldeTotal > 0 ? (joursPris / soldeTotal) * 100 : 0;
        double soldeRestant = soldeTotal - joursPris;

        return LeaveBalanceDTO.builder()
                .joursPris(joursPris)
                .soldeTotal(soldeTotal)
                .percentagePris(Math.round(percentagePris * 100.0) / 100.0)
                .soldeRestant(soldeRestant)
                .build();
    }

    @Override
    public List<ServiceLeaveDTO> getLeaveBalanceByService(String startDate, String endDate) {
        List<ServiceLeaveDTO> result = new ArrayList<>();

        // Get all services
        List<String> services = employeRepository.findAll().stream()
                .map(Employe::getService)
                .distinct()
                .collect(Collectors.toList());

        LocalDateTime start = parseDateTime(startDate);
        LocalDateTime end = parseDateTime(endDate);

        double totalJoursPris = 0;
        Map<String, Double> serviceDaysMap = new HashMap<>();

        for (String service : services) {
            List<Employe> employes = employeRepository.findByService(service);
            double serviceJoursPris = 0;

            for (Employe employe : employes) {
                List<Demande> congesValides = demandeRepository.findByEmployeAndStatutAndCategorieInAndDateCreationBetween(
                        employe, StatutDemande.VALIDEE,
                        Arrays.asList(CategorieDemande.CONGE_STANDARD, CategorieDemande.CONGE_EXCEPTIONNEL),
                        start, end
                );

                serviceJoursPris += congesValides.stream()
                        .filter(d -> d.getTypeDemande() != null && d.getTypeDemande().deductsFromSolde())
                        .mapToDouble(d -> ChronoUnit.DAYS.between(d.getCongeDateDebut(), d.getCongeDateFin()) + 1)
                        .sum();
            }

            serviceDaysMap.put(service, serviceJoursPris);
            totalJoursPris += serviceJoursPris;
        }

        for (String service : services) {
            double joursPris = serviceDaysMap.getOrDefault(service, 0.0);
            double percentage = totalJoursPris > 0 ? (joursPris / totalJoursPris) * 100 : 0;

            result.add(ServiceLeaveDTO.builder()
                    .service(service)
                    .joursPris(joursPris)
                    .percentage(Math.round(percentage * 100.0) / 100.0)
                    .build());
        }

        return result;
    }

    @Override
    public List<AcceptedRequestsDTO> getAcceptedRequestsByService(String startDate, String endDate) {
        LocalDateTime start = parseDateTime(startDate);
        LocalDateTime end = parseDateTime(endDate);

        List<Object[]> results = demandeRepository.countAcceptedDemandesByServiceAndDateRange(start, end);

        return results.stream()
                .map(result -> AcceptedRequestsDTO.builder()
                        .service((String) result[0])
                        .demandesAcceptees((Long) result[1])
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<EmployeeLeaveBalanceDTO> getLeaveBalanceDetails(String service, String startDate, String endDate) {
        List<EmployeeLeaveBalanceDTO> result = new ArrayList<>();

        List<Employe> employes = service != null ?
                employeRepository.findByService(service) :
                employeRepository.findAll();

        // Find the highest balance in the group
        float highestBalance = employes.stream()
                .map(emp -> soldeCongeService.getSoldeActuel(emp.getMatricule())
                        .orElse(SoldeConge.builder().soldeActuel(0f).build())
                        .getSoldeActuel())
                .max(Float::compare)
                .orElse(0f);

        for (Employe employe : employes) {
            SoldeConge solde = soldeCongeService.getSoldeActuel(employe.getMatricule())
                    .orElse(SoldeConge.builder().soldeActuel(0f).build());

            result.add(EmployeeLeaveBalanceDTO.builder()
                    .matricule(employe.getMatricule())
                    .nom(employe.getNom())
                    .prenom(employe.getPrenom())
                    .service(employe.getService())
                    .soldeConges(solde.getSoldeActuel())
                    .hasHighestBalance(solde.getSoldeActuel() == highestBalance)
                    .build());
        }

        return result;
    }

    // In your DemandeService interface, add:
    @Override
    public List<StatusDistributionDTO> getStatusDistribution(LocalDateTime start, LocalDateTime end) {
        // Get the status counts
        List<Object[]> statusCounts = demandeRepository.countByStatutGroupedWithDateRange(start, end);

        // Get total count for percentage calculation
        Long totalCount = demandeRepository.countTotalByDateRange(start, end);

        if (totalCount == null || totalCount == 0) {
            // Return empty distribution if no data
            return new ArrayList<>();
        }

        // Convert to DTOs with percentages
        List<StatusDistributionDTO> distribution = new ArrayList<>();

        for (Object[] result : statusCounts) {
            StatutDemande status = (StatutDemande) result[0];
            Long count = (Long) result[1];

            double percentage = (double) count / totalCount * 100;

            distribution.add(StatusDistributionDTO.builder()
                    .status(status.name())
                    .count(count)
                    .percentage(Math.round(percentage * 100.0) / 100.0) // Round to 2 decimal places
                    .build());
        }

        // Ensure all statuses are represented, even if count is 0
        ensureAllStatusesRepresented(distribution, totalCount);

        return distribution;
    }

    private void ensureAllStatusesRepresented(List<StatusDistributionDTO> distribution, Long totalCount) {
        // Get all possible statuses
        Set<StatutDemande> allStatuses = new HashSet<>(Arrays.asList(StatutDemande.values()));

        // Remove statuses that are already in the distribution
        for (StatusDistributionDTO dto : distribution) {
            try {
                allStatuses.remove(StatutDemande.valueOf(dto.getStatus()));
            } catch (IllegalArgumentException e) {
                // Ignore if status string doesn't match enum
            }
        }

        // Add missing statuses with count 0
        for (StatutDemande missingStatus : allStatuses) {
            distribution.add(StatusDistributionDTO.builder()
                    .status(missingStatus.name())
                    .count(0L)
                    .percentage(0.0)
                    .build());
        }
    }
// dashboard employe and concearge *********************/////////////////////////////////////////////////////////

    @Override
    public EmployeDashboardDTO getEmployeDashboard(String matricule, String role) {
        Employe employe = getEmployeByMatricule(matricule);

        // Calculate date range for this month
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = LocalDateTime.now().withDayOfMonth(LocalDateTime.now().getMonth().length(LocalDate.now().isLeapYear()))
                .withHour(23).withMinute(59).withSecond(59);

        // Get KPIs
        long totalDemandes = countByEmployeAndDateRange(matricule, startOfMonth, endOfMonth);
        long demandesEnCours = findByEmployeAndStatut(matricule, StatutDemande.EN_COURS).size();
        long demandesValidees = findByEmployeAndStatut(matricule, StatutDemande.VALIDEE).size();
        long demandesRefusees = findByEmployeAndStatut(matricule, StatutDemande.REFUSEE).size();

        KPIData kpiData = KPIData.builder()
                .totalDemandes(totalDemandes)
                .demandesEnCours(demandesEnCours)
                .demandesValidees(demandesValidees)
                .demandesRefusees(demandesRefusees)
                .build();

        // Get recent demands (last 5)
        List<Demande> recentDemandes = demandeRepository.findTop3ByEmployeMatriculeOrderByDateCreationDesc(matricule)
                .stream()
                .sorted(Comparator.comparing(Demande::getDateCreation).reversed())
                .limit(5)
                .collect(Collectors.toList());

        List<DemandeRecente> demandesRecentes = recentDemandes.stream()
                .map(this::convertToDemandeRecente)
                .collect(Collectors.toList());

        // Get status distribution
        Map<String, Long> statutDistribution = getStatusDistributionForEmployee(matricule, startOfMonth, endOfMonth);

        // Get category distribution
        Map<String, Long> categorieDistribution = getCategoryDistributionForEmployee(matricule, startOfMonth, endOfMonth);

        // Build the dashboard DTO
        EmployeDashboardDTO dashboard = EmployeDashboardDTO.builder()
                .kpiData(kpiData)
                .demandesRecentes(demandesRecentes)
                .statutDistribution(statutDistribution)
                .categorieDistribution(categorieDistribution)
                .build();

        // Add today's authorizations only for concierge role
        if ("CONCIERGE".equals(role)) {
            List<AutorisationAujourdhui> autorisations = getAutorisationsForToday();
            dashboard.setAutorisationsAujourdhui(autorisations);
            dashboard.getKpiData().setAutorisationsAujourdhui((long) autorisations.size());
        }

        return dashboard;
    }

    @Override
    public List<AutorisationAujourdhui> getAutorisationsForToday() {
        LocalDate today = LocalDate.now();

        List<Demande> autorisationsAujourdhui = demandeRepository.findByCategorieAndAutoDate(
                CategorieDemande.AUTORISATION, today);

        return autorisationsAujourdhui.stream()
                .map(d -> {
                    AutorisationAujourdhui auth = new AutorisationAujourdhui();
                    auth.setDemandeId(d.getId());
                    auth.setEmployeNom(d.getEmploye().getNom());
                    auth.setEmployePrenom(d.getEmploye().getPrenom());
                    auth.setEmployeMatricule(d.getEmploye().getMatricule());
                    auth.setHeureDebut(d.getAutoHeureDebut() != null ? d.getAutoHeureDebut().toString() : "");
                    auth.setHeureFin(d.getAutoHeureFin() != null ? d.getAutoHeureFin().toString() : "");
                    auth.setService(d.getEmploye().getService());
                    return auth;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Demande> getDemandesValideesEtRefuseesDuService(
            String service){
        List<StatutDemande> statuts = Arrays.asList(StatutDemande.VALIDEE, StatutDemande.REFUSEE);
        return demandeRepository.findDemandesByServiceAndStatuts(service, statuts);
    }

    private Map<String, Long> getStatusDistributionForEmployee(String matricule, LocalDateTime start, LocalDateTime end) {
        List<Demande> employeeDemands = demandeRepository.findByEmployeAndDateCreationBetween(
                employeRepository.findByMatricule(matricule).orElseThrow(),
                start, end);

        return employeeDemands.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getStatut().name(),
                        Collectors.counting()
                ));
    }

    private Map<String, Long> getCategoryDistributionForEmployee(String matricule, LocalDateTime start, LocalDateTime end) {
        List<Demande> employeeDemands = demandeRepository.findByEmployeAndDateCreationBetween(
                employeRepository.findByMatricule(matricule).orElseThrow(),
                start, end);

        return employeeDemands.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getCategorie().name(),
                        Collectors.counting()
                ));
    }

    private DemandeRecente convertToDemandeRecente(Demande demande) {
        DemandeRecente recente = new DemandeRecente();
        recente.setId(demande.getId());
        recente.setCategorie(demande.getCategorie().name());
        recente.setTypeDemande(demande.getTypeDemande() != null ? demande.getTypeDemande().name() : "");
        recente.setStatut(demande.getStatut().name());
        recente.setDateCreation(demande.getDateCreation().toString());

        // Set appropriate dates based on category
        if (demande.isCongeStandard() || demande.isCongeExceptionnel()) {
            recente.setDateDebut(demande.getCongeDateDebut().toString());
            recente.setDateFin(demande.getCongeDateFin().toString());
        } else if (demande.isAutorisation()) {
            recente.setDateDebut(demande.getAutoDate().toString());
            recente.setDateFin(demande.getAutoDate().toString()); // Same day for authorization
        } else if (demande.isOrdreMission()) {
            recente.setDateDebut(demande.getMissionDateDebut().toString());
            recente.setDateFin(demande.getMissionDateFin().toString());
        }

        return recente;
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return LocalDateTime.of(2000, 1, 1, 0, 0); // Default start date
        }
        return LocalDateTime.parse(dateTimeStr);
    }

    private long calculateTotalPossibleDemands(LocalDateTime start, LocalDateTime end) {
        // This is a business logic decision
        // For example: number of employees * working days in the period
        long numberOfEmployees = employeRepository.count();
        long workingDays = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) + 1;

        return numberOfEmployees * workingDays;
    }

    private double calculateTotalDaysTaken(List<Demande> conges) {
        return conges.stream()
                .mapToDouble(demande -> {
                    if (demande.getCongeDateDebut() != null && demande.getCongeDateFin() != null) {
                        return ChronoUnit.DAYS.between(
                                demande.getCongeDateDebut(),
                                demande.getCongeDateFin()
                        ) + 1; // Inclusive
                    }
                    return 0;
                })
                .sum();
    }

    private double calculateTotalBalance() {
        return employeRepository.findAll().stream()
                .mapToDouble(emp -> soldeCongeService.getSoldeActuel(emp.getMatricule())
                        .orElse(SoldeConge.builder().soldeActuel(0f).build())
                        .getSoldeActuel())
                .sum();
    }


    @Override
    public   List<Demande> getChefsDemandes() {
        return  demandeRepository.getDemandeByEmployeDrhSuper();
    }
}