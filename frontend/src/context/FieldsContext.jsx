/* eslint-disable react-refresh/only-export-components */

import {
    createContext,
    useCallback,
    useContext,
    useEffect,
    useMemo,
    useState,
} from "react";

import {
    apiRequest,
    getCurrentFarmId,
    setCurrentFarmId,
    clearCurrentFarmId,
} from "../api/apiClient";


const FieldsContext = createContext(null);


/* =========================================================
   Fields Provider
========================================================= */

export function FieldsProvider({ children }) {

    const [fields, setFields] = useState([]);

    const [loading, setLoading] = useState(true);

    const [error, setError] = useState("");

    const [
        currentFarmId,
        setCurrentFarmIdState,
    ] = useState(
        () => getCurrentFarmId()
    );


    /* =====================================================
       Normalize backend FarmResponse
    ===================================================== */

    const normalizeFarm = useCallback(
        (farm) => {

            if (!farm?.id) {
                return null;
            }

            const farmId =
                String(farm.id);

            return {

                id: farmId,

                name:
                    farm.name ||
                    "AgriNexus Farm",

                nameHi:
                    farm.nameHi ||
                    farm.name ||
                    "एग्रीनेक्सस खेत",

                icon:
                    farm.icon ||
                    "🌾",

                cropId:
                    farm.cropId ??
                    null,

                cropName:
                    farm.cropName ??
                    null,

                statusText:
                    farm.statusText ??
                    "UNKNOWN",

                statusTextHi:
                    farm.statusTextHi ??
                    "अज्ञात",

                statusColor:
                    farm.statusColor ??
                    "good",

                stage:
                    farm.stage ??
                    "UNKNOWN",

                stageLabel:
                    farm.stageLabel ??
                    farm.stage ??
                    "Unknown",

                stageLabelHi:
                    farm.stageLabelHi ??
                    farm.stage ??
                    "अज्ञात",

                moisture:
                    farm.moisture ??
                    "—",

                moistureHi:
                    farm.moistureHi ??
                    farm.moisture ??
                    "—",

                lastUpdated:
                    farm.lastUpdated ??
                    farm.updatedAt ??
                    farm.createdAt ??
                    "—",

                lastUpdatedHi:
                    farm.lastUpdatedHi ??
                    farm.lastUpdated ??
                    farm.updatedAt ??
                    farm.createdAt ??
                    "—",

                sizeAcres:
                    farm.sizeAcres ??
                    farm.areaAcres ??
                    null,

                location:
                    farm.location
                        ? {
                            name:
                            farm.location,

                            lat:
                                farm.latitude ??
                                null,

                            lng:
                                farm.longitude ??
                                null,
                        }
                        : (
                            farm.latitude != null &&
                            farm.longitude != null
                        )
                            ? {
                                name: "",

                                lat:
                                farm.latitude,

                                lng:
                                farm.longitude,
                            }
                            : null,

                farmerId:
                    farm.farmerId ??
                    null,

                latitude:
                    farm.latitude ??
                    null,

                longitude:
                    farm.longitude ??
                    null,

                areaAcres:
                    farm.areaAcres ??
                    null,

                createdAt:
                    farm.createdAt ??
                    null,

                updatedAt:
                    farm.updatedAt ??
                    null,
            };
        },
        []
    );


    /* =====================================================
       Load all farms belonging to logged-in farmer
    ===================================================== */

    const loadFields = useCallback(
        async () => {

            const farmerId =
                localStorage.getItem(
                    "agrinexus_user_id"
                );


            if (!farmerId) {

                setFields([]);

                clearCurrentFarmId();

                setCurrentFarmIdState(
                    null
                );

                setError(
                    "Logged-in farmer ID not found."
                );

                setLoading(false);

                return [];
            }


            setLoading(true);

            setError("");


            try {

                const response =
                    await apiRequest(
                        `/api/farms/farmer/${encodeURIComponent(
                            farmerId
                        )}`
                    );


                let farms = [];


                if (
                    Array.isArray(
                        response
                    )
                ) {

                    farms =
                        response;

                } else if (
                    Array.isArray(
                        response?.content
                    )
                ) {

                    farms =
                        response.content;

                } else if (
                    Array.isArray(
                        response?.data
                    )
                ) {

                    farms =
                        response.data;

                } else if (
                    response?.id
                ) {

                    farms = [
                        response,
                    ];
                }


                const normalized =
                    farms
                        .map(
                            normalizeFarm
                        )
                        .filter(Boolean);


                setFields(
                    normalized
                );

                setError("");


                if (
                    normalized.length > 0
                ) {

                    const savedId =
                        getCurrentFarmId();


                    const savedFarmExists =
                        normalized.some(
                            (farm) =>
                                String(
                                    farm.id
                                ) ===
                                String(
                                    savedId
                                )
                        );


                    if (
                        savedId &&
                        savedFarmExists
                    ) {

                        setCurrentFarmId(
                            savedId
                        );

                        setCurrentFarmIdState(
                            String(
                                savedId
                            )
                        );

                    } else {

                        const firstFarmId =
                            String(
                                normalized[0].id
                            );


                        setCurrentFarmId(
                            firstFarmId
                        );

                        setCurrentFarmIdState(
                            firstFarmId
                        );
                    }

                } else {

                    clearCurrentFarmId();

                    setCurrentFarmIdState(
                        null
                    );
                }


                return normalized;

            } catch (
                requestError
                ) {

                console.error(
                    "Failed to load farms:",
                    requestError
                );


                setFields([]);

                clearCurrentFarmId();

                setCurrentFarmIdState(
                    null
                );


                setError(
                    requestError?.message ||
                    "Unable to load farms."
                );


                throw requestError;

            } finally {

                setLoading(false);
            }
        },
        [
            normalizeFarm,
        ]
    );


    /* =====================================================
       Initial load
    ===================================================== */

    useEffect(() => {

        const timer =
            setTimeout(() => {
                void loadFields();
            }, 0);


        return () => {
            clearTimeout(timer);
        };

    }, [
        loadFields,
    ]);


    /* =====================================================
       Current selected farm
    ===================================================== */

    const currentField =
        useMemo(() => {

            if (
                fields.length === 0
            ) {
                return null;
            }


            if (
                currentFarmId
            ) {

                const selected =
                    fields.find(
                        (field) =>
                            String(
                                field.id
                            ) ===
                            String(
                                currentFarmId
                            )
                    );


                if (selected) {
                    return selected;
                }
            }


            return fields[0];

        }, [
            fields,
            currentFarmId,
        ]);


    /* =====================================================
       Select Farm
    ===================================================== */

    const selectField =
        useCallback(
            (farmId) => {

                if (!farmId) {
                    return;
                }


                const normalizedFarmId =
                    String(
                        farmId
                    );


                const exists =
                    fields.some(
                        (field) =>
                            String(
                                field.id
                            ) ===
                            normalizedFarmId
                    );


                if (!exists) {

                    console.warn(
                        "Cannot select unknown farm:",
                        farmId
                    );

                    return;
                }


                setCurrentFarmId(
                    normalizedFarmId
                );


                setCurrentFarmIdState(
                    normalizedFarmId
                );

            },
            [
                fields,
            ]
        );


    /* =====================================================
       Add Field / Farm
    ===================================================== */

    const addField =
        useCallback(
            async () => {

                return loadFields();

            },
            [
                loadFields,
            ]
        );


    /* =====================================================
       Delete Field / Farm
    =====================================================

       REAL BACKEND:

       DELETE /api/farms/{farmId}
    ===================================================== */

    const deleteField =
        useCallback(
            async (
                farmId
            ) => {

                if (!farmId) {

                    throw new Error(
                        "Farm ID is required"
                    );
                }


                const normalizedFarmId =
                    String(
                        farmId
                    );


                const farmExists =
                    fields.some(
                        (field) =>
                            String(
                                field?.id
                            ) ===
                            normalizedFarmId
                    );


                if (!farmExists) {

                    throw new Error(
                        "Farm not found in the current farm list."
                    );
                }


                try {

                    /*
                     * REAL BACKEND DELETE
                     */
                    await apiRequest(
                        `/api/farms/${encodeURIComponent(
                            normalizedFarmId
                        )}`,
                        {
                            method: "DELETE",
                        }
                    );


                    /*
                     * Reload the real backend
                     * farm list.
                     *
                     * loadFields() will automatically
                     * select another real farm if the
                     * deleted farm was selected.
                     */
                    return await loadFields();

                } catch (
                    deleteError
                    ) {

                    console.error(
                        "Failed to delete farm:",
                        deleteError
                    );

                    throw deleteError;
                }
            },
            [
                fields,
                loadFields,
            ]
        );


    /* =====================================================
       Provider value
    ===================================================== */

    const contextValue =
        useMemo(
            () => ({

                fields,

                setFields,

                currentField,

                currentFarmId:
                    currentField?.id ??
                    currentFarmId ??
                    null,

                loading,

                error,

                reloadFields:
                loadFields,

                selectField,

                addField,

                deleteField,

            }),
            [
                fields,
                currentField,
                currentFarmId,
                loading,
                error,
                loadFields,
                selectField,
                addField,
                deleteField,
            ]
        );


    /* =====================================================
       Provider
    ===================================================== */

    return (
        <FieldsContext.Provider
            value={
                contextValue
            }
        >
            {children}
        </FieldsContext.Provider>
    );
}


/* =========================================================
   Hook
========================================================= */

export function useFields() {

    const context =
        useContext(
            FieldsContext
        );


    if (!context) {

        throw new Error(
            "useFields must be used inside FieldsProvider"
        );
    }


    return context;
}