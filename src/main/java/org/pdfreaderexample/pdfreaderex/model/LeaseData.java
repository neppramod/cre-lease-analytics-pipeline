package org.pdfreaderexample.pdfreaderex.model;

// Hold key fields, we are interested from LeaseData
public class LeaseData {
    private String landlord;
    private String tenant;
    private String expirationDate;

    public String getLandlord() {
        return landlord;
    }

    public void setLandlord(String landlord) {
        this.landlord = landlord;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }
}
