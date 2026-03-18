package com.lyricmind.controller;

import com.lyricmind.model.dto.BulkSongRequest;
import com.lyricmind.model.dto.BulkSongResponse;
import com.lyricmind.service.SongEmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lyricmind/v1/embeddings")
public class EmbeddingsController {

    @Autowired
    SongEmbeddingService songEmbeddingService;

    @PostMapping("/bulk-songs")
    ResponseEntity<BulkSongResponse> createEmbeddingFromBulkSong(@RequestBody BulkSongRequest request){
        return new ResponseEntity<>(songEmbeddingService.createEmbeddingFromBulkSong(request), HttpStatus.CREATED);
    }
}
