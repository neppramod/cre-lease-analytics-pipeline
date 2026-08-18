package org.pdfreaderexample.pdfreaderex.service;

import org.pdfreaderexample.pdfreaderex.controller.PDFReaderController;
import org.pdfreaderexample.pdfreaderex.model.LeaseData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.rmi.dgc.Lease;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Extract key lease fields, from our scanned Documents
@Service
public class DeterministicLeaseParser {
    private final Logger log = LoggerFactory.getLogger(DeterministicLeaseParser.class);

    public LeaseData extractFields(List<Document> documents) {
        // Combine all pages into a unified string block
        String fullText = documents.stream()
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n"));

        log.info("Parsed Text", fullText);

        LeaseData data = new LeaseData();

        // Extract Landlord using Regex
        Matcher landlordMatcher = Pattern.compile("Landlord\\s*/\\s*Lessor\\s+([^\n]+)").matcher(fullText);
        if (landlordMatcher.find()) {
            data.setLandlord(landlordMatcher.group(1).trim());
        }

        // Extract Tenant
        Matcher tenantMatcher = Pattern.compile("Tenant\\s*/\\s*Lessee\\s+([^\n]+)").matcher(fullText);
        if (tenantMatcher.find()) {
            data.setTenant(tenantMatcher.group(1).trim());
        }

        // Match all date strings inside the text block
        Pattern datePattern = Pattern.compile("[A-Za-z]+\\s+\\d{1,2},\\s+\\d{4}");
        Matcher dateMatcher = datePattern.matcher(fullText);

        // Commencement Date is the 1st match found
        if (dateMatcher.find()) {
            // Expiration Date is the 2nd match found right after it
            if (dateMatcher.find()) {
                data.setExpirationDate(dateMatcher.group().trim());
            }
        }
        return data;
    }
}
