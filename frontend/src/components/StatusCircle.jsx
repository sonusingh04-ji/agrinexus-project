import "./StatusCircle.css";

// status: "good" | "warning" | "danger"
function StatusCircle({ status = "good", icon, label }) {
  return (
    <div className={`status-circle status-${status}`}>
      <div className="status-icon">{icon}</div>
      <div className="status-label">{label}</div>
    </div>
  );
}

export default StatusCircle;