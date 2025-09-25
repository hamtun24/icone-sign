package com.example.unifiedapi.dto.user;

public class UpdateCredentialsRequest {

    // TTN Credentials
    private String ttnUsername;
    private String ttnPassword;
    private String ttnMatriculeFiscal;

    // ANCE SEAL Credentials
    private String anceSealPin;
    private String anceSealAlias;
    private String certificatePath;

    // Constructors
    public UpdateCredentialsRequest() {}

    // Getters and Setters
    public String getTtnUsername() { return ttnUsername; }
    public void setTtnUsername(String ttnUsername) { this.ttnUsername = ttnUsername; }

    public String getTtnPassword() { return ttnPassword; }
    public void setTtnPassword(String ttnPassword) { this.ttnPassword = ttnPassword; }

    public String getTtnMatriculeFiscal() { return ttnMatriculeFiscal; }
    public void setTtnMatriculeFiscal(String ttnMatriculeFiscal) { this.ttnMatriculeFiscal = ttnMatriculeFiscal; }

    public String getAnceSealPin() { return anceSealPin; }
    public void setAnceSealPin(String anceSealPin) { this.anceSealPin = anceSealPin; }

    public String getAnceSealAlias() { return anceSealAlias; }
    public void setAnceSealAlias(String anceSealAlias) { this.anceSealAlias = anceSealAlias; }

    public String getCertificatePath() { return certificatePath; }
    public void setCertificatePath(String certificatePath) { this.certificatePath = certificatePath; }
}
