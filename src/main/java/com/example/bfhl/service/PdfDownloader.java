package com.example.bfhl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PdfDownloader {
    private final Logger logger = LoggerFactory.getLogger(PdfDownloader.class);

    public Path downloadToPath(String urlString, Path outputFile) throws Exception {
        logger.info("Downloading PDF from: {}", urlString);
        try (InputStream in = new URL(urlString).openStream()) {
            Files.createDirectories(outputFile.getParent());
            try (FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
                StreamUtils.copy(in, fos);
            }
        }
        logger.info("Saved to: {}", outputFile.toAbsolutePath());
        return outputFile;
    }
}
