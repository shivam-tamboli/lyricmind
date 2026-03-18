package com.lyricmind.model;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class SongRecommendation {

    private Song song;
    private Map<String, Object> metadata;
    private Double similarityScore;
    private String reasonForRecommendation;

    public SongRecommendation(Song song, Map<String, Object> metadata, Double similarityScore) {
        this.song = song;
        this.metadata = metadata;
        this.similarityScore = similarityScore;
        this.reasonForRecommendation = generateReasonForRecommendation();
    }

    public SongRecommendation(Song song, Map<String, Object> metadata, Double similarityScore, String reasonForRecommendation) {
        this.song = song;
        this.metadata = metadata;
        this.similarityScore = similarityScore;
        this.reasonForRecommendation = reasonForRecommendation;
    }

    private String generateReasonForRecommendation() {
        if (metadata != null && metadata.get("motivation") != null) {
            return metadata.get("motivation").toString();
        }
        return null;
    }
}
