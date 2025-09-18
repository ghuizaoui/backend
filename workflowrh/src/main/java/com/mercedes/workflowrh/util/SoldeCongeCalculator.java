package com.mercedes.workflowrh.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class SoldeCongeCalculator {

    /**
     * Calcule le droit annuel de congé selon le grade ou le type de contrat.
     */
    public static float calculerDroitAnnuel(int grade, String typeContrat, LocalDate dateEmbauche) {
        if ("CIVP".equalsIgnoreCase(typeContrat)) {
            return 12f;
        }

        if (grade >= 1 && grade <= 8) {
            float droitBase = 18f;

            if ("Contractuel".equalsIgnoreCase(typeContrat)) {
                long anciennete = ChronoUnit.YEARS.between(dateEmbauche, LocalDate.now());
                if (anciennete >= 15) return droitBase + 3;
                if (anciennete >= 10) return droitBase + 2;
                if (anciennete >= 5)  return droitBase + 1;
            }

            return droitBase;
        } else if (grade >= 9 && grade <= 16) {
            return 26f;
        }

        return 0f;
    }

    /**
     * Calcule le droit de congé pour l’année en cours (année civile).
     */
    public static float calculerDroitN(float droitAnnuel) {
        if (droitAnnuel <= 0) return 0f;
        int moisClotures = LocalDate.now().getMonthValue() - 1; // mois déjà passés
        return (droitAnnuel / 12f) * moisClotures;
    }

    /**
     * Calcule le solde disponible actuel.
     */
    public static float calculerSoldeDisponible(
            float soldeN1,
            float droitN,
            float congesAcquisN,
            float retardsN,
            float autorisationsN
    ) {
        return soldeN1 + droitN - Math.max(congesAcquisN, 0) - Math.max(retardsN, 0) - Math.max(autorisationsN, 0);
    }

    /**
     * Convertit des heures en jours de congé.
     */
    public static float convertirHeuresEnJours(float heures) {
        return heures / 8f; // 1 jour = 8h
    }

    /**
     * Calcule la durée entre deux dates en heures.
     */
    public static float calculerDureeEnHeures(LocalDateTime debut, LocalDateTime fin) {
        Duration duration = Duration.between(debut, fin);
        return duration.toMinutes() / 60f;
    }
}
