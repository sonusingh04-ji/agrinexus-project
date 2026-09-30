import "./BigSlider.css";

function BigSlider({ value, onChange, min = 0.5, max = 20, step = 0.5, unit }) {
  return (
    <div className="big-slider-wrap">
      <div className="big-slider-value">
        {value} <span className="big-slider-unit">{unit}</span>
      </div>
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(e) => onChange(parseFloat(e.target.value))}
        className="big-slider-input"
      />
      <div className="big-slider-range-labels">
        <span>{min}</span>
        <span>{max}</span>
      </div>
    </div>
  );
}

export default BigSlider;