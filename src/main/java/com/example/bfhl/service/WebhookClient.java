package com.example.bfhl.service;

import com.example.bfhl.model.GenerateWebhookRequest;
import com.example.bfhl.model.GenerateWebhookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class WebhookClient {
    private final RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(WebhookClient.class);

    public WebhookClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public GenerateWebhookResponse generateWebhook(String url, GenerateWebhookRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GenerateWebhookRequest> entity = new HttpEntity<>(request, headers);
        logger.info("Calling generateWebhook: {}", url);
        ResponseEntity<GenerateWebhookResponse> resp = restTemplate.postForEntity(url, entity, GenerateWebhookResponse.class);
        if (resp.getStatusCode() != HttpStatus.OK && resp.getStatusCode() != HttpStatus.CREATED) {
            logger.warn("generateWebhook returned non-200: {}", resp.getStatusCode());
        }
        return resp.getBody();
    }

    public void postFinalQuery(String webhookUrl, String accessToken, String finalQuery) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);
        var body = Map.of("finalQuery", finalQuery);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        logger.info("Posting final query to webhook: {}", webhookUrl);
        ResponseEntity<String> resp = restTemplate.postForEntity(webhookUrl, entity, String.class);
        logger.info("Webhook response: status={}, body={}", resp.getStatusCodeValue(), resp.getBody());
    }
}
