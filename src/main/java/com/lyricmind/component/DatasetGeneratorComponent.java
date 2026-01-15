package com.lyricmind.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component //This make it a spring component.
public class DatasetGeneratorComponent {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Logger logger = LoggerFactory.getLogger(DatasetGeneratorComponent.class);

}
