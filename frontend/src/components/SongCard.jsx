import React from "react";

const SongCard = ({ song, rank }) => {
  return (
    <div className="song-card">
      <div className="song-card-rank">#{rank}</div>

      <h3 className="song-card-title">🎵 {song.title || "Unknown Title"}</h3>
      <p className="song-card-artist">{song.artist || "Unknown Artist"}</p>

      <div className="song-card-details">
        {song.album && (
          <span className="song-detail-tag">
            <span className="tag-icon">💿</span> {song.album}
          </span>
        )}
        {song.genre && (
          <span className="song-detail-tag">
            <span className="tag-icon">🎸</span> {song.genre}
          </span>
        )}
        {song.releaseYear && (
          <span className="song-detail-tag">
            <span className="tag-icon">📅</span> {song.releaseYear}
          </span>
        )}
      </div>

      {song.motivation && (
        <div className="song-card-motivation">
          <div className="motivation-label">✨ Why this song</div>
          {song.motivation}
        </div>
      )}
    </div>
  );
};

export default SongCard;