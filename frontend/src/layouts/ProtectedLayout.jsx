import { Outlet } from "react-router-dom";

import AppNavigation from "../components/navigation/AppNavigation";

function ProtectedLayout() {
  return (
    <div className="protected-layout">
      <AppNavigation />

      <main className="protected-layout-content">
        <Outlet />
      </main>
    </div>
  );
}

export default ProtectedLayout;
