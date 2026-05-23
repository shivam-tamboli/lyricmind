import React from "react";

const RANK_STYLES = {
  1: { bg: "rgba(255, 215, 0, 0.12)", border: "rgba(255, 215, 0, 0.35)", color: "#ffd700" },
  2: { bg: "rgba(192, 192, 192, 0.12)", border: "rgba(192, 192, 192, 0.35)", color: "#c0c0c0" },
  3: { bg: "rgba(205, 127, 50, 0.12)", border: "rgba(205, 127, 50, 0.35)", color: "#cd7f32" },
};

const SongCard = ({ song, rank }) => {
  const rankStyle = RANK_STYLES[rank] || {
    bg: "rgba(108, 92, 231, 0.12)",
    border: "rgba(108, 92, 231, 0.25)",
    color: "var(--accent-light)",
  };

  return (
    <div className="song-card" style={{ "--rank-delay": `${(rank - 1) * 0.07}s` }}>
      <div
        className="song-card-rank"
        style={{ background: rankStyle.bg, borderColor: rankStyle.border, color: rankStyle.color }}
      >
        #{rank}
      </div>

      <h3 className="song-card-title">{song.title || "Unknown Title"}</h3>
      <p className="song-card-artist">{song.artist || "Unknown Artist"}</p>

      <div className="song-card-tags">
        {song.album && (
          <span className="song-tag">
            <span className="tag-icon">💿</span>
            {song.album}
          </span>
        )}
        {song.genre && (
          <span className="song-tag">
            <span className="tag-icon">🎸</span>
            {song.genre}
          </span>
        )}
        {song.releaseYear && (
          <span className="song-tag">
            <span className="tag-icon">📅</span>
            {song.releaseYear}
          </span>
        )}
      </div>

      {song.motivation && (
        <div className="song-motivation">
          <span className="motivation-label">✦ Why this song</span>
          <p className="motivation-text">{song.motivation}</p>
        </div>
      )}
    </div>
  );
};

export default SongCard;