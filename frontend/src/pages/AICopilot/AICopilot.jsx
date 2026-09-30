import { useState, useRef } from "react";
import { useLanguage } from "../../context/LanguageContext";
import MicButton from "../../components/MicButton";
import BottomNavBar from "../../components/BottomNavBar";
import "./AICopilot.css";

function AICopilot() {
  const { language } = useLanguage();
  const [listening, setListening] = useState(false);
  const [question, setQuestion] = useState("");
  const [answer, setAnswer] = useState("");
  const [asking, setAsking] = useState(false);
  const recognitionRef = useRef(null);

  const speechSupported =
    "webkitSpeechRecognition" in window || "SpeechRecognition" in window;

  const speak = (text) => {
    if (!("speechSynthesis" in window)) return;
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = language === "hi" ? "hi-IN" : "en-IN";
    window.speechSynthesis.speak(utterance);
  };

  const getAnswer = (q) => {
    setAsking(true);
    setTimeout(() => {
      const fakeAnswer =
        language === "hi"
          ? "आपके खेत 1 में मिट्टी की नमी कम है। आज सिंचाई करने की सलाह दी जाती है।"
          : "Your Field 1 has low soil moisture. It's recommended to irrigate today.";
      setAnswer(fakeAnswer);
      setAsking(false);
      speak(fakeAnswer);
    }, 1800);
  };

  const handleMicClick = () => {
    if (!speechSupported) {
      alert(
        language === "hi"
          ? "आपका ब्राउज़र आवाज़ पहचान का समर्थन नहीं करता। कृपया टाइप करें।"
          : "Your browser doesn't support voice recognition. Please type instead."
      );
      return;
    }

    const SpeechRecognition =
      window.SpeechRecognition || window.webkitSpeechRecognition;
    const recognition = new SpeechRecognition();
    recognition.lang = language === "hi" ? "hi-IN" : "en-IN";
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;

    recognition.onstart = () => {
      console.log("Mic started listening");
      setListening(true);
    };

    recognition.onend = () => {
      console.log("Mic stopped listening");
      setListening(false);
    };

    recognition.onerror = (event) => {
      console.error("Speech recognition error:", event.error);
      setListening(false);
      if (
        event.error === "not-allowed" ||
        event.error === "service-not-allowed"
      ) {
        alert(
          language === "hi"
            ? "कृपया माइक्रोफ़ोन की अनुमति दें (ब्राउज़र सेटिंग जांचें)।"
            : "Please allow microphone access (check browser permissions)."
        );
      } else if (event.error === "no-speech") {
        alert(
          language === "hi"
            ? "कोई आवाज़ नहीं सुनाई दी। फिर से कोशिश करें।"
            : "No speech detected. Please try again."
        );
      } else {
        alert((language === "hi" ? "त्रुटि: " : "Error: ") + event.error);
      }
    };

    recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript;
      console.log("Heard:", transcript);
      setQuestion(transcript);
      getAnswer(transcript);
    };

    recognitionRef.current = recognition;
    recognition.start();
  };

  return (
    <div className="copilot-container">
      <h1 className="copilot-title">
        {language === "hi" ? "AI सहायक से पूछें" : "Ask Your AI Copilot"}
      </h1>

      <div className="copilot-main">
        <MicButton listening={listening} onClick={handleMicClick} />
        <p className="copilot-hint">
          {listening
            ? language === "hi"
              ? "सुन रहा हूं..."
              : "Listening..."
            : language === "hi"
            ? "बोलने के लिए माइक दबाएं"
            : "Tap mic to speak"}
        </p>
      </div>

      {question && (
        <div className="copilot-qa-box">
          <div className="copilot-question">
            <span className="copilot-qa-label">
              {language === "hi" ? "आपने पूछा:" : "You asked:"}
            </span>
            <p>{question}</p>
          </div>

          {asking ? (
            <div className="copilot-loading">
              {language === "hi" ? "सोच रहा हूं..." : "Thinking..."}
            </div>
          ) : (
            answer && (
              <div className="copilot-answer">
                <span className="copilot-qa-label">
                  {language === "hi" ? "जवाब:" : "Answer:"}
                </span>
                <p>{answer}</p>
                <button
                  className="copilot-replay"
                  onClick={() => speak(answer)}
                >
                  🔊 {language === "hi" ? "फिर सुनें" : "Replay"}
                </button>
              </div>
            )
          )}
        </div>
      )}

      <BottomNavBar />
    </div>
  );
}

export default AICopilot;