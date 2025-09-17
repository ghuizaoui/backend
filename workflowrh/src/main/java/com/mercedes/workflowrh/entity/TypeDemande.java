// TypeDemande.java
package com.mercedes.workflowrh.entity;

public enum TypeDemande {

    // Catégorie CONGE_STANDARD
    CONGE_ANNUEL(CategorieDemande.CONGE_STANDARD),
    CONGE_REPOS_COMPENSATEUR(CategorieDemande.CONGE_STANDARD),
    CONGE_SANS_SOLDE(CategorieDemande.CONGE_STANDARD),

    // Catégorie CONGE_EXCEPTIONNEL
    CONGE_MATERNITE(CategorieDemande.CONGE_EXCEPTIONNEL),
    CONGE_PATERNITE(CategorieDemande.CONGE_EXCEPTIONNEL),
    CONGE_MARIAGE(CategorieDemande.CONGE_EXCEPTIONNEL),
    CONGE_NAISSANCE(CategorieDemande.CONGE_EXCEPTIONNEL),
    CONGE_DECES(CategorieDemande.CONGE_EXCEPTIONNEL),
    CONGE_CIRCONCISION(CategorieDemande.CONGE_EXCEPTIONNEL),
    CONGE_PELERINAGE(CategorieDemande.CONGE_EXCEPTIONNEL),

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
}
