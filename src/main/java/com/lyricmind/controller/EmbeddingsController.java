package com.lyricmind.controller;

import com.lyricmind.model.dto.BulkSongRequest;
import com.lyricmind.model.dto.BulkSongResponse;
import com.lyricmind.service.SongEmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmbeddingsController {

    @Autowired
    SongEmbeddingService songEmbeddingService;

    ResponseEntity<BulkSongResponse> createEmbeddingFromBulkSong(@RequestBody BulkSongRequest request){
        return new ResponseEntity<>(songEmbeddingService.createEmbeddingFromBulkSong(request), HttpStatus.CREATED);
    }
}
