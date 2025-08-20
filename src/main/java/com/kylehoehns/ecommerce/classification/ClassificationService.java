package com.kylehoehns.ecommerce.classification;

import org.springframework.stereotype.Service;


@Service
public class ClassificationService {

    // LLM will do this
    public ClassificationResult classifyRequest(String text) {
        if (text == null || text.isEmpty()) {
            return new ClassificationResult(Intent.UNKNOWN, Sentiment.NEUTRAL);
        }
        String lower = text.toLowerCase();
        Sentiment sentiment = Sentiment.NEUTRAL;
        if (lower.contains("broken") || lower.contains("defective") || lower.contains("snapped") || lower.contains("cracked")) {
            sentiment = Sentiment.NEGATIVE;
        }

        Intent intent = Intent.UNKNOWN;
        if (lower.contains("refund")) {
            intent = Intent.REFUND;
        } else if (lower.contains("replace") || lower.contains("replacement") || lower.contains("exchange")) {
            intent = Intent.REPLACEMENT;
        }

        return new ClassificationResult(intent, sentiment);
    }
}
