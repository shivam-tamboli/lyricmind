package com.lyricmind.model;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
public class SongRecommendation {

    private Song song;
    private Map<String, Objects> metadata;
    private double similarityScore;
    private String reasonForRecommendation;

    public SongRecommendation(Song song, Map<String, Objects> metadata, double similarityScore, String reasonForRecommendation) {
        this.song = song;
        this.metadata = metadata;
        this.similarityScore = similarityScore;
        this.reasonForRecommendation = reasonForRecommendation;
    }

    private String generateReasonForRecommendation(){
        return metadata.get("motivation").toString();
    }
}
