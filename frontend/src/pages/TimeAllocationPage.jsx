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
          err.response?.data?.message ||
            "Unable to load the current week.",
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
        err.response?.data?.message ||
          "Unable to generate the recommendation.",
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

  return (
    <main className="weekly-availability-page">
      <header className="weekly-availability-header">
        <div>
          <h1>Time Allocation</h1>
          <p>
            Generate and review your recommended weekly time allocation.
          </p>
        </div>
      </header>

      {error && <p className="form-error">{error}</p>}

      {week && (
        <section className="weekly-availability-card">
          <h2>Current Week</h2>

          <div className="weekly-availability-summary">
            <div>
              <span>Week</span>
              <strong>
                {week.weekStartDate} – {week.weekEndDate}
              </strong>
            </div>

            <div>
              <span>Available Time</span>
              <strong>
                {minutesToHoursAndMinutes(week.availableMinutes)}
              </strong>
            </div>

            <div>
              <span>Fixed Commitments</span>
              <strong>
                {minutesToHoursAndMinutes(week.fixedCommitmentMinutes)}
              </strong>
            </div>
          </div>

          <div className="form-actions">
            <button
              type="button"
              className="primary-button"
              onClick={handleGenerateRecommendation}
              disabled={generating}
            >
              {generating
                ? "Generating..."
                : allocation
                  ? "Regenerate Recommendation"
                  : "Generate Recommendation"}
            </button>
          </div>
        </section>
      )}

      {allocation && (
        <section className="weekly-availability-card">
          <h2>Recommended Allocation</h2>

          <div className="weekly-availability-summary">
            <div>
              <span>Discretionary Time</span>
              <strong>
                {minutesToHoursAndMinutes(
                  allocation.discretionaryMinutes,
                )}
              </strong>
            </div>

            <div>
              <span>Total Recommended</span>
              <strong>
                {minutesToHoursAndMinutes(
                  allocation.totalRecommendedMinutes,
                )}
              </strong>
            </div>
          </div>

          <div>
            {allocation.allocations.map((item) => (
              <div key={item.lifeAreaId}>
                <h3>{item.lifeAreaName}</h3>
                <p>
                  Recommended:{" "}
                  {minutesToHoursAndMinutes(item.recommendedMinutes)}
                </p>
                <p>Weight: {item.weight}</p>
                <p>
                  Range:{" "}
                  {minutesToHoursAndMinutes(item.minMinutes)} –{" "}
                  {minutesToHoursAndMinutes(item.maxMinutes)}
                </p>
              </div>
            ))}
          </div>
        </section>
      )}
    </main>
  );
}

export default TimeAllocationPage;