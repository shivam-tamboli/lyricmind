import React from "react";
import SongCard from "../components/SongCard";

const ResultPage = ({ songs, mood, onBack }) => {
  if (!songs || songs.length === 0) {
    return (
      <div className="results-container">
        <div className="empty-state">
          <div className="empty-icon">🎶</div>
          <h3 className="empty-title">No songs found</h3>
          <p className="empty-subtitle">
            Try a different mood or description to discover music.
          </p>
          <button className="back-btn" onClick={onBack} style={{ marginTop: 24, display: "inline-flex" }}>
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
            {songs.length} song{songs.length !== 1 ? "s" : ""} for mood
            <span className="mood-tag">{mood}</span>
          </p>
        </div>
        <button className="back-btn" onClick={onBack}>
          ← New Search
        </button>
      </div>

      <div className="songs-grid">
        {songs.map((song, index) => (
          <SongCard key={index} song={song} rank={index + 1} />
        ))}
      </div>
    </div>
  );
};

export default ResultPage;