package com.lyricmind.controller;

import com.lyricmind.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lyricmind/v1/recommendations")
public class RecommendationController {

    @Autowired
    RecommendationService recommendationService;
}
