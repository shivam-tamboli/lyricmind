import React, { useState } from "react";

const MOOD_SUGGESTIONS = [
  { label: "💔 Heartbreak", value: "heartbreak and missing someone" },
  { label: "😢 Sad", value: "sad and emotional" },
  { label: "🎉 Party", value: "party and celebration" },
  { label: "😎 Successful", value: "successful, confident, on top of the world" },
  { label: "🌙 Late Night", value: "late night and lonely" },
  { label: "💪 Motivated", value: "motivated and grinding for success" },
  { label: "😔 Lonely", value: "lonely and feeling empty inside" },
  { label: "🌑 Dark", value: "dark and troubled thoughts" },
  { label: "🌊 Nostalgic", value: "nostalgic and reminiscing about the past" },
  { label: "⚡ Energetic", value: "energetic and hype" },
];

const MoodForm = ({ onSearch }) => {
  const [mood, setMood] = useState("");
  const [limit, setLimit] = useState(5);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!mood.trim()) return;
    onSearch(mood.trim(), limit);
  };

  const handleChipClick = (value) => {
    setMood(value);
  };

  return (
    <section className="hero-section">
      <h1 className="hero-title">
        Discover Music That
        <br />
        <span className="gradient-text">Matches Your Mood</span>
      </h1>
      <p className="hero-subtitle">
        Tell us how you feel, and our AI will find the perfect songs for you
        using semantic search and intelligent re-ranking.
      </p>

      <div className="mood-form-wrapper">
        <form className="mood-form" onSubmit={handleSubmit}>
          {/* Mood Input */}
          <div className="form-group">
            <label className="form-label">How are you feeling?</label>
            <div className="mood-input-wrapper">
              <span className="input-icon">🎭</span>
              <input
                className="mood-input"
                type="text"
                placeholder='e.g. "I feel nostalgic and want something soulful"'
                value={mood}
                onChange={(e) => setMood(e.target.value)}
                required
              />
            </div>
            <div className="mood-chips">
              {MOOD_SUGGESTIONS.map((m) => (
                <button
                  type="button"
                  key={m.value}
                  className={`mood-chip ${mood === m.value ? "active" : ""}`}
                  onClick={() => handleChipClick(m.value)}
                >
                  {m.label}
                </button>
              ))}
            </div>
          </div>

          {/* Limit Selector */}
          <div className="form-group">
            <label className="form-label">Number of songs</label>
            <div className="limit-selector">
              <button
                type="button"
                className="limit-btn"
                onClick={() => setLimit((l) => Math.max(1, l - 1))}
                disabled={limit <= 1}
              >
                −
              </button>
              <span className="limit-value">{limit}</span>
              <button
                type="button"
                className="limit-btn"
                onClick={() => setLimit((l) => Math.min(10, l + 1))}
                disabled={limit >= 10}
              >
                +
              </button>
              <span className="limit-range">1 – 10 songs</span>
            </div>
          </div>

          {/* Submit */}
          <button
            type="submit"
            className="submit-btn"
            disabled={!mood.trim()}
          >
            <span>🔍</span>
            <span>Find My Songs</span>
          </button>
        </form>
      </div>
    </section>
  );
};

export default MoodForm;