import {
  ArrowLeftRight,
  Clock3,
  Info,
  Lightbulb,
  Sparkles,
} from "lucide-react";
import { motion } from "motion/react";
import { useEffect, useMemo, useState } from "react";

import { getLifeAreas } from "../api/lifeAreaApi";
import { getRebalancingSuggestions } from "../api/rebalancingApi";

function formatHours(minutes = 0) {
  return `${(minutes / 60).toFixed(1)} h`;
}

function RebalancingPage() {
  const [suggestions, setSuggestions] = useState([]);
  const [lifeAreas, setLifeAreas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function loadRebalancing() {
      try {
        setLoading(true);
        setError("");

        const [suggestionData, lifeAreaData] = await Promise.all([
          getRebalancingSuggestions(),
          getLifeAreas(),
        ]);

        if (!cancelled) {
          setSuggestions(suggestionData ?? []);
          setLifeAreas(lifeAreaData ?? []);
        }
      } catch (err) {
        if (!cancelled) {
          console.error("Failed to load rebalancing suggestions:", err);

          setError(
            err.response?.data?.message ||
              "Unable to load rebalancing suggestions. Please try again.",
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadRebalancing();

    return () => {
      cancelled = true;
    };
  }, []);

  const lifeAreaMap = useMemo(
    () => new Map(lifeAreas.map((area) => [area.id, area])),
    [lifeAreas],
  );

  const totalTransferableMinutes = useMemo(
    () =>
      suggestions.reduce(
        (total, suggestion) => total + (suggestion.transferableMinutes ?? 0),
        0,
      ),
    [suggestions],
  );

  if (loading) {
    return (
      <main className="rebalancing-page">
        <section className="rebalancing-loading">
          <div className="rebalancing-loading-icon">
            <ArrowLeftRight size={22} strokeWidth={1.8} />
          </div>

          <div>
            <h1>Rebalancing Suggestions</h1>
            <p>Reviewing your current weekly balance...</p>
          </div>
        </section>
      </main>
    );
  }

  if (error) {
    return (
      <main className="rebalancing-page">
        <motion.header
          className="rebalancing-header"
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25 }}
        >
          <div>
            <div className="rebalancing-eyebrow">
              <ArrowLeftRight size={15} strokeWidth={1.9} />
              <span>REBALANCE YOUR WEEK</span>
            </div>

            <h1>Rebalancing Suggestions</h1>

            <p className="rebalancing-description">
              Review possible ways to redirect time based on your current weekly
              allocation and actual tracked time.
            </p>
          </div>

          <div className="rebalancing-week-badge">
            <ArrowLeftRight size={15} strokeWidth={1.8} />
            <span>Current Week</span>
          </div>
        </motion.header>

        <div className="rebalancing-feedback rebalancing-feedback-error">
          <Info size={18} strokeWidth={1.8} />
          <p>{error}</p>
        </div>
      </main>
    );
  }

  return (
    <main className="rebalancing-page">
      <motion.header
        className="rebalancing-header"
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
      >
        <div>
          <div className="rebalancing-eyebrow">
            <ArrowLeftRight size={15} strokeWidth={1.9} />
            <span>REBALANCE YOUR WEEK</span>
          </div>

          <h1>Rebalancing Suggestions</h1>

          <p className="rebalancing-description">
            Review possible ways to redirect time based on your current weekly
            allocation and actual tracked time.
          </p>
        </div>

        <div className="rebalancing-week-badge">
          <ArrowLeftRight size={15} strokeWidth={1.8} />
          <span>Current Week</span>
        </div>
      </motion.header>

      <motion.section
        className="rebalancing-summary-card"
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.05 }}
      >
        <div className="rebalancing-card-header">
          <div className="rebalancing-card-heading">
            <span className="rebalancing-card-icon">
              <Sparkles size={19} strokeWidth={1.8} />
            </span>

            <div>
              <span className="rebalancing-section-label">WEEKLY BALANCE</span>

              <h2>Review Your Week</h2>

              <p>
                Potential adjustments identified from your planned and tracked
                time.
              </p>
            </div>
          </div>
        </div>

        <div className="rebalancing-summary-grid">
          <article className="rebalancing-stat rebalancing-stat-primary">
            <span>Suggestions</span>
            <strong>{suggestions.length}</strong>
            <p>Potential adjustments identified</p>
          </article>

          <article className="rebalancing-stat rebalancing-stat-transfer">
            <span>Potential Transfer</span>

            <strong>{formatHours(totalTransferableMinutes)}</strong>

            <p>Total time represented by suggestions</p>
          </article>
        </div>
      </motion.section>

      <section className="rebalancing-suggestions-section">
        <div className="rebalancing-list-header">
          <div>
            <span className="rebalancing-section-label">
              POSSIBLE ADJUSTMENTS
            </span>

            <h2>Suggestions</h2>

            <p>
              These suggestions are calculated from your current allocation and
              tracked time.
            </p>
          </div>

          {suggestions.length > 0 && (
            <div className="rebalancing-count">
              <strong>{suggestions.length}</strong>
              <span>
                {suggestions.length === 1 ? "suggestion" : "suggestions"}
              </span>
            </div>
          )}
        </div>

        {suggestions.length === 0 ? (
          <motion.div
            className="rebalancing-empty"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <div className="rebalancing-empty-icon">
              <Lightbulb size={22} strokeWidth={1.8} />
            </div>

            <h3>No rebalancing suggestions</h3>

            <p>
              Your current weekly data does not have any suggested time
              transfers right now.
            </p>
          </motion.div>
        ) : (
          <div className="rebalancing-grid">
            {suggestions.map((suggestion, index) => {
              const sourceArea = lifeAreaMap.get(suggestion.sourceLifeAreaId);

              const destinationArea = lifeAreaMap.get(
                suggestion.destinationLifeAreaId,
              );

              return (
                <motion.article
                  className="rebalancing-card"
                  key={`${suggestion.sourceLifeAreaId}-${suggestion.destinationLifeAreaId}-${index}`}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{
                    duration: 0.28,
                    delay: index * 0.05,
                  }}
                >
                  <div className="rebalancing-card-top">
                    <div className="rebalancing-transfer-icon">
                      <ArrowLeftRight size={18} strokeWidth={1.8} />
                    </div>

                    <span className="rebalancing-suggestion-badge">
                      Suggested
                    </span>
                  </div>

                  <div className="rebalancing-route">
                    <div>
                      <span>FROM</span>

                      <strong>
                        {sourceArea?.name ??
                          `Life Area #${suggestion.sourceLifeAreaId}`}
                      </strong>
                    </div>

                    <ArrowLeftRight
                      size={18}
                      strokeWidth={1.7}
                      className="rebalancing-route-arrow"
                    />

                    <div>
                      <span>TO</span>

                      <strong>
                        {destinationArea?.name ??
                          `Life Area #${suggestion.destinationLifeAreaId}`}
                      </strong>
                    </div>
                  </div>

                  <div className="rebalancing-transfer">
                    <div className="rebalancing-transfer-icon-small">
                      <Clock3 size={15} strokeWidth={1.8} />
                    </div>

                    <div>
                      <span>Transferable Time</span>

                      <strong>
                        {formatHours(suggestion.transferableMinutes)}
                      </strong>
                    </div>
                  </div>

                  <div className="rebalancing-explanation">
                    <div className="rebalancing-explanation-icon">
                      <Lightbulb size={15} strokeWidth={1.8} />
                    </div>

                    <p>{suggestion.explanation}</p>
                  </div>

                  <div className="rebalancing-notice">
                    <Info size={15} strokeWidth={1.8} />

                    <p>
                      Suggestion only. WeekAhead will not automatically change
                      your allocation.
                    </p>
                  </div>
                </motion.article>
              );
            })}
          </div>
        )}
      </section>
    </main>
  );
}

export default RebalancingPage;
