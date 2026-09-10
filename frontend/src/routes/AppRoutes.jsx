import { Navigate, Route, Routes } from "react-router-dom";

function FoundationPage() {
  return (
    <main className="app-shell">
      <section className="foundation-card">
        <p className="eyebrow">WeekAhead</p>

        <h1>AI-Assisted Weekly Time Budget Optimizer</h1>

        <p>Frontend foundation is running successfully.</p>
      </section>
    </main>
  );
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<FoundationPage />} />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default AppRoutes;
