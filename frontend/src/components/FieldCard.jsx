import "./FieldCard.css";

function FieldCard({
  fieldName,
  cropIcon,
  statusText,
  statusColor,
  stage,
  stageLabel,
  moisture,
  lastUpdated,
  onClick,
  onDelete,
}) {
  const stages = ["seedling", "growing", "flowering", "harvest"];
  const stageIndex = stages.indexOf(stage);

  const handleDeleteClick = (e) => {
    e.stopPropagation(); // don't trigger onClick (field detail) when deleting
    onDelete();
  };

  return (
    <div className="field-card" onClick={onClick}>
      <div className="field-card-top">
        <div className="field-card-icon-wrap">
          <span className="field-card-icon">{cropIcon}</span>
        </div>
        <div className="field-card-info">
          <div className="field-card-name">{fieldName}</div>
          <div className={`field-card-status field-status-${statusColor}`}>
            {statusText}
          </div>
        </div>
        {onDelete && (
          <button className="field-card-delete" onClick={handleDeleteClick}>
            🗑️
          </button>
        )}
        <div className="field-card-arrow">›</div>
      </div>

      <div className="field-card-stage">
        {stages.map((s, idx) => (
          <div key={s} className="stage-step">
            <div
              className={`stage-dot ${idx <= stageIndex ? "stage-dot-filled" : ""}`}
            />
            {idx < stages.length - 1 && (
              <div
                className={`stage-line ${idx < stageIndex ? "stage-line-filled" : ""}`}
              />
            )}
          </div>
        ))}
      </div>
      <div className="field-card-stage-label">{stageLabel}</div>

      <div className="field-card-footer">
        <div className="field-footer-item">
          <span>💧</span> {moisture}
        </div>
        <div className="field-footer-item field-footer-time">
          🕒 {lastUpdated}
        </div>
      </div>
    </div>
  );
}

export default FieldCard;