package com.lyricmind;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.ai.openai.api-key=test-key",
        "spring.data.mongodb.uri=mongodb://localhost:27017/test"
})
class LyricmindApplicationTests {

    @Test
    void contextLoads() {
    }
}