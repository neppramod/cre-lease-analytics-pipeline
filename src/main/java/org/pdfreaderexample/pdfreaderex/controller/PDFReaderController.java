package org.pdfreaderexample.pdfreaderex.controller;

import org.pdfreaderexample.pdfreaderex.model.LeaseData;
import org.pdfreaderexample.pdfreaderex.service.DeterministicLeaseParser;
import org.pdfreaderexample.pdfreaderex.service.PDFDocumentReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/pdfreader")
public class PDFReaderController {
    private final Logger log = LoggerFactory.getLogger(PDFReaderController.class);

    private final PDFDocumentReader reader;
    private final DeterministicLeaseParser leaseParser;

    public PDFReaderController(PDFDocumentReader reader, DeterministicLeaseParser leaseParser) {
        this.reader = reader;
        this.leaseParser = leaseParser;
    }

    @GetMapping("/parse")
    public LeaseData parse() {
        // Read raw document pieces and parse the fields
        List<Document> documents = reader.getDocsFromPdf();
        LeaseData structuredData = leaseParser.extractFields(documents);

        log.info("Document Text", structuredData);

        return structuredData;
    }
}

