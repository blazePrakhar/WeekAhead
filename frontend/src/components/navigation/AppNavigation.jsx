import { useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import { NavLink, useNavigate } from "react-router-dom";
import {
  ArrowLeftRight,
  CalendarClock,
  CalendarRange,
  Layers3,
  LayoutDashboard,
  LogOut,
  Sparkles,
  Timer,
} from "lucide-react";

import { useAuth } from "../../context/useAuth";

const navigationItems = [
  {
    label: "Dashboard",
    path: "/dashboard",
    icon: LayoutDashboard,
  },
  {
    label: "Life Areas",
    path: "/life-areas",
    icon: Layers3,
  },
  {
    label: "Weekly Availability",
    path: "/weekly-availability",
    icon: CalendarClock,
  },
  {
    label: "Time Allocation",
    path: "/time-allocation",
    icon: CalendarRange,
  },
  {
    label: "Time Tracking",
    path: "/time-logs",
    icon: Timer,
  },
  {
    label: "Insights",
    path: "/insights",
    icon: Sparkles,
  },
  {
    label: "Rebalancing",
    path: "/rebalancing",
    icon: ArrowLeftRight,
  },
];

const navigationTransition = {
  duration: 0.22,
  ease: [0.2, 0.8, 0.2, 1],
};

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
        <motion.button
          type="button"
          className="app-menu-button"
          onClick={() => setIsMobileMenuOpen((open) => !open)}
          aria-label={isMobileMenuOpen ? "Close navigation" : "Open navigation"}
          aria-expanded={isMobileMenuOpen}
          whileTap={{ scale: 0.94 }}
        >
          <span
            className={`app-menu-icon ${
              isMobileMenuOpen ? "app-menu-icon-open" : ""
            }`}
            aria-hidden="true"
          >
            {isMobileMenuOpen ? "×" : "☰"}
          </span>
        </motion.button>

        <div className="app-mobile-brand">
          <span className="app-brand-mark" aria-hidden="true">
            W
          </span>

          <span className="app-brand">WeekAhead</span>
        </div>
      </header>

      <AnimatePresence>
        {isMobileMenuOpen && (
          <motion.button
            type="button"
            className="app-mobile-overlay"
            aria-label="Close navigation"
            onClick={() => setIsMobileMenuOpen(false)}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={navigationTransition}
          />
        )}
      </AnimatePresence>

      <motion.aside
        className={`app-navigation ${
          isMobileMenuOpen ? "app-navigation-open" : ""
        }`}
        initial={false}
      >
        <div className="app-navigation-header">
          <div className="app-navigation-brand">
            <span className="app-brand-mark" aria-hidden="true">
              W
            </span>

            <div>
              <p className="app-navigation-eyebrow">WeekAhead</p>
              <h1>Time Planner</h1>
            </div>
          </div>

          <p className="app-navigation-tagline">
            Plan your time with intention.
          </p>
        </div>

        <nav className="app-navigation-links" aria-label="Main navigation">
          <p className="app-navigation-section-label">Workspace</p>

          {navigationItems.map((item, index) => (
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
              {({ isActive }) => (
                <>
                  <motion.span
                    className="app-navigation-link-icon"
                    aria-hidden="true"
                    animate={{
                      scale: isActive ? 1 : 0.96,
                    }}
                    transition={{
                      duration: 0.2,
                      delay: index * 0.01,
                    }}
                  >
                    <item.icon size={18} strokeWidth={1.8} aria-hidden="true" />
                  </motion.span>

                  <span className="app-navigation-link-label">
                    {item.label}
                  </span>

                  {isActive && (
                    <motion.span
                      className="app-navigation-active-indicator"
                      layoutId="active-navigation-indicator"
                      transition={navigationTransition}
                      aria-hidden="true"
                    />
                  )}
                </>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="app-navigation-footer">
          <div className="app-navigation-footer-card">
            <div className="app-navigation-footer-icon" aria-hidden="true">
              ✦
            </div>

            <div className="app-navigation-footer-copy">
              <strong>Stay intentional</strong>
              <span>Make your week count.</span>
            </div>
          </div>

          <motion.button
            type="button"
            className="app-navigation-logout"
            onClick={handleLogout}
            whileTap={{ scale: 0.98 }}
          >
            <span className="app-navigation-logout-icon" aria-hidden="true">
              <LogOut size={18} strokeWidth={1.8} />
            </span>

            <span>Logout</span>
          </motion.button>
        </div>
      </motion.aside>
    </>
  );
}

export default AppNavigation;
