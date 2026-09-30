import "./AlertBanner.css";

// type: "warning" | "danger" | "info"
function AlertBanner({ type = "warning", icon, message }) {
  return (
    <div className={`alert-banner alert-${type}`}>
      <span className="alert-icon">{icon}</span>
      <span className="alert-message">{message}</span>
    </div>
  );
}

export default AlertBanner;