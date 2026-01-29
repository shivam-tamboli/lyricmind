package com.lyricmind.controller;

import com.lyricmind.service.SongEmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmbeddingsController {

    @Autowired
    SongEmbeddingService songEmbeddingService;
}
