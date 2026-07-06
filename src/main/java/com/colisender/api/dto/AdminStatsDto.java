package com.colisender.api.dto;

public class AdminStatsDto {
    private long totalUtilisateurs;
    private long utilisateursActifs;
    private long utilisateursSuspendus;
    private long utilisateursEnAttente;
    private long totalColis;
    private long colisEnCours;
    private long colisLivres;
    private long totalPaiements;
    private double montantTotal;
    private long verificationsEnAttente;
    private long litigesOuverts;
    private long litigesEnCours;

    public long getTotalUtilisateurs() { return totalUtilisateurs; }
    public void setTotalUtilisateurs(long v) { this.totalUtilisateurs = v; }
    public long getUtilisateursActifs() { return utilisateursActifs; }
    public void setUtilisateursActifs(long v) { this.utilisateursActifs = v; }
    public long getUtilisateursSuspendus() { return utilisateursSuspendus; }
    public void setUtilisateursSuspendus(long v) { this.utilisateursSuspendus = v; }
    public long getUtilisateursEnAttente() { return utilisateursEnAttente; }
    public void setUtilisateursEnAttente(long v) { this.utilisateursEnAttente = v; }
    public long getTotalColis() { return totalColis; }
    public void setTotalColis(long v) { this.totalColis = v; }
    public long getColisEnCours() { return colisEnCours; }
    public void setColisEnCours(long v) { this.colisEnCours = v; }
    public long getColisLivres() { return colisLivres; }
    public void setColisLivres(long v) { this.colisLivres = v; }
    public long getTotalPaiements() { return totalPaiements; }
    public void setTotalPaiements(long v) { this.totalPaiements = v; }
    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double v) { this.montantTotal = v; }
    public long getVerificationsEnAttente() { return verificationsEnAttente; }
    public void setVerificationsEnAttente(long v) { this.verificationsEnAttente = v; }
    public long getLitigesOuverts() { return litigesOuverts; }
    public void setLitigesOuverts(long v) { this.litigesOuverts = v; }
    public long getLitigesEnCours() { return litigesEnCours; }
    public void setLitigesEnCours(long v) { this.litigesEnCours = v; }
}
