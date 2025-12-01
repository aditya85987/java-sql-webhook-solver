package com.example.bfhl.service;

import com.example.bfhl.model.GenerateWebhookRequest;
import com.example.bfhl.model.GenerateWebhookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StartupService implements ApplicationRunner {
    private final WebhookClient webhookClient;
    private final PdfDownloader pdfDownloader;
    private final PdfTextExtractor pdfTextExtractor;
    private final Environment env;
    private final Logger logger = LoggerFactory.getLogger(StartupService.class);

    public StartupService(WebhookClient webhookClient, PdfDownloader pdfDownloader, PdfTextExtractor pdfTextExtractor, Environment env) {
        this.webhookClient = webhookClient;
        this.pdfDownloader = pdfDownloader;
        this.pdfTextExtractor = pdfTextExtractor;
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String generateUrl = env.getProperty("bfhl.generate-url");
        String testUrl = env.getProperty("bfhl.test-url");
        String name = env.getProperty("bfhl.name");
        String regNo = env.getProperty("bfhl.regNo");
        String email = env.getProperty("bfhl.email");
        String workDir = env.getProperty("app.work-dir", "./work");

        logger.info("StartupService running: generateUrl={}, testUrl={}", generateUrl, testUrl);

        GenerateWebhookRequest request = new GenerateWebhookRequest(name, regNo, email);
        GenerateWebhookResponse response = webhookClient.generateWebhook(generateUrl, request);

        if (response == null) {
            logger.error("No response from generateWebhook. Aborting.");
            return;
        }

        String webhook = response.getWebhook();
        String accessToken = response.getAccessToken();
        logger.info("Received webhook: {}, accessToken present: {}", webhook, accessToken != null);

        int lastTwo = extractLastTwoDigits(regNo);
        boolean isOdd = (lastTwo % 2) == 1;
        logger.info("regNo={} -> lastTwo={} -> isOdd={}", regNo, lastTwo, isOdd);

        String q1 = "https://drive.google.com/uc?export=download&id=1LAPx2to9zmN5DYN0tkMrJRnVXf1guNr";
        String q2 = "https://drive.google.com/uc?export=download&id=1b0p5C-6fUrUQgJVaWWAAB3P12IfoBCH";

        String targetUrl = isOdd ? q1 : q2;

        Path pdfPath = Path.of(workDir, "question.pdf");
        try {
            pdfDownloader.downloadToPath(targetUrl, pdfPath);
        } catch (Exception ex) {
            logger.error("Failed to download PDF: {}. You may need to transform the Google Drive 'view' link into a direct download link (use uc?export=download&id=FILE_ID)", ex.getMessage());
            Files.createDirectories(pdfPath.getParent());
            Files.writeString(Path.of(workDir, "download-error.txt"), ex.toString());
        }

        String extracted = "";
        if (Files.exists(pdfPath)) {
            try {
                extracted = pdfTextExtractor.extractText(pdfPath);
                Files.writeString(Path.of(workDir, "question-text.txt"), extracted);
            } catch (Exception ex) {
                logger.error("PDF extraction failed: {}", ex.getMessage());
            }
        } else {
            logger.warn("PDF not present at {}; skipping extraction", pdfPath);
        }

        String candidateSql = findSqlInText(extracted);
        if (candidateSql == null || candidateSql.isBlank()) {
            logger.warn("No SQL query auto-detected in PDF text. You'll need to provide finalQuery manually.");
            candidateSql = "/* NO_AUTOMATIC_SQL_DETECTED - please open work/question-text.txt and craft finalQuery manually */";
        } else {
            logger.info("Auto-detected candidate SQL ({} chars)", candidateSql.length());
        }

        if (webhook != null && !webhook.isBlank()) {
            webhookClient.postFinalQuery(webhook, accessToken, candidateSql);
        } else {
            logger.error("Webhook URL missing - cannot submit final query");
        }

        logger.info("StartupService completed.");
    }

    private int extractLastTwoDigits(String regNo) {
        if (regNo == null) return 0;
        String digits = regNo.replaceAll("\D+", "");
        if (digits.isEmpty()) return 0;
        if (digits.length() == 1) return Integer.parseInt(digits);
        String lastTwoStr = digits.substring(Math.max(0, digits.length() - 2));
        return Integer.parseInt(lastTwoStr);
    }

   private String findSqlInText(String text) {
    if (text == null || text.isBlank()) return null;
    // Note: in Java string literals backslashes must be escaped,
    // so \s becomes \\s and \S becomes \\S inside the quoted string.
    Pattern p = Pattern.compile("((?m)^(SELECT|WITH|INSERT|UPDATE|DELETE)[\\\\s\\\\S]{0,2000}?;?)", Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(text);
    StringBuilder sb = new StringBuilder();
    while (m.find()) {
        sb.append(m.group(1)).append(System.lineSeparator());
    }
    String candidate = sb.toString().trim();
    if (candidate.isEmpty()) return null;
    return candidate;
}

}
