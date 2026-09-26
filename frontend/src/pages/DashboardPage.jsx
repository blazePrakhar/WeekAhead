import { useEffect, useMemo, useState } from "react";

import { getWeeklyDashboard } from "../api/dashboardApi";
import { generateWeeklyInsight } from "../api/aiInsightApi";

const formatHours = (minutes) => {
  const value = Number(minutes ?? 0);

  if (!Number.isFinite(value)) {
    return "0h";
  }

  const hours = value / 60;

  return `${Number.isInteger(hours) ? hours : hours.toFixed(1)}h`;
};

const calculateUtilization = (actualMinutes, recommendedMinutes) => {
  const actual = Number(actualMinutes ?? 0);
  const recommended = Number(recommendedMinutes ?? 0);

  if (
    !Number.isFinite(actual) ||
    !Number.isFinite(recommended) ||
    recommended <= 0
  ) {
    return 0;
  }

  return (actual / recommended) * 100;
};

const getAreaDifference = (recommendedMinutes, actualMinutes) => {
  const recommended = Number(recommendedMinutes ?? 0);
  const actual = Number(actualMinutes ?? 0);

  if (!Number.isFinite(recommended) || !Number.isFinite(actual)) {
    return 0;
  }

  return recommended - actual;
};

const getAttentionLevel = (area) => {
  if (area.neglected || Number(area.deficitMinutes ?? 0) > 0) {
    return "Attention needed";
  }

  if (Number(area.overflowMinutes ?? 0) > 0) {
    return "Above recommendation";
  }

  return "On track";
};

const parseAIInsight = (insight) => {
  if (!insight) {
    return null;
  }

  if (typeof insight === "object") {
    return insight;
  }

  try {
    return JSON.parse(insight);
  } catch {
    return {
      summary: insight,
      observations: [],
      actions: [],
    };
  }
};

function DashboardPage() {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [aiInsight, setAiInsight] = useState("");
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState("");

  const parsedAIInsight = parseAIInsight(aiInsight);

  useEffect(() => {
    let mounted = true;

    async function loadDashboard() {
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
    }

    loadDashboard();

    return () => {
      mounted = false;
    };
  }, []);

  const lifeAreas = useMemo(() => {
    if (!Array.isArray(dashboard?.lifeAreas)) {
      return [];
    }

    return dashboard.lifeAreas;
  }, [dashboard]);

  const attentionAreas = useMemo(() => {
    return lifeAreas.filter(
      (area) =>
        area?.neglected ||
        Number(area?.deficitMinutes ?? 0) > 0 ||
        Number(area?.overflowMinutes ?? 0) > 0,
    );
  }, [lifeAreas]);

  const availableMinutes = Number(dashboard?.availableMinutes ?? 0);
  const recommendedMinutes = Number(dashboard?.totalRecommendedMinutes ?? 0);
  const plannedMinutes = Number(dashboard?.totalPlannedMinutes ?? 0);
  const actualMinutes = Number(dashboard?.totalActualMinutes ?? 0);

  const remainingMinutes = Math.max(0, availableMinutes - plannedMinutes);

  const utilization = calculateUtilization(actualMinutes, recommendedMinutes);

  const plannedPercentage =
    availableMinutes > 0
      ? Math.min(100, (plannedMinutes / availableMinutes) * 100)
      : 0;

  const areaTotalRecommended = lifeAreas.reduce(
    (total, area) => total + Number(area.recommendedMinutes ?? 0),
    0,
  );

  const distributionItems = lifeAreas.map((area) => {
    const minutes = Number(area.recommendedMinutes ?? 0);

    const percentage =
      areaTotalRecommended > 0 ? (minutes / areaTotalRecommended) * 100 : 0;

    return {
      id: area.lifeAreaId,
      name: area.lifeAreaName,
      minutes,
      percentage,
    };
  });

  const handleGenerateInsight = async () => {
    if (!dashboard?.weekId) {
      setAiError("No current week is available for the insight.");
      return;
    }

    setAiLoading(true);
    setAiError("");

    try {
      const response = await generateWeeklyInsight(dashboard.weekId);

      setAiInsight(response?.insight ?? "");
    } catch (requestError) {
      setAiError(
        requestError?.response?.data?.message ||
          "Unable to generate the weekly insight.",
      );
    } finally {
      setAiLoading(false);
    }
  };

  if (loading) {
    return (
      <main className="wa-dashboard-page">
        <div className="wa-dashboard-container">
          <div className="wa-dashboard-state">
            <span className="wa-dashboard-state-dot" />
            Loading your weekly dashboard...
          </div>
        </div>
      </main>
    );
  }

  if (error) {
    return (
      <main className="wa-dashboard-page">
        <div className="wa-dashboard-container">
          <section className="wa-dashboard-state wa-dashboard-state-error">
            <h1>Unable to load dashboard</h1>
            <p>{error}</p>
          </section>
        </div>
      </main>
    );
  }

  if (!dashboard) {
    return (
      <main className="wa-dashboard-page">
        <div className="wa-dashboard-container">
          <section className="wa-dashboard-state">
            <h1>No dashboard data</h1>
            <p>Configure your current week before viewing the dashboard.</p>
          </section>
        </div>
      </main>
    );
  }

  return (
    <main className="wa-dashboard-page">
      <div className="wa-dashboard-container">
        {/* Page Header */}
        <header className="wa-dashboard-header">
          <div>
            <div className="wa-dashboard-week-line">
              <span className="wa-dashboard-status-dot" />

              <span>
                {dashboard.weekStartDate
                  ? `Week starting ${dashboard.weekStartDate}`
                  : "Current week"}
              </span>
            </div>

            <h1>Good morning</h1>

            <p>
              Here is your weekly time overview across available hours, planned
              allocations, and tracked time.
            </p>
          </div>
        </header>

        {/* Weekly Summary */}
        <section className="wa-dashboard-section">
          <div className="wa-dashboard-section-heading">
            <div>
              <h2>Weekly Summary &amp; Capacity</h2>

              <p>
                A quick view of how your available time is being planned and
                used.
              </p>
            </div>

            <span className="wa-dashboard-capacity-badge">
              {utilization >= 80 ? "Focused flow" : "Comfortable flow"}
            </span>
          </div>

          <div className="wa-dashboard-summary-grid">
            <article className="wa-dashboard-summary-card">
              <span className="wa-dashboard-card-label">
                Available this week
              </span>

              <strong>{formatHours(availableMinutes)}</strong>

              <small>From Weekly Availability</small>
            </article>

            <article className="wa-dashboard-summary-card">
              <span className="wa-dashboard-card-label">
                Planned allocation
              </span>

              <strong>{formatHours(plannedMinutes)}</strong>

              <small>{plannedPercentage.toFixed(1)}% of available time</small>
            </article>

            <article className="wa-dashboard-summary-card wa-dashboard-summary-card-accent">
              <span className="wa-dashboard-card-label">Logged time</span>

              <strong>{formatHours(actualMinutes)}</strong>

              <small>{utilization.toFixed(1)}% pace so far</small>
            </article>

            <article className="wa-dashboard-summary-card">
              <span className="wa-dashboard-card-label">Remaining buffer</span>

              <strong>{formatHours(remainingMinutes)}</strong>

              <small>Unassigned available space</small>
            </article>
          </div>

          {/* Distribution */}
          <div className="wa-dashboard-distribution">
            <div className="wa-dashboard-distribution-header">
              <span>Recommended Time Distribution</span>

              <span>
                {formatHours(areaTotalRecommended)} recommended across active
                areas
              </span>
            </div>

            <div className="wa-dashboard-distribution-bar">
              {distributionItems.map((item) => (
                <span
                  key={item.id}
                  className="wa-dashboard-distribution-segment"
                  style={{
                    width: `${item.percentage}%`,
                  }}
                  title={`${item.name}: ${formatHours(item.minutes)}`}
                />
              ))}

              {availableMinutes > plannedMinutes && (
                <span
                  className="wa-dashboard-distribution-buffer"
                  style={{
                    width: `${
                      availableMinutes > 0
                        ? ((availableMinutes - plannedMinutes) /
                            availableMinutes) *
                          100
                        : 0
                    }%`,
                  }}
                  title="Flexible buffer"
                />
              )}
            </div>

            <div className="wa-dashboard-distribution-legend">
              {distributionItems.map((item) => (
                <div key={item.id} className="wa-dashboard-legend-item">
                  <span className="wa-dashboard-legend-dot" />

                  <span>
                    {item.name} {formatHours(item.minutes)}
                  </span>
                </div>
              ))}

              <div className="wa-dashboard-legend-item">
                <span className="wa-dashboard-legend-dot wa-dashboard-legend-dot-buffer" />

                <span>Available {formatHours(availableMinutes)}</span>
              </div>
            </div>
          </div>
        </section>

        {/* Life Areas */}
        <section className="wa-dashboard-section">
          <div className="wa-dashboard-section-heading">
            <div>
              <h2>Life Area Overview</h2>

              <p>Tracked time compared with recommended weekly time.</p>
            </div>

            <span className="wa-dashboard-section-count">
              {lifeAreas.length} active{" "}
              {lifeAreas.length === 1 ? "area" : "areas"}
            </span>
          </div>

          {lifeAreas.length === 0 ? (
            <div className="wa-dashboard-empty">
              <h3>No life-area data</h3>

              <p>
                There are no life-area allocation records for this week yet.
              </p>
            </div>
          ) : (
            <div className="wa-dashboard-life-area-list">
              {lifeAreas.map((area) => {
                const recommended = Number(area.recommendedMinutes ?? 0);

                const actual = Number(area.actualMinutes ?? 0);

                const areaUtilization = calculateUtilization(
                  actual,
                  recommended,
                );

                const difference = getAreaDifference(recommended, actual);

                const progressWidth = Math.min(100, areaUtilization);

                return (
                  <article
                    key={area.lifeAreaId}
                    className="wa-dashboard-life-area"
                  >
                    <div className="wa-dashboard-life-area-top">
                      <div>
                        <h3>{area.lifeAreaName}</h3>

                        <p>
                          {formatHours(actual)} logged of{" "}
                          {formatHours(recommended)} recommended
                        </p>
                      </div>

                      <div className="wa-dashboard-life-area-value">
                        <strong>{formatHours(recommended)}</strong>

                        <span>recommended</span>
                      </div>
                    </div>

                    <div className="wa-dashboard-progress-track">
                      <span
                        className="wa-dashboard-progress-value"
                        style={{
                          width: `${progressWidth}%`,
                        }}
                      />
                    </div>

                    <div className="wa-dashboard-life-area-bottom">
                      <span>{areaUtilization.toFixed(0)}% utilized</span>

                      <span
                        className={
                          difference > 0
                            ? "wa-dashboard-difference-low"
                            : "wa-dashboard-difference-balanced"
                        }
                      >
                        {difference > 0
                          ? `${formatHours(difference)} remaining`
                          : "On track"}
                      </span>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </section>

        {/* Dashboard Secondary Content */}
        <section className="wa-dashboard-secondary-grid">
          {/* Weekly Insight */}
          <section className="wa-dashboard-section wa-dashboard-insight-section">
            <div className="wa-dashboard-section-heading">
              <div>
                <h2>Weekly Insight</h2>

                <p>
                  AI-generated commentary based on the authoritative weekly
                  backend data.
                </p>
              </div>

              <span className="wa-dashboard-ai-badge">AI Weekly Insight</span>
            </div>

            {!aiInsight && !aiError && (
              <div className="wa-dashboard-insight-empty">
                <p>
                  Generate an insight from this week's allocation, tracked time,
                  and life-area data.
                </p>

                <button
                  type="button"
                  className="wa-dashboard-primary-button"
                  onClick={handleGenerateInsight}
                  disabled={aiLoading}
                >
                  {aiLoading
                    ? "Generating insight..."
                    : "Generate weekly insight"}
                </button>
              </div>
            )}

            {aiLoading && aiInsight && (
              <div className="wa-dashboard-insight-loading">
                Updating weekly insight...
              </div>
            )}

            {aiError && (
              <div className="wa-dashboard-insight-error">
                <strong>Insight unavailable</strong>
                <p>{aiError}</p>
              </div>
            )}

            {aiInsight && !aiLoading && (
              <article className="wa-dashboard-insight-card">
                <div className="wa-dashboard-insight-card-header">
                  <span className="wa-dashboard-insight-dot" />

                  <span>Current week</span>
                </div>

                {parsedAIInsight && (
                  <div className="wa-dashboard-insight-content">
                    {parsedAIInsight.summary && (
                      <div className="wa-dashboard-insight-summary">
                        <h4>Summary</h4>

                        <p>{parsedAIInsight.summary}</p>
                      </div>
                    )}

                    {parsedAIInsight.observations?.length > 0 && (
                      <div className="wa-dashboard-insight-section">
                        <h4>Observations</h4>

                        <ul>
                          {parsedAIInsight.observations.map(
                            (observation, index) => (
                              <li key={`observation-${index}`}>
                                {observation}
                              </li>
                            ),
                          )}
                        </ul>
                      </div>
                    )}

                    {parsedAIInsight.actions?.length > 0 && (
                      <div className="wa-dashboard-insight-section">
                        <h4>Suggested actions</h4>

                        <ul>
                          {parsedAIInsight.actions.map((action, index) => (
                            <li key={`action-${index}`}>{action}</li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </div>
                )}

                <button
                  type="button"
                  className="wa-dashboard-secondary-button"
                  onClick={handleGenerateInsight}
                  disabled={aiLoading}
                >
                  Regenerate insight
                </button>
              </article>
            )}
          </section>

          {/* Areas Needing Attention */}
          <section className="wa-dashboard-section wa-dashboard-attention-section">
            <div className="wa-dashboard-section-heading">
              <div>
                <h2>Areas Needing Attention</h2>

                <p>
                  Life areas with deficits, overflow, or neglect indicators from
                  the backend.
                </p>
              </div>

              <span className="wa-dashboard-section-count">
                {attentionAreas.length}
              </span>
            </div>

            {attentionAreas.length === 0 ? (
              <div className="wa-dashboard-attention-empty">
                <span className="wa-dashboard-success-icon">✓</span>

                <div>
                  <h3>Everything is on track</h3>

                  <p>No life areas currently require additional attention.</p>
                </div>
              </div>
            ) : (
              <div className="wa-dashboard-attention-list">
                {attentionAreas.map((area) => {
                  const deficit = Number(area.deficitMinutes ?? 0);

                  const overflow = Number(area.overflowMinutes ?? 0);

                  const level = getAttentionLevel(area);

                  return (
                    <article
                      key={area.lifeAreaId}
                      className="wa-dashboard-attention-card"
                    >
                      <div className="wa-dashboard-attention-card-header">
                        <div>
                          <h3>{area.lifeAreaName}</h3>

                          <span
                            className={
                              area.neglected
                                ? "wa-dashboard-alert-badge"
                                : "wa-dashboard-neutral-badge"
                            }
                          >
                            {level}
                          </span>
                        </div>
                      </div>

                      <div className="wa-dashboard-attention-details">
                        {deficit > 0 && (
                          <span>
                            {formatHours(deficit)} below recommendation
                          </span>
                        )}

                        {overflow > 0 && (
                          <span>
                            {formatHours(overflow)} above recommendation
                          </span>
                        )}

                        {area.neglected && (
                          <span>Neglect detected by current analysis</span>
                        )}
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </section>
        </section>
      </div>
    </main>
  );
}

export default DashboardPage;
