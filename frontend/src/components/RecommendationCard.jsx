import { useState } from "react";
import "./RecommendationCard.css";

function RecommendationCard({ icon, title, statusColor, summary, whyText, language }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className={`rec-card rec-card-${statusColor}`}>
      <div className="rec-card-header">
        <div className="rec-card-icon">{icon}</div>
        <div className="rec-card-title">{title}</div>
      </div>

      <div className="rec-card-summary">{summary}</div>

      <button
        className="rec-card-why-toggle"
        onClick={() => setExpanded((prev) => !prev)}
      >
        {expanded
          ? language === "hi"
            ? "छुपाएं ▲"
            : "Hide ▲"
          : language === "hi"
          ? "क्यों? ▼"
          : "Why? ▼"}
      </button>

      {expanded && <div className="rec-card-why-text">{whyText}</div>}
    </div>
  );
}

export default RecommendationCard;