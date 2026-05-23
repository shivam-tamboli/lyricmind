import React, { useState } from "react";
import SongCard from "../components/SongCard";

const ResultPage = ({ songs, mood, onBack, onRefine }) => {
  const [searchInput, setSearchInput] = useState("");

  const handleRefine = (e) => {
    e.preventDefault();
    const query = searchInput.trim();
    if (query && onRefine) onRefine(query);
  };

  if (!songs || songs.length === 0) {
    return (
      <div className="results-container">
        <div className="empty-state">
          <div className="empty-icon">🎶</div>
          <h3 className="empty-title">No songs found</h3>
          <p className="empty-subtitle">
            Try rephrasing your mood — the more descriptive, the better the match.
          </p>
          <button className="back-btn" onClick={onBack} style={{ marginTop: 28 }}>
            ← Try Another Mood
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="results-container">
      <div className="results-header">
        <div className="results-header-left">
          <h2 className="results-title">Your Recommendations</h2>
          <p className="results-meta">
            <span className="results-count">{songs.length} song{songs.length !== 1 ? "s" : ""}</span>
            {" "}for{" "}
            <span className="mood-tag">{mood}</span>
          </p>
        </div>
        <button className="back-btn" onClick={onBack}>
          ← New Search
        </button>
      </div>

      {onRefine && (
        <form className="refine-bar" onSubmit={handleRefine}>
          <input
            className="refine-input"
            type="text"
            placeholder="Refine your mood (e.g. more upbeat, less sad)..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
          />
          <button type="submit" className="refine-btn" disabled={!searchInput.trim()}>
            Refine ↵
          </button>
        </form>
      )}

      <div className="songs-grid">
        {songs.map((song, index) => (
          <SongCard key={`${song.title}-${index}`} song={song} rank={index + 1} />
        ))}
      </div>
    </div>
  );
};

export default ResultPage;