import React from "react";

const Loader = () => {
  return (
    <div className="loader-overlay">
      <div className="loader-spinner"></div>
      <p className="loader-text">
        Finding your perfect songs<span className="loader-dots"></span>
      </p>
      <p className="loader-subtext">
        AI is analyzing mood and re-ranking results
      </p>
    </div>
  );
};

export default Loader;