package com.lyricmind.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;


@Slf4j
@Component
@RequiredArgsConstructor
public class RerankComponent {

    private static final String DEFAULT_MOTIVATION = "Relevant to the requested mood";
    private static final int MAX_RERANK_DOCUMENTS = 50;
    private static final String JSON_WRAPPER_REGEX = "(?s)```json\\s*";
    private static final String MARKDOWN_END_REGEX = "(?s)```";

    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;

    /**
     * Reranks candidates and selects the top {@code targetSize} documents for the given mood.
     *
     * <p>The prompt asks the model to SELECT the best {@code targetSize} songs from the
     * candidates rather than ranking every document. This makes the LLM output O(targetSize)
     * tokens instead of O(candidates.size()), cutting reranking latency roughly in half when
     * {@code targetSize < candidates.size()}.
     *
     * @param mood       natural-language mood query
     * @param docs       candidate documents from vector search
     * @param targetSize number of results to return (≤ docs.size())
     */
    public List<Document> rerank(String mood, List<Document> docs, int targetSize) {
        log.info("Re-ranking {} candidates for mood: '{}', selecting top {}", docs.size(), mood, targetSize);

        try {
            List<Document> candidates = limitDocuments(docs);
            int effectiveTarget = Math.min(targetSize, candidates.size());

            String prompt = buildRerankingPrompt(mood, candidates, effectiveTarget);
            ChatResponse response = executeRerankingQuery(prompt);

            List<Map<String, Object>> ranking = parseRerankingResponse(response);
            List<Document> rerankedDocs = applyRerankingResults(candidates, ranking);

            log.info("Re-ranking complete: {} documents selected for mood: '{}'", rerankedDocs.size(), mood);
            return rerankedDocs;

        } catch (Exception e) {
            log.error("Failed to re-rank documents for mood: '{}'", mood, e);
            throw new RuntimeException("Document re-ranking failed", e);
        }
    }

    private List<Document> limitDocuments(List<Document> docs) {
        if (docs.size() <= MAX_RERANK_DOCUMENTS) {
            return docs;
        }
        log.info("Limiting candidates from {} to {} for re-ranking", docs.size(), MAX_RERANK_DOCUMENTS);
        return docs.subList(0, MAX_RERANK_DOCUMENTS);
    }

    /**
     * Builds a compact prompt that asks the model to SELECT the top N songs rather than
     * rank every candidate. Shorter output = fewer tokens generated = lower latency.
     */
    private String buildRerankingPrompt(String mood, List<Document> docs, int targetSize) {
        StringBuilder songsText = new StringBuilder();

        IntStream.range(0, docs.size()).forEach(i -> {
            Document doc = docs.get(i);
            String artist = extractMetadata(doc, "artist");
            String title  = extractMetadata(doc, "title");
            String genre  = extractMetadata(doc, "genre");

            songsText.append("Doc ").append(i + 1).append(": ")
                     .append(artist).append(" — ").append(title);
            if (StringUtils.hasText(genre) && !"Unknown".equals(genre)) {
                songsText.append(" [").append(genre).append("]");
            }
            songsText.append("\n");
        });

        return String.format("""
                You are a music recommendation assistant.
                Mood: %s

                Songs:
                %s
                Select the %d best matches for this mood.
                Return ONLY a JSON array of exactly %d items, most relevant first.
                Format: [{"doc_index": N, "score": 0.0-1.0, "motivation": "reason under 80 chars"}]
                Only valid JSON — no markdown, no extra text.
                """,
                sanitizeInput(mood), songsText, targetSize, targetSize);
    }

    private ChatResponse executeRerankingQuery(String prompt) {
        try {
            Prompt aiPrompt = new Prompt(new UserMessage(prompt));
            ChatResponse response = chatModel.call(aiPrompt);

            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                throw new RuntimeException("Invalid response from AI model");
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to execute re-ranking query", e);
            throw new RuntimeException("AI model query failed", e);
        }
    }

    private List<Map<String, Object>> parseRerankingResponse(ChatResponse response) {
        try {
            String content = response.getResult().getOutput().getText();
            log.debug("Re-ranking raw response: {}", content);

            String cleanedJson = cleanJsonResponse(content);
            List<Map<String, Object>> ranking = objectMapper.readValue(
                    cleanedJson, new TypeReference<List<Map<String, Object>>>() {});

            validateRankingResponse(ranking);
            return ranking;

        } catch (IOException e) {
            log.error("Failed to parse re-ranking JSON response", e);
            throw new RuntimeException("Invalid JSON response from AI model", e);
        }
    }

    private String cleanJsonResponse(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            throw new RuntimeException("Empty response from AI model");
        }
        return rawResponse
                .replaceAll(JSON_WRAPPER_REGEX, "")
                .replaceAll(MARKDOWN_END_REGEX, "")
                .trim();
    }

    private void validateRankingResponse(List<Map<String, Object>> ranking) {
        if (ranking == null || ranking.isEmpty()) {
            throw new RuntimeException("Empty ranking response from AI model");
        }
        for (Map<String, Object> item : ranking) {
            if (!item.containsKey("doc_index") || !item.containsKey("motivation")) {
                throw new RuntimeException("Invalid ranking item structure: missing required fields");
            }
        }
    }

    private List<Document> applyRerankingResults(List<Document> originalDocs, List<Map<String, Object>> ranking) {
        List<Document> rerankedDocs = new ArrayList<>();

        for (Map<String, Object> item : ranking) {
            try {
                int index = extractDocumentIndex(item);
                String motivation = extractMotivation(item);

                if (isValidDocumentIndex(index, originalDocs.size())) {
                    Document doc = originalDocs.get(index);
                    addMotivationMetadata(doc, motivation);
                    rerankedDocs.add(doc);
                } else {
                    log.warn("Invalid doc_index {} (list size {})", index + 1, originalDocs.size());
                }
            } catch (Exception e) {
                log.warn("Failed to process ranking item: {}", item, e);
            }
        }
        return rerankedDocs;
    }

    private int extractDocumentIndex(Map<String, Object> rankingItem) {
        Object docIndexObj = rankingItem.get("doc_index");
        if (docIndexObj instanceof Number number) {
            return number.intValue() - 1;
        }
        throw new RuntimeException("Invalid doc_index type: " + docIndexObj.getClass());
    }

    private String extractMotivation(Map<String, Object> rankingItem) {
        Object motivationObj = rankingItem.get("motivation");
        return motivationObj != null ? motivationObj.toString().trim() : DEFAULT_MOTIVATION;
    }

    private boolean isValidDocumentIndex(int index, int listSize) {
        return index >= 0 && index < listSize;
    }

    private void addMotivationMetadata(Document document, String motivation) {
        document.getMetadata().put("motivation", motivation);
    }

    private String extractMetadata(Document document, String key) {
        Object value = document.getMetadata().get(key);
        return value != null ? value.toString().trim() : "Unknown";
    }

    private String sanitizeInput(String input) {
        return input.replaceAll("[\"'`]", "").trim();
    }
}