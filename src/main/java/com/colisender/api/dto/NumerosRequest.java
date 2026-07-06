package com.colisender.api.dto;

/**
 * Corps de PATCH /api/profil/{userId}/numeros
 * { "numeroPrincipal": "677...", "numeroSecondaire": "698..." }
 */
public class NumerosRequest {
    private String numeroPrincipal;
    private String numeroSecondaire;

    public String getNumeroPrincipal()  { return numeroPrincipal; }
    public String getNumeroSecondaire() { return numeroSecondaire; }
    public void setNumeroPrincipal(String v)  { this.numeroPrincipal = v; }
    public void setNumeroSecondaire(String v) { this.numeroSecondaire = v; }
}
