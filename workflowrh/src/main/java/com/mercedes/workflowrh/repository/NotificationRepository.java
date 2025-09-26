    // src/main/java/com/mercedes/workflowrh/repository/NotificationRepository.java
    package com.mercedes.workflowrh.repository;

    import com.mercedes.workflowrh.entity.Notification;
    import com.mercedes.workflowrh.entity.StatutNotification;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.jpa.repository.*;
    import org.springframework.data.repository.query.Param;
    import org.springframework.stereotype.Repository;

    import java.util.List;

    @Repository
    public interface NotificationRepository extends JpaRepository<Notification, Long> {

        long countByDestinataireMatriculeAndStatut(String matricule, StatutNotification statut);

        Page<Notification> findByDestinataireMatriculeOrderByDateCreationDesc(
                String matricule, Pageable pageable);

        Page<Notification> findByDestinataireMatriculeAndStatutOrderByDateCreationDesc(
                String matricule, StatutNotification statut, Pageable pageable);

        List<Notification> findByDestinataireMatriculeAndStatutOrderByDateCreationDesc(
                String matricule, StatutNotification statut);

        // DELETE ALL for current user (renvoie le nombre supprimé)
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
               update Notification n
                  set n.statut = com.mercedes.workflowrh.entity.StatutNotification.LU
                where n.destinataire.matricule = :matricule
                  and n.statut = com.mercedes.workflowrh.entity.StatutNotification.NON_LU
               """)
        int markAllReadForUser(@Param("matricule") String matricule);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
               delete from Notification n
               where n.destinataire.matricule = :matricule
               """)
        int deleteByDestinataireMatricule(@Param("matricule") String matricule);
    }
