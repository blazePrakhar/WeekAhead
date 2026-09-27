import { useEffect, useMemo, useState } from "react";
import {
  Activity,
  AlertTriangle,
  CalendarDays,
  Clock3,
  LayoutDashboard,
  Target,
  TrendingUp,
} from "lucide-react";
import { motion } from "motion/react";

import { getWeeklyDashboard } from "../api/dashboardApi";

function formatHours(minutes) {
  const value = Number(minutes ?? 0);

  if (!Number.isFinite(value)) {
    return "0h";
  }

  const hours = value / 60;

  return `${Number.isInteger(hours) ? hours : hours.toFixed(1)}h`;
}

function formatPercentage(value) {
  const number = Number(value);

  if (!Number.isFinite(number)) {
    return "0%";
  }

  return `${number.toFixed(0)}%`;
}

function calculateUtilization(actualMinutes, recommendedMinutes) {
  const actual = Number(actualMinutes ?? 0);
  const recommended = Number(recommendedMinutes ?? 0);

  if (!Number.isFinite(actual) || !Number.isFinite(recommended)) {
    return 0;
  }

  if (recommended <= 0) {
    return 0;
  }

  return (actual / recommended) * 100;
}

function getDifferenceMinutes(recommendedMinutes, actualMinutes) {
  const recommended = Number(recommendedMinutes ?? 0);
  const actual = Number(actualMinutes ?? 0);

  if (!Number.isFinite(recommended) || !Number.isFinite(actual)) {
    return 0;
  }

  return actual - recommended;
}

function getUtilizationClass(utilization) {
  if (utilization >= 100) {
    return "weekly-dashboard-status weekly-dashboard-status-success";
  }

  if (utilization >= 80) {
    return "weekly-dashboard-status weekly-dashboard-status-warning";
  }

  return "weekly-dashboard-status weekly-dashboard-status-danger";
}

function DashboardPage() {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;

    const loadDashboard = async () => {
      setLoading(true);
      setError("");

      try {
        const response = await getWeeklyDashboard();

        if (mounted) {
          setDashboard(response?.data ?? response);
        }
      } catch (requestError) {
        if (mounted) {
          setError(
            requestError?.response?.data?.message ||
              "Unable to load the weekly dashboard.",
          );
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    loadDashboard();

    return () => {
      mounted = false;
    };
  }, []);

  const lifeAreas = useMemo(() => {
    if (!dashboard?.lifeAreas) {
      return [];
    }

    return Array.isArray(dashboard.lifeAreas) ? dashboard.lifeAreas : [];
  }, [dashboard]);

  const totalAvailableMinutes = Number(dashboard?.availableMinutes ?? 0);

  const totalRecommendedMinutes = Number(
    dashboard?.totalRecommendedMinutes ?? 0,
  );

  const totalActualMinutes = Number(dashboard?.totalActualMinutes ?? 0);

  const remainingMinutes = Math.max(
    0,
    totalAvailableMinutes - totalActualMinutes,
  );

  const overallUtilization = calculateUtilization(
    totalActualMinutes,
    totalRecommendedMinutes,
  );

  if (loading) {
    return (
      <main className="weekly-dashboard-page">
        <div className="weekly-dashboard-container">
          <section className="weekly-dashboard-header">
            <div>
              <p className="weekly-dashboard-eyebrow">WEEK AHEAD</p>
              <h1>Weekly Dashboard</h1>
              <p>A clear view of how your current week is progressing.</p>
            </div>
          </section>

          <div className="weekly-dashboard-feedback">
            <Clock3 size={20} />
            <span>Loading your current week...</span>
          </div>
        </div>
      </main>
    );
  }

  if (error) {
    return (
      <main className="weekly-dashboard-page">
        <div className="weekly-dashboard-container">
          <section className="weekly-dashboard-header">
            <div>
              <p className="weekly-dashboard-eyebrow">WEEK AHEAD</p>
              <h1>Weekly Dashboard</h1>
              <p>A clear view of how your current week is progressing.</p>
            </div>
          </section>

          <div className="weekly-dashboard-feedback weekly-dashboard-feedback-error">
            <AlertTriangle size={20} />

            <div>
              <strong>Unable to load dashboard</strong>
              <p>{error}</p>
            </div>
          </div>
        </div>
      </main>
    );
  }

  if (!dashboard) {
    return (
      <main className="weekly-dashboard-page">
        <div className="weekly-dashboard-container">
          <section className="weekly-dashboard-header">
            <div>
              <p className="weekly-dashboard-eyebrow">WEEK AHEAD</p>
              <h1>Weekly Dashboard</h1>
              <p>A clear view of how your current week is progressing.</p>
            </div>
          </section>

          <div className="weekly-dashboard-empty">
            <div className="weekly-dashboard-empty-icon">
              <LayoutDashboard size={22} />
            </div>

            <h2>No dashboard data available</h2>

            <p>
              Create or configure your current week before viewing the
              dashboard.
            </p>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="weekly-dashboard-page">
      <div className="weekly-dashboard-container">
        <motion.header
          className="weekly-dashboard-header"
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.28, ease: "easeOut" }}
        >
          <div>
            <p className="weekly-dashboard-eyebrow">WEEK AHEAD</p>

            <h1>Weekly Dashboard</h1>

            <p>
              {dashboard.weekStartDate
                ? `Week starting ${dashboard.weekStartDate}`
                : "Overview of your current week."}
            </p>
          </div>

          <div className="weekly-dashboard-week-badge">
            <CalendarDays size={16} />

            <span>
              {dashboard.weekStartDate
                ? dashboard.weekStartDate
                : "Current week"}
            </span>
          </div>
        </motion.header>

        <section className="weekly-dashboard-section">
          <div className="weekly-dashboard-section-header">
            <div>
              <p className="weekly-dashboard-kicker">AT A GLANCE</p>

              <h2>Weekly Overview</h2>

              <p>
                See how much time is available, planned, tracked, and still
                remaining.
              </p>
            </div>
          </div>

          <div className="weekly-dashboard-summary-grid">
            <motion.article
              className="weekly-dashboard-summary-card"
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.25, delay: 0.04 }}
            >
              <div className="weekly-dashboard-summary-icon">
                <Clock3 size={18} />
              </div>

              <span>Available</span>

              <strong>{formatHours(totalAvailableMinutes)}</strong>
            </motion.article>

            <motion.article
              className="weekly-dashboard-summary-card"
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.25, delay: 0.08 }}
            >
              <div className="weekly-dashboard-summary-icon">
                <Target size={18} />
              </div>

              <span>Recommended</span>

              <strong>{formatHours(totalRecommendedMinutes)}</strong>
            </motion.article>

            <motion.article
              className="weekly-dashboard-summary-card"
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.25, delay: 0.12 }}
            >
              <div className="weekly-dashboard-summary-icon">
                <Activity size={18} />
              </div>

              <span>Tracked</span>

              <strong>{formatHours(totalActualMinutes)}</strong>
            </motion.article>

            <motion.article
              className="weekly-dashboard-summary-card"
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.25, delay: 0.16 }}
            >
              <div className="weekly-dashboard-summary-icon">
                <Clock3 size={18} />
              </div>

              <span>Remaining</span>

              <strong>{formatHours(remainingMinutes)}</strong>
            </motion.article>

            <motion.article
              className="weekly-dashboard-summary-card weekly-dashboard-summary-card-accent"
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.25, delay: 0.2 }}
            >
              <div className="weekly-dashboard-summary-icon">
                <TrendingUp size={18} />
              </div>

              <span>Utilization</span>

              <strong>{formatPercentage(overallUtilization)}</strong>
            </motion.article>
          </div>
        </section>

        <section className="weekly-dashboard-section">
          <div className="weekly-dashboard-section-header">
            <div>
              <p className="weekly-dashboard-kicker">BALANCE</p>

              <h2>Life Area Breakdown</h2>

              <p>
                Compare recommended time with the time you have actually
                tracked.
              </p>
            </div>
          </div>

          {lifeAreas.length === 0 ? (
            <div className="weekly-dashboard-empty">
              <div className="weekly-dashboard-empty-icon">
                <Target size={22} />
              </div>

              <h3>No life-area data</h3>

              <p>
                There are no life-area allocation records for this week yet.
              </p>
            </div>
          ) : (
            <div className="weekly-dashboard-balance-card">
              <div className="weekly-dashboard-balance-header">
                <span>Life Area</span>
                <span>Recommended</span>
                <span>Actual</span>
                <span>Difference</span>
                <span>Utilization</span>
              </div>

              <div className="weekly-dashboard-balance-list">
                {lifeAreas.map((area) => {
                  const utilization = calculateUtilization(
                    area.actualMinutes,
                    area.recommendedMinutes,
                  );

                  const difference = getDifferenceMinutes(
                    area.recommendedMinutes,
                    area.actualMinutes,
                  );

                  return (
                    <motion.div
                      className="weekly-dashboard-balance-row"
                      key={area.lifeAreaId}
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      transition={{ duration: 0.25 }}
                    >
                      <div className="weekly-dashboard-balance-area">
                        <div className="weekly-dashboard-balance-area-icon">
                          <Target size={16} />
                        </div>

                        <strong>{area.lifeAreaName}</strong>
                      </div>

                      <span>{formatHours(area.recommendedMinutes)}</span>

                      <span>{formatHours(area.actualMinutes)}</span>

                      <span
                        className={
                          difference >= 0
                            ? "weekly-dashboard-difference weekly-dashboard-difference-positive"
                            : "weekly-dashboard-difference weekly-dashboard-difference-negative"
                        }
                      >
                        {difference >= 0 ? "+" : ""}
                        {formatHours(Math.abs(difference))}
                      </span>

                      <span className={getUtilizationClass(utilization)}>
                        {formatPercentage(utilization)}
                      </span>
                    </motion.div>
                  );
                })}
              </div>
            </div>
          )}
        </section>
      </div>
    </main>
  );
}

export default DashboardPage;
