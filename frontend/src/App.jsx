import React, { useState } from "react";
import MoodForm from "./components/MoodForm";
import ResultPage from "./pages/ResultPage";
import Loader from "./components/Loader";
import { getRecommendations } from "./services/api";
import "./App.css";

function App() {
  const [songs, setSongs] = useState([]);
  const [mood, setMood] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showResults, setShowResults] = useState(false);

  const handleSearch = async (moodText, limit) => {
    setLoading(true);
    setError(null);
    setMood(moodText);

    try {
      const data = await getRecommendations(moodText, limit);
      setSongs(data);
      setShowResults(true);
    } catch (err) {
      setError(err.message || "Something went wrong. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleBack = () => {
    setShowResults(false);
    setSongs([]);
    setError(null);
  };

  const handleRetry = () => {
    if (mood) {
      handleSearch(mood, 5);
    }
  };

  return (
    <>
      {/* Animated background */}
      <div className="app-bg">
        <div className="orb orb-1"></div>
        <div className="orb orb-2"></div>
        <div className="orb orb-3"></div>
      </div>

      <div className="app-container">
        {/* Header */}
        <header className="app-header">
          <div className="app-logo" onClick={handleBack}>
            <div className="app-logo-icon">🎵</div>
            <span className="app-logo-text">LyricMind</span>
          </div>
          <span className="app-logo-badge">AI Powered</span>
        </header>

        {/* Main Content */}
        {loading ? (
          <Loader />
        ) : error ? (
          <div className="error-container">
            <div className="error-icon">⚠️</div>
            <p className="error-message">Failed to get recommendations</p>
            <p className="error-hint">{error}</p>
            <button className="retry-btn" onClick={handleRetry}>
              Try Again
            </button>
          </div>
        ) : showResults ? (
          <ResultPage songs={songs} mood={mood} onBack={handleBack} />
        ) : (
          <MoodForm onSearch={handleSearch} />
        )}

        {/* Footer */}
        <footer className="app-footer">
          <div>Built with ❤️ using RAG Architecture</div>
          <div className="footer-tech">
            Spring Boot • MongoDB Atlas • OpenAI • React
          </div>
        </footer>
      </div>
    </>
  );
}

export default App;