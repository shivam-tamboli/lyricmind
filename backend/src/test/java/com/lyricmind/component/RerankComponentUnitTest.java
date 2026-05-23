package com.lyricmind.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RerankComponentUnitTest {

    @org.mockito.Mock
    OpenAiChatModel chatModel;

    ObjectMapper objectMapper = new ObjectMapper();
    RerankComponent component;

    @Captor
    ArgumentCaptor<Prompt> promptCaptor;

    @BeforeEach
    void setUp() {
        component = new RerankComponent(chatModel, objectMapper);
    }

    private Document doc(String artist, String title, String genre) {
        Map<String, Object> md = new HashMap<>();
        md.put("artist", artist);
        md.put("title", title);
        if (genre != null) md.put("genre", genre);
        return new Document("lyrics...", md);
    }

    private void mockAiReturns(String rawText) {
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        Message message = new AssistantMessage(rawText);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn((AssistantMessage) message);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
    }

    @Test
    void rerank_happyPath_selectsTopN_ordersAndAddsMotivations() {
        List<Document> docs = List.of(
                doc("Artist A", "Title A", "Pop"),
                doc("Artist B", "Title B", "Rock")
        );

        // Model selects doc #2 first (most relevant), then doc #1
        String json = "[{\"doc_index\":2,\"score\":0.91,\"motivation\":\"Highly consistent with the mood\"}," +
                " {\"doc_index\":1,\"score\":0.35,\"motivation\":\"Partially consistent\"}]";
        mockAiReturns(json);

        List<Document> ranked = component.rerank("energetic", docs, 2);

        assertEquals(2, ranked.size());
        assertEquals("Title B", ranked.get(0).getMetadata().get("title"));
        assertEquals("Title A", ranked.get(1).getMetadata().get("title"));
        assertEquals("Highly consistent with the mood", ranked.get(0).getMetadata().get("motivation"));
        assertEquals("Partially consistent", ranked.get(1).getMetadata().get("motivation"));
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void rerank_selectsFewerThanTotal_returnsOnlyTargetSize() {
        List<Document> docs = List.of(
                doc("Artist A", "Title A", "Pop"),
                doc("Artist B", "Title B", "Rock"),
                doc("Artist C", "Title C", "Jazz")
        );

        // Model selects only 1 (targetSize=1) from 3 candidates
        String json = "[{\"doc_index\":2,\"score\":0.95,\"motivation\":\"Best match\"}]";
        mockAiReturns(json);

        List<Document> ranked = component.rerank("mellow", docs, 1);

        assertEquals(1, ranked.size());
        assertEquals("Title B", ranked.get(0).getMetadata().get("title"));
        assertEquals("Best match", ranked.get(0).getMetadata().get("motivation"));
    }

    @Test
    void rerank_cleansMarkdownCodeFence() {
        List<Document> docs = List.of(
                doc("Artist A", "Title A", null),
                doc("Artist B", "Title B", null)
        );

        String fenced = "```json\n" +
                "[{\"doc_index\":2,\"score\":0.9,\"motivation\":\"OK\"}," +
                " {\"doc_index\":1,\"score\":0.4,\"motivation\":\"OK\"}]\n" +
                "```";
        mockAiReturns(fenced);

        List<Document> ranked = component.rerank("calm", docs, 2);

        assertEquals(2, ranked.size());
        assertEquals("Title B", ranked.get(0).getMetadata().get("title"));
        assertEquals("OK", ranked.get(0).getMetadata().get("motivation"));
    }

    @Test
    void rerank_limitsTo50Documents_and_sanitizesMoodInPrompt() {
        List<Document> docs = IntStream.rangeClosed(1, 60)
                .mapToObj(i -> doc("Artist " + i, "Title " + i, (i % 2 == 0 ? "Pop" : "Rock")))
                .collect(Collectors.toList());

        String json = "[{\"doc_index\":50,\"score\":0.99,\"motivation\":\"Top\"}," +
                " {\"doc_index\":1,\"score\":0.5,\"motivation\":\"Base\"}]";
        mockAiReturns(json);

        String rawMood = "mo\"o'd `X";
        component.rerank(rawMood, docs, 2);

        verify(chatModel).call(promptCaptor.capture());
        Prompt usedPrompt = promptCaptor.getValue();
        Message msg = usedPrompt.getInstructions().get(0);
        assertTrue(msg instanceof UserMessage);
        String promptText = msg.getText();

        assertTrue(promptText.contains("Doc 50:"), "Prompt must contain 'Doc 50:'");
        assertFalse(promptText.contains("Doc 51:"), "Prompt must NOT contain 'Doc 51:'");
        assertFalse(promptText.contains("Doc 60:"), "Prompt must NOT contain 'Doc 60:'");

        // Mood should be sanitized
        assertTrue(promptText.contains("mood X"), "Sanitized mood should appear in prompt");
    }

    @Test
    void rerank_promptContainsTargetSize() {
        List<Document> docs = List.of(
                doc("Artist A", "Title A", "Pop"),
                doc("Artist B", "Title B", "Rock"),
                doc("Artist C", "Title C", "Jazz")
        );

        String json = "[{\"doc_index\":1,\"score\":0.9,\"motivation\":\"Top pick\"}]";
        mockAiReturns(json);

        component.rerank("happy", docs, 1);

        verify(chatModel).call(promptCaptor.capture());
        String promptText = promptCaptor.getValue().getInstructions().get(0).getText();

        // Prompt must tell the model how many to select
        assertTrue(promptText.contains("Select the 1 best"), "Prompt must specify targetSize=1");
    }

    @Test
    void rerank_invalidModelResponse_throws() {
        ChatResponse bad = mock(ChatResponse.class);
        when(bad.getResult()).thenReturn(null);
        when(chatModel.call(any(Prompt.class))).thenReturn(bad);

        List<Document> docs = List.of(doc("A", "T", null));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> component.rerank("any", docs, 1));
        assertTrue(ex.getMessage().contains("Document re-ranking failed"));
    }

    @Test
    void rerank_rankingItemMissingRequiredFields_throws() {
        String invalidJson = "[{\"doc_index\":1},{\"motivation\":\"missing index\"}]";
        mockAiReturns(invalidJson);

        List<Document> docs = List.of(doc("A", "T", null));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> component.rerank("any", docs, 1));
        assertTrue(ex.getMessage().contains("Document re-ranking failed"));
    }

    @Test
    void rerank_outOfRangeIndex_isSkipped() {
        String json = "[{\"doc_index\":999,\"score\":0.1,\"motivation\":\"out\"}]";
        mockAiReturns(json);

        List<Document> docs = List.of(doc("A", "T1", null), doc("B", "T2", null));

        List<Document> ranked = component.rerank("any", docs, 2);
        assertTrue(ranked.isEmpty());
    }

    @Test
    void rerank_nonNumericDocIndex_itemSkipped() {
        String json = "[{\"doc_index\":\"one\",\"motivation\":\"test\"}]";
        mockAiReturns(json);

        List<Document> docs = List.of(doc("A", "T", null));

        List<Document> ranked = component.rerank("any", docs, 1);
        assertTrue(ranked.isEmpty());
    }
}