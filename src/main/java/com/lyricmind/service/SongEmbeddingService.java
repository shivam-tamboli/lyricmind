package com.lyricmind.service;

import com.lyricmind.component.DatasetGeneratorComponent;
import com.lyricmind.model.Song;
import com.lyricmind.model.dto.BulkSongRequest;
import com.lyricmind.model.dto.BulkSongResponse;
import com.lyricmind.model.dto.SongRequest;
import com.lyricmind.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongEmbeddingService {

    private static final String RESOURCES_PATH = "src/main/resources/";

    private final SongRepository songRepository;
    private final VectorStore vectorStore;
    private final DatasetGeneratorComponent datasetGeneratorComponent;

    /**
     * INGESTION PIPELINE: Processes SongRequest list → MongoDB documents + vector embeddings
     * 1. Map SongRequest → Song entities
     * 2. Save Songs to MongoDB collection
     * 3. Create Spring AI Documents from Songs
     * 4. Embed Documents → MongoDB vector collection (HNSW index)
     * @return number of successfully embedded songs
     */
    @Transactional
    public Integer createEmbeddingFromSongList(List<SongRequest> requestList) {
        if (requestList == null || requestList.isEmpty()) {
            throw new IllegalArgumentException("Song request list cannot be null or empty");
        }

        log.info("Starting bulk embedding for {} songs", requestList.size());

        List<Song> savedSongs = new ArrayList<>();
        List<Document> documents = new ArrayList<>();

        try {
            // 1. MAP REQUESTS TO ENTITIES
            for (SongRequest request : requestList) {
                Song song = mapRequestToSong(request);
                savedSongs.add(song);
            }

            // 2. BULK SAVE SONGS to MongoDB (regular collection)
            savedSongs = songRepository.saveAll(savedSongs);

            // 3. CREATE DOCUMENTS for vector embedding
            documents = savedSongs.stream()
                    .map(this::createDocumentFromSong)
                    .collect(Collectors.toList());

            // 4. EMBED DOCUMENTS → MongoDB vector collection
            embedDocuments(documents);

            log.info("Successfully embedded {} songs", documents.size());
            return documents.size();

        } catch (Exception e) {
            log.error("Bulk embedding failed", e);
            throw new RuntimeException("Bulk embedding failed", e);
        }
    }

    /**
     * CSV INGESTION ENDPOINT: File → Songs → Embeddings
     * Orchestrates complete ingestion from CSV file path
     */
    @Transactional
    public BulkSongResponse createEmbeddingFromBulkSong(BulkSongRequest request) {
        if (request == null || request.fileName() == null || request.fileName().trim().isEmpty()) {
            throw new IllegalArgumentException("Bulk request and filename cannot be null or empty");
        }

        String filePath = Paths.get(RESOURCES_PATH, request.fileName()).toString();
        log.info("Processing bulk embedding from: {}", filePath);

        try {
            // CSV → SongRequest list (DatasetGeneratorComponent)
            List<SongRequest> songRequestList = datasetGeneratorComponent.generateSongRequestFromCSV(filePath);

            if (songRequestList.isEmpty()) {
                log.warn("No songs in file: {}", filePath);
                return new BulkSongResponse(0);
            }

            // SongRequest → Songs → Embeddings (main pipeline)
            Integer embeddedCount = createEmbeddingFromSongList(songRequestList);
            return new BulkSongResponse(embeddedCount);

        } catch (IOException e) {
            log.error("CSV file read failed: {}", filePath, e);
            throw new RuntimeException("CSV processing failed: " + request.fileName(), e);
        }
    }

    /**
     * DOCUMENT CREATION: Song entity → Spring AI Document (for vector embedding)
     * Content = title + artist + lyrics
     * Metadata = songId, title, artist, album, genre, year (for filtering)
     */
    private Document createDocumentFromSong(Song song) {
        if (song == null) {
            throw new IllegalArgumentException("Song cannot be null");
        }

        // Document content: searchable text (lyrics + metadata)
        StringBuilder content = new StringBuilder();
        content.append("Title: ").append(sanitizeText(song.getTitle())).append("\n");
        content.append("Artist: ").append(sanitizeText(song.getArtist())).append("\n");
        content.append("Lyrics: ").append(sanitizeText(song.getLyrics())).append("\n");

        // Document metadata: filters + context (retrieved with search results)
        Map<String, Object> metadata = createMetadata(song);

        return new Document(content.toString(), metadata);
    }

    private Map<String, Object> createMetadata(Song song) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("songId", song.getId());      // Link to full Song document
        metadata.put("title", song.getTitle());
        metadata.put("artist", song.getArtist());
        metadata.put("album", song.getAlbum());
        metadata.put("genre", song.getGenre());
        metadata.put("description", song.getDescription());
        metadata.put("releaseYear", song.getReleaseYear());
        return metadata;
    }

    /**
     * VECTOR STORE EMBEDDING: Saves Documents to MongoDB Atlas vector collection
     * OpenAI auto-embeds content → HNSW index for fast similarity search
     */
    private void embedDocuments(List<Document> documents) {
        try {
            vectorStore.add(documents);  // Spring AI → MongoDB vector collection
            log.debug("Embedded {} documents", documents.size());
        } catch (Exception e) {
            log.error("Vector embedding failed", e);
            throw new RuntimeException("Vector embedding failed", e);
        }
    }

    private Song mapRequestToSong(SongRequest request) {
        // DTO → Entity mapping with sanitization
        Song song = new Song();
        song.setTitle(sanitizeText(request.title()));
        song.setArtist(sanitizeText(request.artist()));
        song.setAlbum(sanitizeText(request.album()));
        song.setGenre(sanitizeText(request.genre()));
        song.setDescription(sanitizeText(request.description()));
        song.setLyrics(sanitizeText(request.lyrics()));
        song.setReleaseYear(request.releaseYear());
        return song;
    }

    private String sanitizeText(String text) {
        return text != null ? text.trim() : "";
    }


}
