// src/main/java/com/mercedes/workflowrh/service/impl/DemandeServiceImpl.java
package com.mercedes.workflowrh.service.impl;

import com.mercedes.workflowrh.dto.DemandeDetailDTO;
import com.mercedes.workflowrh.dto.DemandeListDTO;
import com.mercedes.workflowrh.entity.*;
import com.mercedes.workflowrh.repository.DemandeRepository;
import com.mercedes.workflowrh.repository.EmployeRepository;
import com.mercedes.workflowrh.repository.HistoriqueDemandeRepository;
import com.mercedes.workflowrh.service.DemandeService;
import com.mercedes.workflowrh.service.MailService;
import com.mercedes.workflowrh.service.NotificationService;
import com.mercedes.workflowrh.entity.StatutDemande;
import com.mercedes.workflowrh.service.SoldeCongeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DemandeServiceImpl implements DemandeService {

    private final DemandeRepository demandeRepository;
    private final EmployeRepository employeRepository;
    private final HistoriqueDemandeRepository historiqueDemandeRepository;
    private final NotificationService notificationService;
    private final MailService mailService;

    @Override
    @Transactional
    public Demande createCongeStandard(
            TypeDemande typeDemande,
            LocalDate dateDebut, LocalTime heureDebut,
            LocalDate dateFin,   LocalTime heureFin) {

        assertType(typeDemande, CategorieDemande.CONGE_STANDARD);
        Employe employe = currentEmployeOr404();
        validateDates(dateDebut, dateFin);

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

        Demande saved = demandeRepository.save(d);
        historiserCreation(saved);
        notificationService.notifyManagerOfNewDemand(saved);

        return saved;
    }

    @Override
    public List<Demande> getAll() {
        return  demandeRepository.findAll();
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

//-----------------------------------------------------------------------------------------------------

    @Override
    @Transactional
    public Demande createCongeExceptionnel(
            TypeDemande typeDemande,
            LocalDate dateDebut, LocalTime heureDebut,
            LocalDate dateFin,   LocalTime heureFin) {

        assertType(typeDemande, CategorieDemande.CONGE_EXCEPTIONNEL);
        Employe employe = currentEmployeOr404();
        validateDates(dateDebut, dateFin);

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

        Demande saved = demandeRepository.save(d);
        historiserCreation(saved);
        notificationService.notifyManagerOfNewDemand(saved);

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

        Demande saved = demandeRepository.save(d);
        historiserCreation(saved);
        notificationService.notifyManagerOfNewDemand(saved);

        return saved;
    }

    @Override
    @Transactional
    public Demande createOrdreMission(
            LocalDate dateDebut, LocalTime heureDebut,
            LocalDate dateFin,   LocalTime heureFin,
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

        Demande saved = demandeRepository.save(d);
        historiserCreation(saved);
        notificationService.notifyManagerOfNewDemand(saved);

        return saved;
    }

    @Override
    public DemandeDetailDTO findDetail(Long id) {
        Demande d = demandeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande non trouvée"));
        return toDetail(d);
    }

    @Override
    public List<DemandeListDTO> findAllForChef(String matriculeChef) {
        // sécurité basique : s'assurer que l'appelant est bien un CHEF
        Employe chef = employeRepository.findByMatricule(matriculeChef)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        if (chef.getRole() != Role.CHEF) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé aux CHEFs.");
        }

        return demandeRepository.findAllForChefValidationAnyEmployee()
                .stream()
                .map(this::toListItem)
                .toList();
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
            case AUTORISATION -> { dDebut = d.getAutoDate(); dFin = d.getAutoDate(); }
            case ORDRE_MISSION -> { dDebut = d.getMissionDateDebut(); dFin = d.getMissionDateFin(); }
            case CONGE_STANDARD, CONGE_EXCEPTIONNEL -> {
                dDebut = d.getCongeDateDebut(); dFin = d.getCongeDateFin();
            }
        }

        return DemandeListDTO.builder()
                .id(d.getId())
                .employeMatricule(d.getEmploye()!=null ? d.getEmploye().getMatricule() : null)
                .employeNom(d.getEmploye()!=null ? d.getEmploye().getNom() : null)
                .employePrenom(d.getEmploye()!=null ? d.getEmploye().getPrenom() : null)
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
                .employeMatricule(d.getEmploye()!=null ? d.getEmploye().getMatricule() : null)
                .employeNom(d.getEmploye()!=null ? d.getEmploye().getNom() : null)
                .employePrenom(d.getEmploye()!=null ? d.getEmploye().getPrenom() : null)
                .employeEmail(d.getEmploye()!=null ? d.getEmploye().getEmail() : null)
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

        if (!estValidateurAutorise(d, validateur)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorisé.");
        }

        d.setStatut(StatutDemande.VALIDEE);
        d.setValidateur(validateur);
        d.setCommentaireRefus(null);
        d.setDateValidation(LocalDateTime.now());

        HistoriqueDemande h = HistoriqueDemande.creerHistoriqueValidation(d);
        historiqueDemandeRepository.save(h);

        Demande saved = demandeRepository.save(d);

        // ✅ Notif WebSocket + e-mail à l’employé créateur
        notificationService.notifyEmployeeOnValidation(saved);

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

        return saved;
    }

    private boolean estValidateurAutorise(Demande demande, Employe validateur) {
        Role roleCreat = demande.getEmploye() != null ? demande.getEmploye().getRole() : null;
        if (roleCreat == null) return false;

        boolean chefPeut = (validateur.getRole() == Role.CHEF) && (roleCreat == Role.EMPLOYE);
        boolean drhPeut  = (validateur.getRole() == Role.DRH)  && (roleCreat == Role.CHEF);

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

    // (facultatif) si tu as d’autres opérations :
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
    public List<Demande> getHistoriqueDemandes(String matriculeEmploye) {
        Employe employe = employeRepository.findByMatricule(matriculeEmploye)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employé non trouvé"));

        return demandeRepository.findByEmployeOrderByDateDemandeDesc(employe);
    }

    /**
     * Récupère toutes les demandes en attente pour un chef donné.
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


}
