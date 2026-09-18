import LoginPage from "../pages/LoginPage";
import RegisterPage from "../pages/RegisterPage";
import { useEffect, useState } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { getHealth } from "../api/healthApi";
import ProtectedRoute from "./ProtectedRoute";
import HomePage from "../pages/HomePage";

function FoundationPage() {
  const [backendStatus, setBackendStatus] = useState("Checking...");
  const [error, setError] = useState("");

  useEffect(() => {
    let isMounted = true;

    getHealth()
      .then((data) => {
        if (isMounted) {
          setBackendStatus(data.status ?? "UNKNOWN");
        }
      })
      .catch(() => {
        if (isMounted) {
          setBackendStatus("DOWN");
          setError("Unable to connect to the backend.");
        }
      });

    return () => {
      isMounted = false;
    };
  }, []);

  return (
    <main className="app-shell">
      <section className="foundation-card">
        <p className="eyebrow">WeekAhead</p>

        <h1>AI-Assisted Weekly Time Budget Optimizer</h1>

        <p>Frontend foundation is running successfully.</p>

        <p>
          Backend status: <strong>{backendStatus}</strong>
        </p>

        {error && <p>{error}</p>}
      </section>
    </main>
  );
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<FoundationPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/login" element={<LoginPage />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/home" element={<HomePage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default AppRoutes;
