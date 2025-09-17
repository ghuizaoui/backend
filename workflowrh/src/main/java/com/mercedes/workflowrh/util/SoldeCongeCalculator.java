package com.mercedes.workflowrh.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class SoldeCongeCalculator {

    /**
     * Calcule le droit annuel de congé selon le grade (int) ou le type de contrat.
     */
    public static float calculerDroitAnnuel(int grade, String typeContrat, LocalDate dateEmbauche) {
        if ("CIVP".equalsIgnoreCase(typeContrat)) {
            return 12;
        }

        if (grade >= 1 && grade <= 8) {
            float droitBase = 18f;

            if ("Contractuel".equalsIgnoreCase(typeContrat)) {
                int anciennete = LocalDate.now().getYear() - dateEmbauche.getYear();
                if (anciennete >= 15) return droitBase + 3;
                if (anciennete >= 10) return droitBase + 2;
                if (anciennete >= 5) return droitBase + 1;
            }

            return droitBase;
        } else if (grade >= 9 && grade <= 16) {
            return 26f;
        }

        return 0;
    }

    /**
     * Calcule le droit de congé pour l’année en cours, en fonction du mois courant.
     */
    public static float calculerDroitN(float droitAnnuel) {
        int moisClotures = LocalDate.now().getMonthValue() - 1; // mois écoulés
        return (droitAnnuel / 12f) * moisClotures;
    }

    /**
     * Calcule le solde actuel.
     */
    public static float calculerSoldeActuel(
            float solde2012,
            float droitN,
            float congesAcquisN,
            float retardsN,
            float autorisationsN
    ) {
        return solde2012 + droitN - congesAcquisN - retardsN - autorisationsN;
    }

    /**
     * Convertit une durée d’autorisation (ex : 1h30) en équivalent jours de congé.
     */
    public static float convertirHeuresEnJours(float heures) {
        return heures / 8f; // Supposant une journée = 8h
    }

    /**
     * Calcule la durée entre deux dates (en heures).
     */
    public static float calculerDureeEnHeures(LocalDateTime debut, LocalDateTime fin) {
        Duration duration = Duration.between(debut, fin);
        return duration.toMinutes() / 60f;
    }
}
