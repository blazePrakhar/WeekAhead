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
      <main className="weekly-availability-page">
        <section className="weekly-availability-header">
          <div>
            <h1>Rebalancing Suggestions</h1>
            <p className="page-description">
              Review possible ways to redirect time based on your current week.
            </p>
          </div>
        </section>

        <div className="weekly-availability-status">
          <p>Loading rebalancing suggestions...</p>
        </div>
      </main>
    );
  }

  if (error) {
    return (
      <main className="weekly-availability-page">
        <section className="weekly-availability-header">
          <div>
            <h1>Rebalancing Suggestions</h1>
            <p className="page-description">
              Review possible ways to redirect time based on your current week.
            </p>
          </div>
        </section>

        <div className="weekly-availability-error">
          <p>{error}</p>
        </div>
      </main>
    );
  }

  return (
    <main className="weekly-availability-page">
      <section className="weekly-availability-header">
        <div>
          <h1>Rebalancing Suggestions</h1>
          <p className="page-description">
            Review possible ways to redirect time based on your current weekly allocation and actual tracked time.
          </p>
        </div>
      </section>

      <section className="life-areas-section">
        <div className="section-header">
          <h2>Summary</h2>
        </div>

        <div className="life-areas-grid">
          <article className="life-area-card">
            <div className="life-area-card-header">
              <div>
                <h3>Suggestions</h3>
                <p className="life-area-description">
                  Potential rebalancing options identified by the backend.
                </p>
              </div>
            </div>

            <div className="life-area-details">
              <div className="life-area-detail">
                <span className="detail-label">Count</span>
                <strong>{suggestions.length}</strong>
              </div>
            </div>
          </article>

          <article className="life-area-card">
            <div className="life-area-card-header">
              <div>
                <h3>Potential Transfer</h3>
                <p className="life-area-description">
                  Total time represented by the current suggestions.
                </p>
              </div>
            </div>

            <div className="life-area-details">
              <div className="life-area-detail">
                <span className="detail-label">Time</span>
                <strong>{formatHours(totalTransferableMinutes)}</strong>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section className="life-areas-section">
        <div className="section-header">
          <div>
            <h2>Suggestions</h2>
            <p className="page-description">
              These suggestions are calculated from your current allocation and tracked time.
            </p>
          </div>
        </div>

        {suggestions.length === 0 ? (
          <div className="empty-state">
            <p>No rebalancing suggestions are currently available.</p>
          </div>
        ) : (
          <div className="life-areas-grid">
            {suggestions.map((suggestion, index) => {
              const sourceArea = lifeAreaMap.get(suggestion.sourceLifeAreaId);
              const destinationArea = lifeAreaMap.get(
                suggestion.destinationLifeAreaId,
              );

              return (
                <article
                  className="life-area-card"
                  key={`${suggestion.sourceLifeAreaId}-${suggestion.destinationLifeAreaId}-${index}`}
                >
                  <div className="life-area-card-header">
                    <div>
                      <h3>
                        {sourceArea?.name ??
                          `Life Area #${suggestion.sourceLifeAreaId}`}
                        {" -> "}
                        {destinationArea?.name ??
                          `Life Area #${suggestion.destinationLifeAreaId}`}
                      </h3>
                      <p className="life-area-description">
                        Potential time transfer
                      </p>
                    </div>
                  </div>

                  <div className="life-area-details">
                    <div className="life-area-detail">
                      <span className="detail-label">Transferable</span>
                      <strong>
                        {formatHours(suggestion.transferableMinutes)}
                      </strong>
                    </div>
                  </div>

                  <p className="life-area-description">
                    {suggestion.explanation}
                  </p>

                  <p className="life-area-description">
                    Suggestion only. WeekAhead will not automatically change your allocation.
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

export default RebalancingPage;
