import { useEffect, useMemo, useState } from "react";

import { getWeeklyDashboard } from "../api/dashboardApi";
import { getNeglectInsights } from "../api/neglectApi";

function formatHours(minutes = 0) {
  return `${(minutes / 60).toFixed(1)} h`;
}

function formatUtilization(value = 0) {
  return `${(value * 100).toFixed(1)}%`;
}

function getNeglectMap(neglectInsights) {
  return new Map(neglectInsights.map((item) => [item.lifeAreaId, item]));
}

function getCurrentWeekStatus(recommendedMinutes, actualMinutes) {
  if (recommendedMinutes <= 0) {
    return "No recommendation";
  }

  const utilization = actualMinutes / recommendedMinutes;

  if (utilization < 0.8) {
    return "Below 80% this week";
  }

  return "Meeting recommendation";
}

function InsightsPage() {
  const [dashboard, setDashboard] = useState(null);
  const [neglectInsights, setNeglectInsights] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function loadInsights() {
      try {
        setLoading(true);
        setError("");

        const [dashboardData, neglectData] = await Promise.all([
          getWeeklyDashboard(),
          getNeglectInsights(),
        ]);

        if (!cancelled) {
          setDashboard(dashboardData);
          setNeglectInsights(neglectData);
        }
      } catch (err) {
        if (!cancelled) {
          console.error("Failed to load insights:", err);

          setError(
            err.response?.data?.message ||
              "Unable to load insights. Please try again.",
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadInsights();

    return () => {
      cancelled = true;
    };
  }, []);

  const neglectMap = useMemo(
    () => getNeglectMap(neglectInsights),
    [neglectInsights],
  );

  const overallUtilization = useMemo(() => {
    if (!dashboard || dashboard.totalRecommendedMinutes <= 0) {
      return 0;
    }

    return dashboard.totalActualMinutes / dashboard.totalRecommendedMinutes;
  }, [dashboard]);

  const neglectedAreas = useMemo(
    () =>
      neglectInsights.filter((item) => item.consecutiveUnderTargetWeeks >= 2),
    [neglectInsights],
  );

  if (loading) {
    return (
      <main className="weekly-availability-page">
        <section className="weekly-availability-header">
          <div>
            <h1>Insights</h1>
            <p className="page-description">
              Compare your actual tracked time with your weekly recommendations.
            </p>
          </div>
        </section>

        <div className="weekly-availability-status">
          <p>Loading insights...</p>
        </div>
      </main>
    );
  }

  if (error) {
    return (
      <main className="weekly-availability-page">
        <section className="weekly-availability-header">
          <div>
            <h1>Insights</h1>
            <p className="page-description">
              Compare your actual tracked time with your weekly recommendations.
            </p>
          </div>
        </section>

        <div className="weekly-availability-error">
          <p>{error}</p>
        </div>
      </main>
    );
  }

  if (!dashboard) {
    return (
      <main className="weekly-availability-page">
        <section className="weekly-availability-header">
          <div>
            <h1>Insights</h1>
            <p className="page-description">
              Compare your actual tracked time with your weekly recommendations.
            </p>
          </div>
        </section>

        <div className="empty-state">
          <p>No dashboard data is available.</p>
        </div>
      </main>
    );
  }

  return (
    <main className="weekly-availability-page">
      {/* Page Header */}
      <section className="weekly-availability-header">
        <div>
          <h1>Insights</h1>
          <p className="page-description">
            Compare your actual tracked time with your weekly recommendations
            and identify areas that may need attention.
          </p>
        </div>
      </section>

      {/* Overview */}
      <section className="life-areas-section">
        <div className="section-header">
          <h2>Overview</h2>
        </div>

        <div className="life-areas-grid">
          <article className="life-area-card">
            <div className="life-area-card-header">
              <div>
                <h3>Recommended</h3>
                <p className="life-area-description">
                  Total recommended time for this week.
                </p>
              </div>
            </div>

            <div className="life-area-details">
              <div className="life-area-detail">
                <span className="detail-label">Hours</span>
                <strong>
                  {formatHours(dashboard.totalRecommendedMinutes)}
                </strong>
              </div>
            </div>
          </article>

          <article className="life-area-card">
            <div className="life-area-card-header">
              <div>
                <h3>Actual</h3>
                <p className="life-area-description">
                  Total time recorded this week.
                </p>
              </div>
            </div>

            <div className="life-area-details">
              <div className="life-area-detail">
                <span className="detail-label">Hours</span>
                <strong>{formatHours(dashboard.totalActualMinutes)}</strong>
              </div>
            </div>
          </article>

          <article className="life-area-card">
            <div className="life-area-card-header">
              <div>
                <h3>Overall Utilization</h3>
                <p className="life-area-description">
                  Actual time compared with recommended time.
                </p>
              </div>
            </div>

            <div className="life-area-details">
              <div className="life-area-detail">
                <span className="detail-label">Utilization</span>
                <strong>{formatUtilization(overallUtilization)}</strong>
              </div>
            </div>
          </article>
        </div>
      </section>

      {/* Life Area Breakdown */}
      <section className="life-areas-section">
        <div className="section-header">
          <div>
            <h2>Life Area Breakdown</h2>
            <p className="page-description">
              See how your actual time compares with the recommendation for each
              life area.
            </p>
          </div>
        </div>

        {dashboard.lifeAreas.length === 0 ? (
          <div className="empty-state">
            <p>No life-area data is available for this week.</p>
          </div>
        ) : (
          <div className="life-areas-grid">
            {dashboard.lifeAreas.map((area) => {
              const neglect = neglectMap.get(area.lifeAreaId);

              const utilization =
                area.recommendedMinutes > 0
                  ? area.actualMinutes / area.recommendedMinutes
                  : 0;

              const difference = area.actualMinutes - area.recommendedMinutes;

              const currentWeekStatus = getCurrentWeekStatus(
                area.recommendedMinutes,
                area.actualMinutes,
              );

              return (
                <article className="life-area-card" key={area.lifeAreaId}>
                  <div className="life-area-card-header">
                    <div>
                      <h3>{area.lifeAreaName}</h3>
                      <p className="life-area-description">
                        Current week comparison
                      </p>
                    </div>

                    {neglect && (
                      <span className="status-badge">{neglect.level}</span>
                    )}
                  </div>

                  <div className="life-area-details">
                    <div className="life-area-detail">
                      <span className="detail-label">Recommended</span>
                      <strong>{formatHours(area.recommendedMinutes)}</strong>
                    </div>

                    <div className="life-area-detail">
                      <span className="detail-label">Actual</span>
                      <strong>{formatHours(area.actualMinutes)}</strong>
                    </div>

                    <div className="life-area-detail">
                      <span className="detail-label">Difference</span>
                      <strong>{formatHours(difference)}</strong>
                    </div>

                    <div className="life-area-detail">
                      <span className="detail-label">Utilization</span>
                      <strong>{formatUtilization(utilization)}</strong>
                    </div>
                  </div>

                  <p className="life-area-description">
                    Current week: {currentWeekStatus}
                  </p>

                  {neglect && (
                    <p className="life-area-description">
                      Historical status: {neglect.level}. Below target for{" "}
                      {neglect.consecutiveUnderTargetWeeks} consecutive week
                      {neglect.consecutiveUnderTargetWeeks === 1 ? "" : "s"}.
                    </p>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </section>

      {/* Neglected Areas */}
      <section className="life-areas-section">
        <div className="section-header">
          <div>
            <h2>Neglected Areas</h2>
            <p className="page-description">
              Areas that satisfy the backend's historical neglect rule.
            </p>
          </div>
        </div>

        {neglectedAreas.length === 0 ? (
          <div className="empty-state">
            <p>No life areas currently meet the historical neglect rule.</p>
          </div>
        ) : (
          <div className="life-areas-grid">
            {neglectedAreas.map((item) => {
              const area = dashboard.lifeAreas.find(
                (lifeArea) => lifeArea.lifeAreaId === item.lifeAreaId,
              );

              return (
                <article className="life-area-card" key={item.lifeAreaId}>
                  <div className="life-area-card-header">
                    <div>
                      <h3>
                        {area?.lifeAreaName ?? `Life Area #${item.lifeAreaId}`}
                      </h3>

                      <p className="life-area-description">
                        Historical neglect detected
                      </p>
                    </div>

                    <span className="status-badge">{item.level}</span>
                  </div>

                  <div className="life-area-details">
                    <div className="life-area-detail">
                      <span className="detail-label">Utilization</span>
                      <strong>{formatUtilization(item.utilization)}</strong>
                    </div>

                    <div className="life-area-detail">
                      <span className="detail-label">Consecutive Weeks</span>
                      <strong>{item.consecutiveUnderTargetWeeks}</strong>
                    </div>
                  </div>

                  <p className="life-area-description">
                    This area has been below 80% of its recommendation for{" "}
                    {item.consecutiveUnderTargetWeeks} consecutive week
                    {item.consecutiveUnderTargetWeeks === 1 ? "" : "s"}.
                  </p>
                </article>
              );
            })}
          </div>
        )}
      </section>
    </main>
  );
}

export default InsightsPage;
