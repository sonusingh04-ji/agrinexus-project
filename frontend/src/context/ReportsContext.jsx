import {
  createContext,
  useContext,
  useState,
} from "react";

const ReportsContext =
    createContext(null);

export function ReportsProvider({
                                  children,
                                }) {
  const [reports, setReports] =
      useState([]);

  /*
   * Compatibility function.
   *
   * New production data should be saved through
   * a real backend endpoint, not localStorage.
   */
  const addReport = (
      report
  ) => {
    if (!report) {
      return;
    }

    setReports(
        (previous) => [
          {
            ...report,
            id:
                report.id ??
                crypto.randomUUID(),
            date:
                report.date ??
                new Date().toISOString(),
          },
          ...previous,
        ]
    );
  };


  /*
   * Kept only for older components that still call it.
   *
   * Real Field History now comes directly from:
   *
   * GET /api/crop-history/farm/{farmId}
   */
  const getReportsForField = (
      fieldId
  ) => {
    if (!fieldId) {
      return [];
    }

    return reports.filter(
        (report) =>
            String(
                report.fieldId
            ) ===
            String(fieldId)
    );
  };


  return (
      <ReportsContext.Provider
          value={{
            reports,
            addReport,
            getReportsForField,
          }}
      >
        {children}
      </ReportsContext.Provider>
  );
}


export function useReports() {
  const context =
      useContext(
          ReportsContext
      );

  if (!context) {
    throw new Error(
        "useReports must be used inside ReportsProvider"
    );
  }

  return context;
}