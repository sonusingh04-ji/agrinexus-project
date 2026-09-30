import { useNavigate } from "react-router-dom";
import { useLanguage } from "../../context/LanguageContext";
import "./Splash.css";

function Splash() {
  const { setLanguage, t } = useLanguage();
  const navigate = useNavigate();

  const handleLanguageSelect = (lang) => {
    setLanguage(lang);
    navigate("/auth");
  };

  return (
    <div className="splash-container">
      <h1 className="splash-title">{t("appName")}</h1>
      <p className="splash-subtitle">{t("selectLanguage")}</p>

      <div className="splash-buttons">
        <button
          className="lang-button"
          onClick={() => handleLanguageSelect("hi")}
        >
          हिंदी
        </button>
        <button
          className="lang-button"
          onClick={() => handleLanguageSelect("en")}
        >
          English
        </button>
      </div>
    </div>
  );
}

export default Splash;