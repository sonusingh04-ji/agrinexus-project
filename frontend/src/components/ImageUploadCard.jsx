import { useRef } from "react";
import "./ImageUploadCard.css";

function ImageUploadCard({ icon, title, image, onImageSelect, onRetake }) {
  const inputRef = useRef(null);

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      const imageUrl = URL.createObjectURL(file);
      onImageSelect(file, imageUrl);
    }
  };

  const handleClick = () => {
    inputRef.current.click();
  };

  return (
    <div className="upload-card">
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        capture="environment"
        style={{ display: "none" }}
        onChange={handleFileChange}
      />

      {!image ? (
        <div className="upload-card-empty" onClick={handleClick}>
          <div className="upload-card-icon">{icon}</div>
          <div className="upload-card-title">{title}</div>
        </div>
      ) : (
        <div className="upload-card-filled">
          <img src={image} alt={title} className="upload-card-preview" />
          <button className="upload-card-retake" onClick={onRetake}>
            🔄 Retake
          </button>
        </div>
      )}
    </div>
  );
}

export default ImageUploadCard;