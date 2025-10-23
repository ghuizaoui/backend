package com.mercedes.workflowrh.repository;

import com.mercedes.workflowrh.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DemandeRepository extends JpaRepository<Demande, Long> {
    List<Demande> findByEmployeMatricule(String matricule);
    List<Demande> findTop3ByEmployeMatriculeOrderByDateCreationDesc(String matricule);

    /**
     * Trouve toutes les demandes pour un employé donné, triées par date de demande décroissante.
     * @param employe L'employé dont on veut récupérer les demandes.
     * @return Une liste de demandes.
     */
    List<Demande> findByEmployeOrderByDateCreationDesc(Employe employe);

    /**
     * Trouve toutes les demandes ayant un statut donné.
     * @param statut Le statut de la demande.
     * @return Une liste de demandes.
     */
    List<Demande> findByStatut(StatutDemande statut);

    @Query("""
            select d from Demande d
            where d.employe.role = com.mercedes.workflowrh.entity.Role.EMPLOYE
        """)
    List<Demande> findAllForChefValidationAnyEmployee();

    @Query("""
            select d from Demande d
            where d.employe.role = com.mercedes.workflowrh.entity.Role.CHEF
        """)
    List<Demande> findAllForDrhValidation();

    // ******************************** dashboard methods
    // ---- Compteurs globaux ----
    long countByStatut(StatutDemande statut);
    long countByCategorie(CategorieDemande categorie);

    // ---- Filtrer par période ----
    long countByDateCreationBetween(LocalDateTime start, LocalDateTime end);

    long countByStatutAndDateCreationBetween(StatutDemande statut, LocalDateTime start, LocalDateTime end);

    // ---- DRH KPIs : Nombre de demandes par employé ----
    @Query("SELECT d.employe.matricule, COUNT(d) FROM Demande d GROUP BY d.employe.matricule")
    List<Object[]> countDemandesByEmploye();

    // ---- DRH KPIs : Nombre de demandes par service ----
    @Query("SELECT d.employe.service, COUNT(d) FROM Demande d GROUP BY d.employe.service")
    List<Object[]> countDemandesByService();

    // ---- DRH KPIs : Nombre de demandes par catégorie ----
    @Query("SELECT d.categorie, COUNT(d) FROM Demande d GROUP BY d.categorie")
    List<Object[]> countDemandesByCategorie();

    // ---- DRH KPIs : Délai moyen de validation ----
    @Query(value = "SELECT AVG(TIMESTAMPDIFF(SECOND, d.date_creation, d.date_validation)) " +
            "FROM demandes d WHERE d.date_validation IS NOT NULL", nativeQuery = true)
    Double averageValidationTimeSeconds();

    // ---- DRH KPIs : Nombre de refus / validations ----
    @Query("SELECT d.statut, COUNT(d) FROM Demande d GROUP BY d.statut")
    List<Object[]> countByStatutGrouped();

    // Optional: Count demandes per month for charting over time
    @Query("SELECT FUNCTION('MONTH', d.dateCreation), COUNT(d) FROM Demande d " +
            "WHERE d.dateCreation BETWEEN :start AND :end " +
            "GROUP BY FUNCTION('MONTH', d.dateCreation) " +
            "ORDER BY FUNCTION('MONTH', d.dateCreation)")
    List<Object[]> countDemandesPerMonth(LocalDateTime start, LocalDateTime end);

    // 1. Find demandes by employee and status
    List<Demande> findByEmployeAndStatut(Employe employe, StatutDemande statut);

    // 2. Find demandes by type
    List<Demande> findByTypeDemande(TypeDemande typeDemande);

    List<Demande> findByCategorie(CategorieDemande categorieDemande);

    // 3. Find demandes in a date creation range
    List<Demande> findByDateCreationBetween(LocalDateTime start, LocalDateTime end);

    // 4. Count demandes by employee in a date range
    long countByEmployeAndDateCreationBetween(Employe employe, LocalDateTime start, LocalDateTime end);

    // -------------------- Charting / Time-series --------------------

    @Query("SELECT FUNCTION('YEAR', d.dateCreation), FUNCTION('MONTH', d.dateCreation), COUNT(d) FROM Demande d " +
            "GROUP BY FUNCTION('YEAR', d.dateCreation), FUNCTION('MONTH', d.dateCreation) " +
            "ORDER BY FUNCTION('YEAR', d.dateCreation), FUNCTION('MONTH', d.dateCreation)")
    List<Object[]> countDemandesPerMonthAndYear();

    @Query("SELECT d.categorie, FUNCTION('MONTH', d.dateCreation), COUNT(d) FROM Demande d " +
            "WHERE d.dateCreation BETWEEN :start AND :end " +
            "GROUP BY d.categorie, FUNCTION('MONTH', d.dateCreation) " +
            "ORDER BY d.categorie, FUNCTION('MONTH', d.dateCreation)")
    List<Object[]> countDemandesByCategoriePerMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT d.typeDemande, COUNT(d) FROM Demande d GROUP BY d.typeDemande")
    List<Object[]> countDemandesByType();

    // update the repository to can see the dashboard like
    // Add these methods to your DemandeRepository

    @Query("SELECT d.employe.service, COUNT(d) FROM Demande d " +
            "WHERE d.statut = 'VALIDEE' " +
            "AND d.dateCreation BETWEEN :start AND :end " +
            "GROUP BY d.employe.service")
    List<Object[]> countAcceptedDemandesByServiceAndDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    List<Demande> findByStatutAndCategorieInAndDateCreationBetween(
            StatutDemande statut,
            List<CategorieDemande> categories,
            LocalDateTime start,
            LocalDateTime end);

    List<Demande> findByEmployeAndStatutAndCategorieInAndDateCreationBetween(
            Employe employe,
            StatutDemande statut,
            List<CategorieDemande> categories,
            LocalDateTime start,
            LocalDateTime end);

    // In your DemandeRepository.java, add these methods:

    // Get status distribution with date filtering
    @Query("SELECT d.statut, COUNT(d) FROM Demande d " +
            "WHERE d.dateCreation BETWEEN :start AND :end " +
            "GROUP BY d.statut")
    List<Object[]> countByStatutGroupedWithDateRange(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);

    // Get total count for percentage calculation
    @Query("SELECT COUNT(d) FROM Demande d WHERE d.dateCreation BETWEEN :start AND :end")
    Long countTotalByDateRange(@Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    //******************************* dashboard employe
    // In DemandeRepository
    List<Demande> findByCategorieAndAutoDate(CategorieDemande categorie, LocalDate date);
    List<Demande> findByEmployeAndDateCreationBetween(Employe employe, LocalDateTime start, LocalDateTime end);

    // #############################################"" dashboard  chef
    // Dans DemandeRepository.java
    long countByEmployeMatriculeInAndDateCreationBetween(List<String> matricules, LocalDateTime start, LocalDateTime end);
    long countByEmployeMatriculeInAndCategorieInAndDateCreationBetween(List<String> matricules, List<CategorieDemande> categories, LocalDateTime start, LocalDateTime end);
    long countByEmployeMatriculeInAndCategorieAndDateCreationBetween(List<String> matricules, CategorieDemande categorie, LocalDateTime start, LocalDateTime end);
    long countByEmployeMatriculeInAndStatutAndDateCreationBetween(List<String> matricules, StatutDemande statut, LocalDateTime start, LocalDateTime end);
    List<Demande> findByEmployeMatriculeInAndStatutAndCategorieInAndDateCreationBetween(List<String> matricules, StatutDemande statut, List<CategorieDemande> categories, LocalDateTime start, LocalDateTime end);
    List<Demande> findByEmployeMatriculeInAndDateCreationBetween(List<String> matricules, LocalDateTime start, LocalDateTime end);

    @Query("SELECT e.service, e.direction, COUNT(d) FROM Demande d " +
            "JOIN d.employe e " +
            "WHERE e.direction = :direction " +
            "AND d.statut = 'VALIDEE' " +
            "AND d.dateCreation BETWEEN :start AND :end " +
            "GROUP BY e.service, e.direction")
    List<Object[]> countAcceptedDemandesByServiceAndDirectionAndDateRange(
            @Param("direction") String direction,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT d FROM Demande d " +
            "JOIN d.employe e " +
            "WHERE e.service = :service " +
            "AND d.statut IN :statuts "
    )
    List<Demande> findDemandesByServiceAndStatuts(
            @Param("service") String service,
            @Param("statuts") List<StatutDemande> statuts);


    List<Demande>   findByEmployeChefHierarchique2Matricule(String m);

    @Query("SELECT d FROM Demande d WHERE d.employe.service = :service AND d.employe.chefHierarchique2Matricule = :chefLevel2Matricules ")
    List<Demande> findByEmployeServiceAndChefHierarchique2MatriculeIn(
            @Param("service") String service,
            @Param("chefLevel2Matricules") String chefLevel2Matricules

    );




    @Query("""
       SELECT d FROM Demande d
       JOIN d.employe e
       WHERE e.service = :service
       """)
    List<Demande> findDemandesByService(@Param("service") String service);



    @Query("""
    SELECT d FROM Demande d
    JOIN d.employe e
    WHERE e.service = :service
    AND e.chefHierarchique2Matricule = :chef2Matricule
""")
    List<Demande> findDemandesByServiceAndChef2Matricule(
            @Param("service") String service,
            @Param("chef2Matricule") String chef2Matricule
    );



    @Query("""
    SELECT d FROM Demande d
    JOIN d.employe e
    WHERE e.drhSuper = true
""")
    List<Demande> getDemandeByEmployeDrhSuper();




    // Dans DemandeRepository.java
    @Query("SELECT d.categorie, d.typeDemande, COUNT(d) FROM Demande d " +
            "WHERE d.dateCreation BETWEEN :start AND :end " +
            "GROUP BY d.categorie, d.typeDemande " +
            "ORDER BY d.categorie, COUNT(d) DESC")
    List<Object[]> countDemandesByCategorieAndTypeWithDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT d.categorie, d.typeDemande, COUNT(d) FROM Demande d " +
            "GROUP BY d.categorie, d.typeDemande " +
            "ORDER BY d.categorie, COUNT(d) DESC")
    List<Object[]> countDemandesByCategorieAndType();



    @Query("""
    SELECT d 
    FROM Demande d 
    WHERE d.statut = com.mercedes.workflowrh.entity.StatutDemande.VALIDEE
    AND FUNCTION('DATE', d.dateCreation) = CURRENT_DATE
""")
    List<Demande> findValidatedDemandesToday();


    List<Demande> findByStatutAndEstLiberer(StatutDemande statut, Boolean estLiberer);

    List<Demande> findByEstLiberer(Boolean estLiberer);

    // Find demands ready for liberation (validated and not liberated)
    @Query("SELECT d FROM Demande d WHERE d.statut = 'VALIDEE' AND d.estLiberer = false")
    List<Demande> findDemandesPourLiberation();

    // Find today's validated demands for liberation
    @Query("SELECT d FROM Demande d WHERE d.statut = 'VALIDEE' AND d.estLiberer = false AND FUNCTION('DATE', d.autoDate) = CURRENT_DATE")
    List<Demande> findTodayAutorisationsPourLiberation();


    List<Demande> findTop5ByEmployeMatriculeOrderByDateCreationDesc(String matricule);

    // Find all autorisations for a specific date (any status)
    @Query("SELECT d FROM Demande d WHERE d.categorie = com.mercedes.workflowrh.entity.CategorieDemande.AUTORISATION " +
            "AND d.autoDate = :date")
    List<Demande> findAutorisationsForDate(@Param("date") LocalDate date);

    // Find only VALIDATED autorisations for a specific date
    @Query("SELECT d FROM Demande d WHERE d.categorie = com.mercedes.workflowrh.entity.CategorieDemande.AUTORISATION " +
            "AND d.autoDate = :date AND d.statut = com.mercedes.workflowrh.entity.StatutDemande.VALIDEE")
    List<Demande> findValidatedAutorisationsForDate(@Param("date") LocalDate date);

    // Count demands by category for an employee
    @Query("SELECT d.categorie, COUNT(d) FROM Demande d WHERE d.employe.matricule = :matricule GROUP BY d.categorie")
    List<Object[]> countByCategoryForEmployee(@Param("matricule") String matricule);

    // Count demands by status for an employee
    @Query("SELECT d.statut, COUNT(d) FROM Demande d WHERE d.employe.matricule = :matricule GROUP BY d.statut")
    List<Object[]> countByStatusForEmployee(@Param("matricule") String matricule);


    @Query("SELECT d FROM Demande d WHERE d.employe.matricule IN :matricules " +
            "AND d.statut = :statut " +
            "AND d.categorie IN :categories " +
            "AND ((d.congeDateDebut BETWEEN :dateDebut AND :dateFin) OR " +
            "(d.congeDateFin BETWEEN :dateDebut AND :dateFin) OR " +
            "(d.congeDateDebut <= :dateDebut AND d.congeDateFin >= :dateFin))")
    List<Demande> findCongesValidesByPeriode(
            @Param("matricules") List<String> matricules,
            @Param("statut") StatutDemande statut,
            @Param("categories") List<CategorieDemande> categories,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);


    // Add this method to DemandeRepository
    List<Demande> findByEmployeMatriculeInAndStatutAndCategorieIn(
            List<String> matricules,
            StatutDemande statut,
            List<CategorieDemande> categories
    );
}


