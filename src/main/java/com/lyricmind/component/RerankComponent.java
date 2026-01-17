package com.lyricmind.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;


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

}
