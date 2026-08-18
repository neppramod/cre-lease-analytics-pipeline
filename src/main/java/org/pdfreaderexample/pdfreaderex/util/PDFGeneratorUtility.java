package org.pdfreaderexample.pdfreaderex.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;

public class PDFGeneratorUtility {
    public static void generateMockLease(String filePath, String landlord, String tenant, String expiration) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                // PDFBox compliant font initialization style
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                contentStream.setLeading(14.5f);
                contentStream.newLineAtOffset(50, 750);

                // Emulate the horizontal text layout PDFBox reads across rows
                contentStream.showText("COMMERCIAL REAL ESTATE LEASE AGREEMENT");
                contentStream.newLine();
                contentStream.showText("Lease Parameter Agreement Details");
                contentStream.newLine();
                contentStream.showText("Landlord / Lessor " + landlord);
                contentStream.newLine();
                contentStream.showText("Tenant / Lessee " + tenant);
                contentStream.newLine();
                contentStream.showText("Lease Term Commencement Lease Term Expiration Initial Base Rent");
                contentStream.newLine();
                contentStream.showText("October 1, 2026 " + expiration + " $13,125.00");

                contentStream.endText();
            }

            File targetFile = new File(filePath);
            targetFile.getParentFile().mkdirs();
            document.save(targetFile);
            System.out.println("Generated: " + targetFile.getAbsolutePath());
        }
    }
}
