import { useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";

import { useAuth } from "../../context/useAuth";

const navigationItems = [
  { label: "Dashboard", path: "/dashboard" },
  { label: "Life Areas", path: "/life-areas" },
  { label: "Weekly Availability", path: "/weekly-availability" },
  { label: "Time Allocation", path: "/time-allocation" },
  { label: "Time Tracking", path: "/time-logs" },
  { label: "Insights", path: "/insights" },
  { label: "Rebalancing", path: "/rebalancing" },
];

function AppNavigation() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    setIsMobileMenuOpen(false);
    navigate("/login", { replace: true });
  };

  const handleNavigation = () => {
    setIsMobileMenuOpen(false);
  };

  return (
    <>
      <header className="app-mobile-header">
        <button
          type="button"
          className="app-menu-button"
          onClick={() => setIsMobileMenuOpen((open) => !open)}
          aria-label={isMobileMenuOpen ? "\u00D7" : "\u2630"}
          aria-expanded={isMobileMenuOpen}
        >
          {isMobileMenuOpen ? "\u00D7" : "\u2630"}
        </button>

        <span className="app-brand">WeekAhead</span>
      </header>

      {isMobileMenuOpen && (
        <button
          type="button"
          className="app-mobile-overlay"
          aria-label="Close navigation"
          onClick={() => setIsMobileMenuOpen(false)}
        />
      )}

      <aside
        className={`app-navigation ${
          isMobileMenuOpen ? "app-navigation-open" : ""
        }`}
      >
        <div className="app-navigation-header">
          <div>
            <p className="app-navigation-eyebrow">WeekAhead</p>
            <h1>Time Planner</h1>
          </div>
        </div>

        <nav className="app-navigation-links" aria-label="Main navigation">
          {navigationItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `app-navigation-link ${
                  isActive ? "app-navigation-link-active" : ""
                }`
              }
              onClick={handleNavigation}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="app-navigation-footer">
          <button
            type="button"
            className="app-navigation-logout"
            onClick={handleLogout}
          >
            Logout
          </button>
        </div>
      </aside>
    </>
  );
}

export default AppNavigation;
