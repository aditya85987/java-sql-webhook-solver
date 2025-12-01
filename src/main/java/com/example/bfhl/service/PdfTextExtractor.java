package com.example.bfhl.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class PdfTextExtractor {
    private final Logger logger = LoggerFactory.getLogger(PdfTextExtractor.class);

    public String extractText(Path pdfPath) throws Exception {
        logger.info("Extracting text from PDF: {}", pdfPath.toAbsolutePath());
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            logger.info("Extracted {} characters", text.length());
            return text;
        }
    }
}
