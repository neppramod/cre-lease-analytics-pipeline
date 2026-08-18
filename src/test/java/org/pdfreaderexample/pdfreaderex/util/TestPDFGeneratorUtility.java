package org.pdfreaderexample.pdfreaderex.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TestPDFGeneratorUtility {
    private static final String baseDir = "src/main/resources/sampledocumentsfolder/";

    @Test
    public void generateDocuments() throws IOException {
        // Generate Sample 2 (Industrial Warehouse)
        PDFGeneratorUtility.generateMockLease(
                baseDir + "sample2.pdf",
                "Titan Industrial Parks LLC",
                "Vanguard Logistics Corp",
                "December 31, 2032"
        );

        // Generate Sample 3 (Retail Space)
        PDFGeneratorUtility.generateMockLease(
                baseDir + "sample3.pdf",
                "Plaza Retail Partners LP",
                "Blue Bottle Coffee Ventures LLC",
                "May 31, 2035"
        );
    }
}
