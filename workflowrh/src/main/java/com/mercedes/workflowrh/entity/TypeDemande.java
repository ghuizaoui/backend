package com.mercedes.workflowrh.entity;

// TypeDemande.java (corrected typo in comment and added deductsFromSolde method)


public enum TypeDemande {

    // Catégorie CONGE_STANDARD
    CONGE_ANNUEL(CategorieDemande.CONGE_STANDARD),

    // Catégorie CONGE_EXCEPTIONNEL
    CONGE_MATERNITE(CategorieDemande.CONGE_EXCEPTIONNEL),
    CONGE_MALADIE(CategorieDemande.CONGE_EXCEPTIONNEL),
    CONGE_MARIAGE(CategorieDemande.CONGE_EXCEPTIONNEL),
    CONGE_NAISSANCE(CategorieDemande.CONGE_EXCEPTIONNEL),
    CONGE_DECES(CategorieDemande.CONGE_EXCEPTIONNEL),
    CONGE_CIRCONCISION(CategorieDemande.CONGE_EXCEPTIONNEL),
    CONGE_PELERINAGE(CategorieDemande.CONGE_EXCEPTIONNEL),
    CONGE_REPOS_COMPENSATEUR(CategorieDemande.CONGE_EXCEPTIONNEL),
    CONGE_SANS_SOLDE(CategorieDemande.CONGE_EXCEPTIONNEL),

    // Catégorie AUTORISATION
    AUTORISATION_SORTIE_PONCTUELLE(CategorieDemande.AUTORISATION),
    AUTORISATION_ABSENCE_EXCEPTIONNELLE(CategorieDemande.AUTORISATION),
    AUTORISATION_RETARD(CategorieDemande.AUTORISATION);

    private final CategorieDemande categorie;

    TypeDemande(CategorieDemande categorie) {
        this.categorie = categorie;
    }

    public CategorieDemande getCategorie() {
        return categorie;
    }

    public boolean deductsFromSolde() {
        switch (this) {
            case CONGE_ANNUEL:
            case CONGE_REPOS_COMPENSATEUR:
                return true;
            default:
                return false;
        }
    }
}