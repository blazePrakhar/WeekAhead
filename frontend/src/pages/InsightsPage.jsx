import { useEffect, useMemo, useState } from "react";
import {
  Activity,
  AlertTriangle,
  ArrowLeftRight,
  BarChart3,
  BrainCircuit,
  CalendarDays,
  CheckCircle2,
  Clock3,
  Lightbulb,
  Sparkles,
  TrendingUp,
} from "lucide-react";
import { motion } from "motion/react";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import { getAnalytics } from "../api/analyticsApi";
import { generateWeeklyInsight } from "../api/aiInsightApi";
import { getNeglectInsights } from "../api/neglectApi";
import { getRebalancingSuggestions } from "../api/rebalancingApi";
import { getCurrentWeek } from "../api/weekApi";

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

function InsightsPage() {
  const [currentWeek, setCurrentWeek] = useState(null);

  const [neglectData, setNeglectData] = useState([]);
  const [neglectLoading, setNeglectLoading] = useState(true);
  const [neglectError, setNeglectError] = useState("");

  const [rebalancingSuggestions, setRebalancingSuggestions] = useState([]);
  const [rebalancingLoading, setRebalancingLoading] = useState(true);
  const [rebalancingError, setRebalancingError] = useState("");

  const [analytics, setAnalytics] = useState(null);
  const [analyticsLoading, setAnalyticsLoading] = useState(true);
  const [analyticsError, setAnalyticsError] = useState("");

  const [aiInsight, setAiInsight] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState("");

  useEffect(() => {
    let mounted = true;

    const loadCurrentWeek = async () => {
      try {
        const response = await getCurrentWeek();

        if (mounted) {
          setCurrentWeek(response);
        }
      } catch (error) {
        if (mounted) {
          console.error("Failed to load current week:", error);
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

    const loadRebalancing = async () => {
      setRebalancingLoading(true);
      setRebalancingError("");

      try {
        const response = await getRebalancingSuggestions();

        if (mounted) {
          const data = response?.data ?? response;

          setRebalancingSuggestions(Array.isArray(data) ? data : []);
        }
      } catch (error) {
        if (mounted) {
          setRebalancingError(
            error?.response?.data?.message ||
              "Unable to load rebalancing suggestions.",
          );
        }
      } finally {
        if (mounted) {
          setRebalancingLoading(false);
        }
      }
    };

    const loadAnalytics = async () => {
      setAnalyticsLoading(true);
      setAnalyticsError("");

      try {
        const response = await getAnalytics(12);

        if (mounted) {
          setAnalytics(response?.data ?? response ?? null);
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

    loadCurrentWeek();
    loadNeglect();
    loadRebalancing();
    loadAnalytics();

    return () => {
      mounted = false;
    };
  }, []);

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

  const handleGenerateInsight = async () => {
    const weekId = currentWeek?.id ?? currentWeek?.weekId;

    if (!weekId) {
      setAiError("No current week is available.");
      return;
    }

    setAiLoading(true);
    setAiError("");

    try {
      const response = await generateWeeklyInsight(weekId);

      let parsedInsight = response?.insight ?? response?.data?.insight;

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

  return (
    <main className="insights-page">
      <div className="insights-container">
        <motion.header
          className="insights-header"
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{
            duration: 0.28,
            ease: "easeOut",
          }}
        >
          <div>
            <p className="insights-eyebrow">WEEK AHEAD</p>

            <h1>Weekly Insights</h1>

            <p className="insights-header-description">
              Understand patterns in your time, identify neglected areas, and
              review suggestions for the rest of your week.
            </p>
          </div>

          <div className="insights-week-badge">
            <CalendarDays size={16} />

            <span>{currentWeek?.weekStartDate ?? "Current week"}</span>
          </div>
        </motion.header>

        {/* Neglected Areas */}

        <section className="insights-section">
          <div className="insights-section-header">
            <div>
              <p className="insights-section-kicker">ATTENTION</p>

              <h2>Neglected Areas</h2>

              <p>Areas identified by the existing neglect detection logic.</p>
            </div>
          </div>

          {neglectLoading ? (
            <div className="insights-feedback insights-feedback-loading">
              <Clock3 size={20} />

              <span>Loading neglected areas...</span>
            </div>
          ) : neglectError ? (
            <div className="insights-feedback insights-feedback-error">
              <AlertTriangle size={20} />

              <div>
                <strong>Unable to load neglected areas</strong>

                <p>{neglectError}</p>
              </div>
            </div>
          ) : neglectData.length === 0 ? (
            <div className="insights-positive-card">
              <div className="insights-positive-icon">
                <CheckCircle2 size={22} />
              </div>

              <div>
                <h3>Everything is on track</h3>

                <p>
                  No neglected life areas were reported for the current analysis
                  period.
                </p>
              </div>
            </div>
          ) : (
            <div className="insights-card-grid">
              {neglectData.map((item) => (
                <motion.article
                  className="insights-info-card insights-neglect-card"
                  key={item.lifeAreaId}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{
                    duration: 0.25,
                    ease: "easeOut",
                  }}
                >
                  <div className="insights-info-card-header">
                    <div className="insights-info-card-icon insights-info-card-icon-warning">
                      <AlertTriangle size={18} />
                    </div>

                    {item.level && (
                      <span className="insights-severity">{item.level}</span>
                    )}
                  </div>

                  <h3>{item.lifeAreaName ?? `Life Area ${item.lifeAreaId}`}</h3>

                  <div className="insights-info-card-stats">
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
                </motion.article>
              ))}
            </div>
          )}
        </section>

        {/* Rebalancing Suggestions */}

        <section className="insights-section">
          <div className="insights-section-header">
            <div>
              <p className="insights-section-kicker">REBALANCE</p>

              <h2>Rebalancing Suggestions</h2>

              <p>
                Possible ways to redirect remaining time between life areas.
                Suggestions never change your plan automatically.
              </p>
            </div>
          </div>

          {rebalancingLoading ? (
            <div className="insights-feedback insights-feedback-loading">
              <Clock3 size={20} />

              <span>Loading rebalancing suggestions...</span>
            </div>
          ) : rebalancingError ? (
            <div className="insights-feedback insights-feedback-error">
              <AlertTriangle size={20} />

              <div>
                <strong>Unable to load rebalancing suggestions</strong>

                <p>{rebalancingError}</p>
              </div>
            </div>
          ) : rebalancingSuggestions.length === 0 ? (
            <div className="insights-empty">
              <div className="insights-empty-icon">
                <ArrowLeftRight size={22} />
              </div>

              <h3>No rebalancing suggestions</h3>

              <p>
                There are no current suggestions for shifting time between life
                areas.
              </p>
            </div>
          ) : (
            <div className="insights-card-grid">
              {rebalancingSuggestions.map((suggestion, index) => (
                <motion.article
                  className="insights-info-card"
                  key={
                    suggestion.id ??
                    `${suggestion.sourceLifeAreaId}-${suggestion.destinationLifeAreaId}-${index}`
                  }
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{
                    duration: 0.25,
                    ease: "easeOut",
                  }}
                >
                  <div className="insights-info-card-header">
                    <div className="insights-info-card-icon">
                      <ArrowLeftRight size={18} />
                    </div>
                  </div>

                  <div className="insights-route">
                    <strong>
                      {suggestion.sourceLifeAreaName ??
                        suggestion.fromLifeAreaName ??
                        `Area ${suggestion.sourceLifeAreaId ?? suggestion.fromLifeAreaId}`}
                    </strong>

                    <ArrowLeftRight size={16} />

                    <strong>
                      {suggestion.destinationLifeAreaName ??
                        suggestion.toLifeAreaName ??
                        `Area ${suggestion.destinationLifeAreaId ?? suggestion.toLifeAreaId}`}
                    </strong>
                  </div>

                  <div className="insights-transfer">
                    <span>Potential transfer</span>

                    <strong>
                      {formatHours(
                        suggestion.transferableMinutes ?? suggestion.minutes,
                      )}
                    </strong>
                  </div>

                  <p className="insights-card-description">
                    {suggestion.explanation ??
                      suggestion.reason ??
                      "No explanation was provided."}
                  </p>

                  <div className="insights-suggestion-note">
                    <Lightbulb size={15} />

                    <span>Suggestion only — your allocation is unchanged.</span>
                  </div>
                </motion.article>
              ))}
            </div>
          )}
        </section>

        {/* Historical Trends */}

        <section className="insights-section">
          <div className="insights-section-header">
            <div>
              <p className="insights-section-kicker">PATTERNS</p>

              <h2>Historical Trends</h2>

              <p>
                Review how your time usage has changed across previous weeks.
              </p>
            </div>
          </div>

          {analyticsLoading ? (
            <div className="insights-feedback insights-feedback-loading">
              <Clock3 size={20} />

              <span>Loading historical trends...</span>
            </div>
          ) : analyticsError ? (
            <div className="insights-feedback insights-feedback-error">
              <AlertTriangle size={20} />

              <div>
                <strong>Unable to load historical trends</strong>

                <p>{analyticsError}</p>
              </div>
            </div>
          ) : !Array.isArray(analytics?.weeklyTrends) ||
            analytics.weeklyTrends.length === 0 ? (
            <div className="insights-empty">
              <div className="insights-empty-icon">
                <BarChart3 size={22} />
              </div>

              <h3>No historical data yet</h3>

              <p>
                Historical trends will appear here once completed weeks contain
                tracked activity and recommendation data.
              </p>
            </div>
          ) : (
            <div className="insights-chart-grid">
              <article className="insights-chart-card">
                <div className="insights-chart-header">
                  <div className="insights-chart-icon">
                    <TrendingUp size={18} />
                  </div>

                  <div>
                    <h3>Weekly Utilization</h3>

                    <p>Actual tracked time compared with recommended time.</p>
                  </div>
                </div>

                <div className="insights-chart">
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
              </article>

              <article className="insights-chart-card">
                <div className="insights-chart-header">
                  <div className="insights-chart-icon">
                    <BarChart3 size={18} />
                  </div>

                  <div>
                    <h3>Recommended vs Actual</h3>

                    <p>
                      Compare recommended time with the time you actually
                      tracked each week.
                    </p>
                  </div>
                </div>

                <div className="insights-chart">
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
              </article>
            </div>
          )}
        </section>

        {/* AI Weekly Insight */}

        <section className="insights-section">
          <div className="insights-section-header insights-ai-header">
            <div>
              <p className="insights-section-kicker">AI ASSISTED</p>

              <h2>AI Weekly Insight</h2>

              <p>
                Get a natural-language summary based on the backend's weekly
                data.
              </p>
            </div>

            <button
              type="button"
              className="insights-primary-button"
              onClick={handleGenerateInsight}
              disabled={aiLoading}
            >
              <Sparkles size={17} />

              <span>
                {aiLoading ? "Generating Insight..." : "Generate Insight"}
              </span>
            </button>
          </div>

          {aiLoading ? (
            <div className="insights-feedback insights-feedback-loading">
              <BrainCircuit size={20} />

              <span>Generating your weekly insight...</span>
            </div>
          ) : aiError ? (
            <div className="insights-feedback insights-feedback-error">
              <AlertTriangle size={20} />

              <div>
                <strong>Unable to generate insight</strong>

                <p>{aiError}</p>
              </div>
            </div>
          ) : !aiInsight ? (
            <div className="insights-ai-empty">
              <div className="insights-ai-icon">
                <BrainCircuit size={24} />
              </div>

              <div>
                <h3>No insight generated yet</h3>

                <p>
                  Generate an AI weekly insight to receive a concise summary,
                  observations, and suggested actions.
                </p>
              </div>
            </div>
          ) : (
            <motion.div
              className="insights-ai-card"
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{
                duration: 0.28,
                ease: "easeOut",
              }}
            >
              {aiInsight.summary && (
                <div className="insights-ai-summary">
                  <div className="insights-ai-section-icon">
                    <Sparkles size={17} />
                  </div>

                  <div>
                    <h3>Summary</h3>

                    <p>{aiInsight.summary}</p>
                  </div>
                </div>
              )}

              {Array.isArray(aiInsight.observations) &&
                aiInsight.observations.length > 0 && (
                  <div className="insights-ai-section">
                    <div className="insights-ai-section-heading">
                      <Activity size={17} />

                      <h3>Observations</h3>
                    </div>

                    <ul>
                      {aiInsight.observations.map((observation, index) => (
                        <li key={index}>{observation}</li>
                      ))}
                    </ul>
                  </div>
                )}

              {Array.isArray(aiInsight.actions) &&
                aiInsight.actions.length > 0 && (
                  <div className="insights-ai-section">
                    <div className="insights-ai-section-heading">
                      <Lightbulb size={17} />

                      <h3>Suggested Actions</h3>
                    </div>

                    <ul>
                      {aiInsight.actions.map((action, index) => (
                        <li key={index}>{action}</li>
                      ))}
                    </ul>
                  </div>
                )}
            </motion.div>
          )}
        </section>
      </div>
    </main>
  );
}

export default InsightsPage;
