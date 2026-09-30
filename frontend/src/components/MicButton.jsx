import "./MicButton.css";

function MicButton({ listening, onClick }) {
  return (
    <button
      className={`mic-button ${listening ? "mic-button-listening" : ""}`}
      onClick={onClick}
    >
      <span className="mic-icon">🎤</span>
    </button>
  );
}

export default MicButton;