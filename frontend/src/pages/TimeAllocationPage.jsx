import {
  CalendarRange,
  Clock3,
  RefreshCw,
  Sparkles,
  Target,
} from "lucide-react";
import { motion } from "motion/react";
import { useEffect, useState } from "react";

import {
  generateRecommendation,
  getAllocations,
  getCurrentWeek,
} from "../api/weekApi";

function minutesToHoursAndMinutes(totalMinutes = 0) {
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;

  if (hours === 0) {
    return `${minutes}m`;
  }

  if (minutes === 0) {
    return `${hours}h`;
  }

  return `${hours}h ${minutes}m`;
}

function TimeAllocationPage() {
  const [week, setWeek] = useState(null);
  const [allocation, setAllocation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadWeek() {
      try {
        setLoading(true);
        setError("");

        const currentWeek = await getCurrentWeek();
        setWeek(currentWeek);

        try {
          const existingAllocation = await getAllocations(currentWeek.id);
          setAllocation(existingAllocation);
        } catch {
          setAllocation(null);
        }
      } catch (err) {
        setError(
          err.response?.data?.message || "Unable to load the current week.",
        );
      } finally {
        setLoading(false);
      }
    }

    loadWeek();
  }, []);

  async function handleGenerateRecommendation() {
    if (!week) {
      return;
    }

    try {
      setGenerating(true);
      setError("");

      const result = await generateRecommendation(week.id);
      setAllocation(result);
    } catch (err) {
      setError(
        err.response?.data?.message || "Unable to generate the recommendation.",
      );
    } finally {
      setGenerating(false);
    }
  }

  if (loading) {
    return (
      <main className="weekly-availability-page">
        <p className="loading-message">Loading time allocation...</p>
      </main>
    );
  }

  if (error && !allocation) {
    return (
      <main className="weekly-availability-page">
        <p className="form-error">{error}</p>
      </main>
    );
  }

  if (loading) {
    return (
      <main className="time-allocation-page">
        <section className="time-allocation-loading">
          <div className="time-allocation-loading-icon">
            <CalendarRange size={22} strokeWidth={1.8} />
          </div>

          <div>
            <h1>Time Allocation</h1>
            <p>Loading your weekly allocation...</p>
          </div>
        </section>
      </main>
    );
  }

  if (error && !allocation) {
    return (
      <main className="time-allocation-page">
        <section className="time-allocation-error">
          <div className="time-allocation-feedback-icon">
            <Target size={20} strokeWidth={1.8} />
          </div>

          <div>
            <h1>Time Allocation</h1>
            <p>{error}</p>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="time-allocation-page">
      <motion.header
        className="time-allocation-header"
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
      >
        <div>
          <div className="time-allocation-eyebrow">
            <CalendarRange size={15} strokeWidth={1.9} />
            <span>PLAN YOUR WEEK</span>
          </div>

          <h1>Time Allocation</h1>

          <p className="time-allocation-description">
            Generate and review your recommended weekly time allocation.
          </p>
        </div>

        {week && (
          <div className="time-allocation-week-badge">
            <CalendarRange size={15} strokeWidth={1.8} />
            <span>
              {week.weekStartDate} – {week.weekEndDate}
            </span>
          </div>
        )}
      </motion.header>

      {error && (
        <motion.div
          className="time-allocation-feedback time-allocation-feedback-error"
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <Target size={18} strokeWidth={1.8} />
          <p>{error}</p>
        </motion.div>
      )}

      {week && (
        <motion.section
          className="time-allocation-card"
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.05 }}
        >
          <div className="time-allocation-card-header">
            <div className="time-allocation-card-heading">
              <span className="time-allocation-card-icon">
                <Clock3 size={19} strokeWidth={1.8} />
              </span>

              <div>
                <span className="time-allocation-section-label">
                  WEEKLY CAPACITY
                </span>

                <h2>Current Week</h2>

                <p>
                  Review the time available before generating your
                  recommendation.
                </p>
              </div>
            </div>
          </div>

          <div className="time-allocation-stats">
            <div className="time-allocation-stat time-allocation-stat-primary">
              <span>Available Time</span>
              <strong>{minutesToHoursAndMinutes(week.availableMinutes)}</strong>
            </div>

            <div className="time-allocation-stat">
              <span>Fixed Commitments</span>
              <strong>
                {minutesToHoursAndMinutes(week.fixedCommitmentMinutes)}
              </strong>
            </div>

            <div className="time-allocation-stat time-allocation-stat-week">
              <span>Week</span>
              <strong>
                {week.weekStartDate} – {week.weekEndDate}
              </strong>
            </div>
          </div>

          <div className="time-allocation-actions">
            <button
              type="button"
              className="primary-button"
              onClick={handleGenerateRecommendation}
              disabled={generating}
            >
              {generating ? (
                <>
                  <RefreshCw
                    size={16}
                    strokeWidth={2}
                    className="time-allocation-spin"
                  />
                  Generating...
                </>
              ) : (
                <>
                  <Sparkles size={16} strokeWidth={1.9} />
                  {allocation
                    ? "Regenerate Recommendation"
                    : "Generate Recommendation"}
                </>
              )}
            </button>
          </div>
        </motion.section>
      )}

      {allocation && (
        <motion.section
          className="time-allocation-card time-allocation-recommendation"
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3, delay: 0.1 }}
        >
          <div className="time-allocation-card-header">
            <div className="time-allocation-card-heading">
              <span className="time-allocation-card-icon time-allocation-card-icon-accent">
                <Sparkles size={19} strokeWidth={1.8} />
              </span>

              <div>
                <span className="time-allocation-section-label">
                  RECOMMENDATION
                </span>

                <h2>Recommended Allocation</h2>

                <p>
                  A structured view of how your discretionary time is
                  distributed across life areas.
                </p>
              </div>
            </div>
          </div>

          <div className="time-allocation-stats">
            <div className="time-allocation-stat time-allocation-stat-primary">
              <span>Discretionary Time</span>
              <strong>
                {minutesToHoursAndMinutes(allocation.discretionaryMinutes)}
              </strong>
            </div>

            <div className="time-allocation-stat time-allocation-stat-accent">
              <span>Total Recommended</span>
              <strong>
                {minutesToHoursAndMinutes(allocation.totalRecommendedMinutes)}
              </strong>
            </div>
          </div>

          <div className="time-allocation-list">
            {allocation.allocations.map((item, index) => (
              <motion.article
                key={item.lifeAreaId}
                className="time-allocation-item"
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{
                  duration: 0.25,
                  delay: 0.12 + index * 0.04,
                }}
              >
                <div className="time-allocation-item-header">
                  <div>
                    <span className="time-allocation-item-label">
                      LIFE AREA
                    </span>
                    <h3>{item.lifeAreaName}</h3>
                  </div>

                  <strong className="time-allocation-item-time">
                    {minutesToHoursAndMinutes(item.recommendedMinutes)}
                  </strong>
                </div>

                <div className="time-allocation-item-details">
                  <div>
                    <span>Weight</span>
                    <strong>{item.weight}</strong>
                  </div>

                  <div>
                    <span>Range</span>
                    <strong>
                      {minutesToHoursAndMinutes(item.minMinutes)} –{" "}
                      {minutesToHoursAndMinutes(item.maxMinutes)}
                    </strong>
                  </div>
                </div>
              </motion.article>
            ))}
          </div>
        </motion.section>
      )}
    </main>
  );
}

export default TimeAllocationPage;
