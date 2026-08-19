package org.pdfreaderexample.pdfreaderex.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.pdfreaderexample.pdfreaderex.model.LeaseData;
import org.springframework.ai.document.Document;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DeterministicLeaseParserTest {
    private final DeterministicLeaseParser parser = new DeterministicLeaseParser();

    @Test
    @DisplayName("Should accurately parse structured parameters from horizontal PDF raw text stream")
    void shouldExtractLeaseFieldsFromRawText() {
        // Arrange: Simulate the exact messy horizontal string layout extracted by PDFBox
        String simulatedPdfText = """
                COMMERCIAL REAL ESTATE LEASE AGREEMENT \n\
                Lease Parameter Agreement Details \n\
                Landlord / Lessor Apex Commercial Holdings LLC \n\
                Tenant / Lessee Nexus Software Solutions Inc. \n\
                Lease Term Commencement Lease Term Expiration Initial Base Rent \n\
                October 1, 2026 September 30, 2031 $13,125.00\
                """;

        Document mockDocument = new Document(simulatedPdfText);
        List<Document> documentList = Collections.singletonList(mockDocument);

        // Act
        LeaseData result = parser.extractFields(documentList);

        // Assert: Group assertions
        assertAll("Lease Parameters Extraction Validation",
                () -> assertNotNull(result, "Parsed payload should not be null"),
                () -> assertEquals("Apex Commercial Holdings LLC", result.getLandlord(), "Failed to extract Landlord"),
                () -> assertEquals("Nexus Software Solutions Inc.", result.getTenant(), "Failed to extract Tenant"),
                () -> assertEquals("September 30, 2031", result.getExpirationDate(), "Failed to isolate sequential Expiration Date")
        );
    }
}
