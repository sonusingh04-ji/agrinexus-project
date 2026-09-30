import "./WeatherCard.css";

function WeatherCard({
                         icon,
                         temp,
                         condition,
                         humidity,
                         rainfall,
                         language,
                     }) {
    const isHindi = language === "hi";

    return (
        <div className="weather-card">

            {/* Main weather information */}
            <div className="weather-card-main">

                <div className="weather-card-icon">
                    {icon}
                </div>

                <div>
                    <div className="weather-card-temp">
                        {temp !== null &&
                        temp !== undefined &&
                        temp !== "--"
                            ? `${temp}°C`
                            : "--"}
                    </div>

                    <div className="weather-card-condition">
                        {condition || "--"}
                    </div>
                </div>

            </div>


            {/* Weather details */}
            <div className="weather-card-details">

                {/* Humidity */}
                <div className="weather-detail-item">
          <span className="weather-detail-icon">
            💧
          </span>

                    <span>
            {isHindi ? "नमी" : "Humidity"}:{" "}
                        {humidity !== null &&
                        humidity !== undefined &&
                        humidity !== "--"
                            ? `${humidity}%`
                            : "--"}
          </span>
                </div>


                {/* Rainfall */}
                <div className="weather-detail-item">
          <span className="weather-detail-icon">
            🌧️
          </span>

                    <span>
            {isHindi ? "वर्षा" : "Rainfall"}:{" "}
                        {rainfall !== null &&
                        rainfall !== undefined &&
                        rainfall !== "--"
                            ? rainfall
                            : "--"}
                        {rainfall !== null &&
                        rainfall !== undefined &&
                        rainfall !== "--"
                            ? " mm"
                            : ""}
          </span>
                </div>

            </div>

        </div>
    );
}

export default WeatherCard;