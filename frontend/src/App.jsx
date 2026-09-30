import {
    BrowserRouter,
    Routes,
    Route,
    Navigate,
} from "react-router-dom";

import {
    LanguageProvider,
} from "./context/LanguageContext";

import {
    FieldsProvider,
} from "./context/FieldsContext";

import {
    ReportsProvider,
} from "./context/ReportsContext";


/* =========================================================
   PAGES
   ========================================================= */

import Splash from "./pages/Splash/Splash";

import Auth from "./pages/Auth/Auth";

import Home from "./pages/Home/Home";

import AddField from "./pages/AddField/AddField";

import CropSoilAnalysis
    from "./pages/CropSoilAnalysis/CropSoilAnalysis";

import RecommendationReport
    from "./pages/RecommendationReport/RecommendationReport";

import FieldHistory
    from "./pages/FieldHistory/FieldHistory";

import CropHistoryDetail
    from "./pages/CropHistoryDetail/CropHistoryDetail";

import FieldDetail
    from "./pages/FieldDetail/FieldDetail";

import FieldHealthPassport
    from "./pages/FieldHealthPassport/FieldHealthPassport";

import WeatherRadar
    from "./pages/WeatherRadar/WeatherRadar";

import SatelliteMonitoring
    from "./pages/SatelliteMonitoring/SatelliteMonitoring";

import NutrientBalance
    from "./pages/NutrientBalance/NutrientBalance";

import SowingAdvisor
    from "./pages/SowingAdvisor/SowingAdvisor";

import Irrigation
    from "./pages/Irrigation/Irrigation";

import Fertilizer
    from "./pages/Fertilizer/Fertilizer";

import LowestCostPlan
    from "./pages/LowestCostPlan/LowestCostPlan";

import AICopilot
    from "./pages/AICopilot/AICopilot";

import Profile
    from "./pages/Profile/Profile";


function App() {

    return (

        <LanguageProvider>

            <FieldsProvider>

                <ReportsProvider>

                    <BrowserRouter>

                        <Routes>


                            {/* =================================================
                               ENTRY
                            ================================================= */}

                            <Route
                                path="/"
                                element={
                                    <Splash />
                                }
                            />


                            {/* =================================================
                               AUTHENTICATION
                            ================================================= */}

                            <Route
                                path="/auth"
                                element={
                                    <Auth />
                                }
                            />


                            {/* =================================================
                               HOME
                            ================================================= */}

                            <Route
                                path="/home"
                                element={
                                    <Home />
                                }
                            />


                            {/* =================================================
                               FIELD MANAGEMENT
                            ================================================= */}

                            <Route
                                path="/add-field"
                                element={
                                    <AddField />
                                }
                            />


                            <Route
                                path="/field/:id"
                                element={
                                    <FieldDetail />
                                }
                            />


                            {/* =================================================
                               SOIL & CROP ANALYSIS
                            ================================================= */}

                            <Route
                                path="/analysis"
                                element={
                                    <CropSoilAnalysis />
                                }
                            />


                            {/* =================================================
                               CROP RECOMMENDATION REPORT
                            ================================================= */}

                            <Route
                                path="/recommendation-report"
                                element={
                                    <RecommendationReport />
                                }
                            />


                            {/* =================================================
                               CROP HISTORY
                            ================================================= */}

                            <Route
                                path="/history"
                                element={
                                    <FieldHistory />
                                }
                            />


                            <Route
                                path="/crop-history/:id"
                                element={
                                    <CropHistoryDetail />
                                }
                            />


                            {/* =================================================
                               NUTRIENT BALANCE
                            ================================================= */}

                            <Route
                                path="/nutrient-balance"
                                element={
                                    <NutrientBalance />
                                }
                            />


                            {/* =================================================
                               FIELD HEALTH PASSPORT
                            ================================================= */}

                            <Route
                                path="/field-health-passport"
                                element={
                                    <FieldHealthPassport />
                                }
                            />


                            {/* =================================================
                               WEATHER RISK RADAR

                               IMPORTANT:
                               LULC / Land Cover Mapping is integrated
                               INSIDE this page.

                               Therefore there is NO separate /lulc route.
                            ================================================= */}

                            <Route
                                path="/weather-radar"
                                element={
                                    <WeatherRadar />
                                }
                            />


                            {/* =================================================
                               SOWING ADVISOR
                            ================================================= */}

                            <Route
                                path="/sowing-advisor"
                                element={
                                    <SowingAdvisor />
                                }
                            />


                            {/* =================================================
                               SMART IRRIGATION
                            ================================================= */}

                            <Route
                                path="/irrigation"
                                element={
                                    <Irrigation />
                                }
                            />


                            {/* =================================================
                               FERTILIZER
                            ================================================= */}

                            <Route
                                path="/fertilizer"
                                element={
                                    <Fertilizer />
                                }
                            />


                            {/* =================================================
                               LOWEST COST PLAN
                            ================================================= */}

                            <Route
                                path="/lowest-cost-plan"
                                element={
                                    <LowestCostPlan />
                                }
                            />


                            {/* =================================================
                               AI COPILOT
                            ================================================= */}

                            <Route
                                path="/copilot"
                                element={
                                    <AICopilot />
                                }
                            />


                            {/* =================================================
                               SATELLITE MONITORING
                            ================================================= */}

                            <Route
                                path="/satellite"
                                element={
                                    <SatelliteMonitoring />
                                }
                            />


                            {/* =================================================
                               PROFILE
                            ================================================= */}

                            <Route
                                path="/profile"
                                element={
                                    <Profile />
                                }
                            />


                            {/* =================================================
                               FALLBACK

                               Any unknown URL → Home
                            ================================================= */}

                            <Route
                                path="*"
                                element={
                                    <Navigate
                                        to="/home"
                                        replace
                                    />
                                }
                            />

                        </Routes>

                    </BrowserRouter>

                </ReportsProvider>

            </FieldsProvider>

        </LanguageProvider>
    );
}


export default App;