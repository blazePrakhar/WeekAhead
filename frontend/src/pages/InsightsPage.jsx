import { useEffect, useMemo, useState } from "react";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import { getWeeklyDashboard } from "../api/dashboardApi";
import { getNeglectInsights } from "../api/neglectApi";
import { getAnalytics } from "../api/analyticsApi";
import { generateWeeklyInsight } from "../api/aiInsightApi";

const formatHours = (minutes) => {
  const value = Number(minutes ?? 0);

  if (!Number.isFinite(value)) {
    return "0h";
  }

  const hours = value / 60;

  return `${Number.isInteger(hours) ? hours : hours.toFixed(1)}h`;
};

const formatPercentage = (value) => {
  const number = Number(value);

  if (!Number.isFinite(number)) {
    return "0%";
  }

  return `${number.toFixed(0)}%`;
};

const calculateUtilization = (actualMinutes, recommendedMinutes) => {
  const actual = Number(actualMinutes ?? 0);
  const recommended = Number(recommendedMinutes ?? 0);

  if (!Number.isFinite(actual) || !Number.isFinite(recommended)) {
    return 0;
  }

  if (recommended <= 0) {
    return 0;
  }

  return (actual / recommended) * 100;
};

const getDifferenceMinutes = (recommendedMinutes, actualMinutes) => {
  const recommended = Number(recommendedMinutes ?? 0);
  const actual = Number(actualMinutes ?? 0);

  if (!Number.isFinite(recommended) || !Number.isFinite(actual)) {
    return 0;
  }

  return actual - recommended;
};

const getUtilizationClass = (utilization) => {
  if (utilization >= 100) {
    return "dashboard-status dashboard-status-success";
  }

  if (utilization >= 80) {
    return "dashboard-status dashboard-status-warning";
  }

  return "dashboard-status dashboard-status-danger";
};

function InsightsPage() {
  const [dashboard, setDashboard] = useState(null);
  const [neglectData, setNeglectData] = useState([]);
  const [dashboardLoading, setDashboardLoading] = useState(true);
  const [neglectLoading, setNeglectLoading] = useState(true);
  const [dashboardError, setDashboardError] = useState("");
  const [neglectError, setNeglectError] = useState("");

  const [analytics, setAnalytics] = useState(null);
  const [analyticsLoading, setAnalyticsLoading] = useState(true);
  const [analyticsError, setAnalyticsError] = useState("");

  const [aiInsight, setAiInsight] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState("");

  useEffect(() => {
    let mounted = true;

    const loadDashboard = async () => {
      setDashboardLoading(true);
      setDashboardError("");

      try {
        const response = await getWeeklyDashboard();

        if (mounted) {
          setDashboard(response?.data ?? response);
        }
      } catch (error) {
        if (mounted) {
          setDashboardError(
            error?.response?.data?.message ||
              "Unable to load the weekly dashboard.",
          );
        }
      } finally {
        if (mounted) {
          setDashboardLoading(false);
        }
      }
    };

    const loadNeglect = async () => {
      setNeglectLoading(true);
      setNeglectError("");

      try {
        const response = await getNeglectInsights();

        if (mounted) {
          const data = response?.data ?? response;

          setNeglectData(Array.isArray(data) ? data : []);
        }
      } catch (error) {
        if (mounted) {
          setNeglectError(
            error?.response?.data?.message ||
              "Unable to load neglected-area data.",
          );
        }
      } finally {
        if (mounted) {
          setNeglectLoading(false);
        }
      }
    };

    const loadAnalytics = async () => {
      setAnalyticsLoading(true);
      setAnalyticsError("");

      try {
        const response = await getAnalytics(12);

        if (mounted) {
          setAnalytics(response ?? null);
        }
      } catch (error) {
        if (mounted) {
          setAnalyticsError(
            error?.response?.data?.message ||
              "Unable to load historical analytics.",
          );
        }
      } finally {
        if (mounted) {
          setAnalyticsLoading(false);
        }
      }
    };

    loadDashboard();
    loadNeglect();
    loadAnalytics();

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

  const utilizationTrendData = useMemo(() => {
    const weeklyTrends = analytics?.weeklyTrends;

    if (!Array.isArray(weeklyTrends)) {
      return [];
    }

    return weeklyTrends
      .filter(
        (week) =>
          week && week.weekStartDate && Number(week.recommendedMinutes) > 0,
      )
      .map((week) => {
        const recommendedMinutes = Number(week.recommendedMinutes);
        const actualMinutes = Number(week.actualMinutes ?? 0);

        const utilization = (actualMinutes / recommendedMinutes) * 100;

        return {
          weekStartDate: week.weekStartDate,
          label: new Date(`${week.weekStartDate}T00:00:00`).toLocaleDateString(
            "en-US",
            {
              month: "short",
              day: "numeric",
            },
          ),
          utilization: Number(utilization.toFixed(1)),
        };
      });
  }, [analytics]);

  const recommendedActualTrendData = useMemo(() => {
    const weeklyTrends = analytics?.weeklyTrends;

    if (!Array.isArray(weeklyTrends)) {
      return [];
    }

    return weeklyTrends
      .filter((week) => week && week.weekStartDate)
      .map((week) => ({
        weekStartDate: week.weekStartDate,
        label: new Date(`${week.weekStartDate}T00:00:00`).toLocaleDateString(
          "en-US",
          {
            month: "short",
            day: "numeric",
          },
        ),
        recommendedHours: Number(
          ((week.recommendedMinutes ?? 0) / 60).toFixed(1),
        ),
        actualHours: Number(((week.actualMinutes ?? 0) / 60).toFixed(1)),
      }));
  }, [analytics]);

  const rebalancingSuggestions = useMemo(() => {
    if (!dashboard?.rebalancingSuggestions) {
      return [];
    }

    return Array.isArray(dashboard.rebalancingSuggestions)
      ? dashboard.rebalancingSuggestions
      : [];
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

  const handleGenerateInsight = async () => {
    if (!dashboard?.weekId) {
      setAiError("No current week is available.");
      return;
    }

    setAiLoading(true);
    setAiError("");

    try {
      const response = await generateWeeklyInsight(dashboard.weekId);

      let parsedInsight = response?.insight;

      if (typeof parsedInsight === "string") {
        parsedInsight = JSON.parse(parsedInsight);
      }

      setAiInsight(parsedInsight);
    } catch (requestError) {
      console.error("Failed to generate AI weekly insight:", requestError);

      setAiInsight(null);
      setAiError(
        requestError?.response?.data?.message ||
          "Unable to generate the weekly insight.",
      );
    } finally {
      setAiLoading(false);
    }
  };

  if (dashboardLoading) {
    return (
      <main className="dashboard-page">
        <div className="dashboard-container">
          <div className="dashboard-page-header">
            <div>
              <h1>Weekly Dashboard</h1>
              <p>Loading your current week...</p>
            </div>
          </div>

          <div className="dashboard-loading">Loading dashboard data...</div>
        </div>
      </main>
    );
  }

  if (dashboardError) {
    return (
      <main className="dashboard-page">
        <div className="dashboard-container">
          <div className="dashboard-page-header">
            <div>
              <h1>Weekly Dashboard</h1>
              <p>Overview of your current week.</p>
            </div>
          </div>

          <div className="dashboard-error">
            <h2>Unable to load dashboard</h2>
            <p>{dashboardError}</p>
          </div>
        </div>
      </main>
    );
  }

  if (!dashboard) {
    return (
      <main className="dashboard-page">
        <div className="dashboard-container">
          <div className="dashboard-page-header">
            <div>
              <h1>Weekly Dashboard</h1>
              <p>Overview of your current week.</p>
            </div>
          </div>

          <div className="dashboard-empty">
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
    <main className="dashboard-page">
      <div className="dashboard-container">
        <header className="dashboard-page-header">
          <div>
            <p className="dashboard-eyebrow">WeekAhead</p>
            <h1>Weekly Dashboard</h1>

            <p>
              {dashboard.weekStartDate
                ? `Week starting ${dashboard.weekStartDate}`
                : "Overview of your current week."}
            </p>
          </div>
        </header>

        {/* Weekly Overview */}
        <section className="dashboard-section">
          <div className="dashboard-section-header">
            <div>
              <h2>Weekly Overview</h2>
              <p>A quick view of your available and used time.</p>
            </div>
          </div>

          <div className="dashboard-summary-grid">
            <article className="dashboard-summary-card">
              <span>Available</span>
              <strong>{formatHours(totalAvailableMinutes)}</strong>
            </article>

            <article className="dashboard-summary-card">
              <span>Recommended</span>
              <strong>{formatHours(totalRecommendedMinutes)}</strong>
            </article>

            <article className="dashboard-summary-card">
              <span>Tracked</span>
              <strong>{formatHours(totalActualMinutes)}</strong>
            </article>

            <article className="dashboard-summary-card">
              <span>Remaining</span>
              <strong>{formatHours(remainingMinutes)}</strong>
            </article>

            <article className="dashboard-summary-card">
              <span>Utilization</span>
              <strong>{formatPercentage(overallUtilization)}</strong>
            </article>
          </div>
        </section>

        {/* Life Area Breakdown */}
        <section className="dashboard-section">
          <div className="dashboard-section-header">
            <div>
              <h2>Life Area Breakdown</h2>
              <p>
                Compare recommended time with the time you have actually
                tracked.
              </p>
            </div>
          </div>

          {lifeAreas.length === 0 ? (
            <div className="dashboard-empty">
              <h3>No life-area data</h3>
              <p>
                There are no life-area allocation records for this week yet.
              </p>
            </div>
          ) : (
            <div className="dashboard-table-wrapper">
              <table className="dashboard-table">
                <thead>
                  <tr>
                    <th>Life Area</th>
                    <th>Recommended</th>
                    <th>Actual</th>
                    <th>Difference</th>
                    <th>Utilization</th>
                  </tr>
                </thead>

                <tbody>
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
                      <tr key={area.lifeAreaId}>
                        <td>
                          <strong>{area.lifeAreaName}</strong>
                        </td>

                        <td>{formatHours(area.recommendedMinutes)}</td>

                        <td>{formatHours(area.actualMinutes)}</td>

                        <td>
                          <span
                            className={
                              difference >= 0
                                ? "dashboard-difference positive"
                                : "dashboard-difference negative"
                            }
                          >
                            {difference >= 0 ? "+" : ""}
                            {formatHours(Math.abs(difference))}
                          </span>
                        </td>

                        <td>
                          <span className={getUtilizationClass(utilization)}>
                            {formatPercentage(utilization)}
                          </span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* Neglected Areas */}
        <section className="dashboard-section">
          <div className="dashboard-section-header">
            <div>
              <h2>Neglected Areas</h2>
              <p>Areas identified by the existing neglect detection logic.</p>
            </div>
          </div>

          {neglectLoading ? (
            <div className="dashboard-loading">Loading neglected areas...</div>
          ) : neglectError ? (
            <div className="dashboard-error">
              <p>{neglectError}</p>
            </div>
          ) : neglectData.length === 0 ? (
            <div className="dashboard-empty dashboard-empty-positive">
              <h3>Everything is on track</h3>
              <p>
                No neglected life areas were reported for the current analysis
                period.
              </p>
            </div>
          ) : (
            <div className="dashboard-card-grid">
              {neglectData.map((item) => (
                <article className="dashboard-info-card" key={item.lifeAreaId}>
                  <div className="dashboard-info-card-header">
                    <h3>
                      {item.lifeAreaName || `Life Area ${item.lifeAreaId}`}
                    </h3>

                    {item.level && (
                      <span className="dashboard-severity">{item.level}</span>
                    )}
                  </div>

                  <div className="dashboard-info-card-stats">
                    <div>
                      <span>Utilization</span>
                      <strong>
                        {formatPercentage(Number(item.utilization ?? 0) * 100)}
                      </strong>
                    </div>

                    <div>
                      <span>Under target</span>
                      <strong>{item.consecutiveUnderTargetWeeks ?? 0}</strong>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        {/* Rebalancing Suggestions */}
        <section className="dashboard-section">
          <div className="dashboard-section-header">
            <div>
              <h2>Rebalancing Suggestions</h2>
              <p>
                These are suggestions for adjusting the remaining time in your
                week. They do not change your plan automatically.
              </p>
            </div>
          </div>

          {rebalancingSuggestions.length === 0 ? (
            <div className="dashboard-empty">
              <h3>No rebalancing suggestions</h3>
              <p>
                There are no current suggestions for shifting time between life
                areas.
              </p>
            </div>
          ) : (
            <div className="dashboard-card-grid">
              {rebalancingSuggestions.map((suggestion, index) => (
                <article
                  className="dashboard-info-card"
                  key={
                    suggestion.id ??
                    `${suggestion.sourceLifeAreaId}-${suggestion.destinationLifeAreaId}-${index}`
                  }
                >
                  <div className="dashboard-rebalance-route">
                    <strong>
                      {suggestion.sourceLifeAreaName ??
                        `Area ${suggestion.sourceLifeAreaId}`}
                    </strong>

                    <span>→</span>

                    <strong>
                      {suggestion.destinationLifeAreaName ??
                        `Area ${suggestion.destinationLifeAreaId}`}
                    </strong>
                  </div>

                  <p className="dashboard-transfer">
                    Potential transfer:{" "}
                    <strong>
                      {formatHours(suggestion.transferableMinutes)}
                    </strong>
                  </p>

                  <p className="dashboard-reason">
                    {suggestion.explanation ??
                      suggestion.reason ??
                      "No explanation was provided."}
                  </p>
                </article>
              ))}
            </div>
          )}
        </section>

        {/* Historical Trends */}
        <section className="dashboard-section">
          <div className="dashboard-section-header">
            <div>
              <h2>Historical Trends</h2>
              <p>
                Review how your time usage has changed across previous weeks.
              </p>
            </div>
          </div>

          {analyticsLoading ? (
            <div className="dashboard-loading">
              Loading historical trends...
            </div>
          ) : analyticsError ? (
            <div className="dashboard-error">
              <h3>Unable to load historical trends</h3>
              <p>{analyticsError}</p>
            </div>
          ) : !Array.isArray(analytics?.weeklyTrends) ||
            analytics.weeklyTrends.length === 0 ? (
            <div className="dashboard-empty">
              <h3>No historical data yet</h3>
              <p>
                Historical trends will appear here once completed weeks contain
                tracked activity and recommendation data.
              </p>
            </div>
          ) : (
            <>
              <div className="dashboard-chart-card">
                <div className="dashboard-chart-header">
                  <div>
                    <h3>Weekly Utilization</h3>
                    <p>Actual tracked time compared with recommended time.</p>
                  </div>
                </div>

                <div className="dashboard-chart">
                  <ResponsiveContainer width="100%" height={320}>
                    <LineChart
                      data={utilizationTrendData}
                      margin={{
                        top: 16,
                        right: 20,
                        left: 0,
                        bottom: 8,
                      }}
                    >
                      <CartesianGrid strokeDasharray="3 3" />

                      <XAxis dataKey="label" tick={{ fontSize: 12 }} />

                      <YAxis
                        domain={[0, "auto"]}
                        tickFormatter={(value) => `${value}%`}
                        tick={{ fontSize: 12 }}
                      />

                      <Tooltip
                        formatter={(value) => [`${value}%`, "Utilization"]}
                      />

                      <Line
                        type="monotone"
                        dataKey="utilization"
                        strokeWidth={2}
                        dot={{ r: 4 }}
                        activeDot={{ r: 6 }}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </div>

              <div className="dashboard-chart-card">
                <div className="dashboard-chart-header">
                  <div>
                    <h3>Recommended vs Actual</h3>
                    <p>
                      Compare recommended time with the time you actually
                      tracked each week.
                    </p>
                  </div>
                </div>

                <div className="dashboard-chart">
                  <ResponsiveContainer width="100%" height={320}>
                    <LineChart
                      data={recommendedActualTrendData}
                      margin={{
                        top: 16,
                        right: 20,
                        left: 0,
                        bottom: 8,
                      }}
                    >
                      <CartesianGrid strokeDasharray="3 3" />

                      <XAxis dataKey="label" tick={{ fontSize: 12 }} />

                      <YAxis
                        tickFormatter={(value) => `${value}h`}
                        tick={{ fontSize: 12 }}
                      />

                      <Tooltip
                        formatter={(value, name) => [
                          `${value}h`,
                          name === "recommendedHours"
                            ? "Recommended"
                            : "Actual",
                        ]}
                      />

                      <Line
                        type="monotone"
                        dataKey="recommendedHours"
                        strokeWidth={2}
                        dot={{ r: 4 }}
                        activeDot={{ r: 6 }}
                      />

                      <Line
                        type="monotone"
                        dataKey="actualHours"
                        strokeWidth={2}
                        dot={{ r: 4 }}
                        activeDot={{ r: 6 }}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </div>
            </>
          )}
        </section>

        {/* AI Weekly Insight */}
        <section className="dashboard-section">
          <div className="dashboard-section-header">
            <div>
              <h2>AI Weekly Insight</h2>
              <p>
                Get a natural-language summary of your current week based on the
                backend's weekly data.
              </p>
            </div>

            <button
              type="button"
              className="dashboard-primary-button"
              onClick={handleGenerateInsight}
              disabled={aiLoading}
            >
              {aiLoading ? "Generating Insight..." : "Generate Insight"}
            </button>
          </div>

          {aiLoading ? (
            <div className="dashboard-loading">
              Generating your weekly insight...
            </div>
          ) : aiError ? (
            <div className="dashboard-error">
              <h3>Unable to generate insight</h3>
              <p>{aiError}</p>
            </div>
          ) : !aiInsight ? (
            <div className="dashboard-empty">
              <h3>No insight generated yet</h3>
              <p>
                Generate an AI weekly insight to receive a concise summary,
                observations, and suggested actions.
              </p>
            </div>
          ) : (
            <div className="dashboard-ai-card">
              {aiInsight.summary && (
                <div className="dashboard-ai-summary">
                  <h3>Summary</h3>
                  <p>{aiInsight.summary}</p>
                </div>
              )}

              {Array.isArray(aiInsight.observations) &&
                aiInsight.observations.length > 0 && (
                  <div className="dashboard-ai-section">
                    <h3>Observations</h3>

                    <ul>
                      {aiInsight.observations.map((observation, index) => (
                        <li key={index}>{observation}</li>
                      ))}
                    </ul>
                  </div>
                )}

              {Array.isArray(aiInsight.actions) &&
                aiInsight.actions.length > 0 && (
                  <div className="dashboard-ai-section">
                    <h3>Suggested Actions</h3>

                    <ul>
                      {aiInsight.actions.map((action, index) => (
                        <li key={index}>{action}</li>
                      ))}
                    </ul>
                  </div>
                )}
            </div>
          )}
        </section>
      </div>
    </main>
  );
}

export default InsightsPage;
