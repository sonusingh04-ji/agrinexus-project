import { useEffect, useState } from "react";
import "./AnalyzingLoader.css";

function AnalyzingLoader({ messages }) {
  const [index, setIndex] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setIndex((prev) => (prev + 1) % messages.length);
    }, 1800);
    return () => clearInterval(interval);
  }, [messages]);

  return (
    <div className="analyzing-overlay">
      <div className="analyzing-box">
        <div className="analyzing-spinner">🌱</div>
        <div className="analyzing-text">{messages[index]}</div>
      </div>
    </div>
  );
}

export default AnalyzingLoader;