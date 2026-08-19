package org.pdfreaderexample.pdfreaderex.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.pdfreaderexample.pdfreaderex.model.LeaseData;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Parses the sample PDFs through the same reader the running service uses.
//
// Difference with DeterministicLeaseParserTest.  DeterministicLeaseParserTest uses a mocked string parsed as PDF instead of using sample PDFs
class RealLeasePdfParsingTest {

    private final PDFDocumentReader reader = new PDFDocumentReader();
    private final DeterministicLeaseParser parser = new DeterministicLeaseParser();

    @Test
    @DisplayName("Extracts single-spaced values from the real third-party lease")
    void shouldExtractCleanFieldsFromRealLease() {
        assertLease("sample1",
                "Apex Commercial Holdings LLC",
                "Nexus Software Solutions Inc.",
                "September 30, 2031");
    }

    @Test
    @DisplayName("Extracts single-spaced values from the generated industrial lease")
    void shouldExtractCleanFieldsFromGeneratedIndustrialLease() {
        assertLease("sample2",
                "Titan Industrial Parks LLC",
                "Vanguard Logistics Corp",
                "December 31, 2032");
    }

    @Test
    @DisplayName("Extracts single-spaced values from the generated retail lease")
    void shouldExtractCleanFieldsFromGeneratedRetailLease() {
        assertLease("sample3",
                "Plaza Retail Partners LP",
                "Blue Bottle Coffee Ventures LLC",
                "May 31, 2035");
    }

    private void assertLease(String fileName, String landlord, String tenant, String expirationDate) {
        LeaseData result = parser.extractFields(
                reader.getDocsFromPdf("sampledocumentsfolder/" + fileName + ".pdf"));

        assertAll(fileName + " must yield values a system of record can match exactly",
                () -> assertEquals(landlord, result.getLandlord(), "Landlord"),
                () -> assertEquals(tenant, result.getTenant(), "Tenant"),
                () -> assertEquals(expirationDate, result.getExpirationDate(), "Expiration date"));
    }
}
