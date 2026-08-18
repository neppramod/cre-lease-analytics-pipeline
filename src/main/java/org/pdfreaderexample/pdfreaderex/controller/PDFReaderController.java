package org.pdfreaderexample.pdfreaderex.controller;

import org.pdfreaderexample.pdfreaderex.model.LeaseData;
import org.pdfreaderexample.pdfreaderex.service.DeterministicLeaseParser;
import org.pdfreaderexample.pdfreaderex.service.PDFDocumentReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/pdfreader")
public class PDFReaderController {
    private final Logger log = LoggerFactory.getLogger(PDFReaderController.class);

    private final PDFDocumentReader reader;
    private final DeterministicLeaseParser leaseParser;

    @Value("${cre.lease.folder-path}")
    private String leaseFolder;

    public PDFReaderController(PDFDocumentReader reader, DeterministicLeaseParser leaseParser) {
        this.reader = reader;
        this.leaseParser = leaseParser;
    }

    @GetMapping("/parse")
    public LeaseData parse(@RequestParam(value = "file", defaultValue = "sample1") String fileName) {
        // 1. Pass the fileName argument down cleanly into your resource allocator logic
        List<Document> documents = reader.getDocsFromPdf(leaseFolder + "/" + fileName + ".pdf");

        // 2. Process via the deterministic extraction engine
        return leaseParser.extractFields(documents);
    }
}

