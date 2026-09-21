import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";

import { getLifeAreas } from "../api/lifeAreaApi";
import {
  createTimeLog,
  getTimeLogs,
  updateTimeLog,
  deleteTimeLog,
} from "../api/timeLogApi";

function getTodayDate() {
  return new Date().toISOString().split("T")[0];
}

function getWeekStartDate() {
  const today = new Date();
  const day = today.getDay();
  const difference = day === 0 ? -6 : 1 - day;

  const weekStart = new Date(today);
  weekStart.setDate(today.getDate() + difference);

  return weekStart.toISOString().split("T")[0];
}

function formatDuration(totalMinutes = 0) {
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

function TimeLogsPage() {
  const [lifeAreas, setLifeAreas] = useState([]);
  const [timeLogs, setTimeLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [editingId, setEditingId] = useState(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: {
      lifeAreaId: "",
      logDate: getTodayDate(),
      durationMinutes: "",
      note: "",
      source: "MANUAL",
    },
  });

  const loadTimeLogs = async () => {
    const from = getWeekStartDate();
    const to = getTodayDate();

    const data = await getTimeLogs(from, to);
    setTimeLogs(data);
  };

  useEffect(() => {
    let cancelled = false;

    async function loadInitialData() {
      try {
        setLoading(true);
        setError("");

        const [areas, logs] = await Promise.all([
          getLifeAreas(),
          getTimeLogs(getWeekStartDate(), getTodayDate()),
        ]);

        if (!cancelled) {
          setLifeAreas(areas);
          setTimeLogs(logs);
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err.response?.data?.message || "Unable to load time tracking data.",
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadInitialData();

    return () => {
      cancelled = true;
    };
  }, []);

  const handleFormSubmit = async (formData) => {
    try {
      setSubmitting(true);
      setError("");
      setSuccess("");

      const payload = {
        lifeAreaId: Number(formData.lifeAreaId),
        logDate: formData.logDate,
        durationMinutes: Number(formData.durationMinutes),
        note: formData.note.trim(),
        source: formData.source,
      };

      if (editingId) {
        await updateTimeLog(editingId, payload);
        setSuccess("Time log updated successfully.");
      } else {
        await createTimeLog(payload);
        setSuccess("Time logged successfully.");
      }

      setEditingId(null);

      reset({
        lifeAreaId: "",
        logDate: getTodayDate(),
        durationMinutes: "",
        note: "",
        source: "MANUAL",
      });

      await loadTimeLogs();
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Unable to save the time log. Please check your input.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = (timeLog) => {
    setEditingId(timeLog.id);
    setError("");
    setSuccess("");

    reset({
      lifeAreaId: String(timeLog.lifeAreaId),
      logDate: timeLog.logDate,
      durationMinutes: timeLog.durationMinutes,
      note: timeLog.note ?? "",
      source: timeLog.source,
    });

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  };

  const handleDelete = async (id) => {
    const confirmed = window.confirm(
      "Are you sure you want to delete this time log?",
    );

    if (!confirmed) {
      return;
    }

    try {
      setError("");
      setSuccess("");

      await deleteTimeLog(id);

      if (editingId === id) {
        setEditingId(null);

        reset({
          lifeAreaId: "",
          logDate: getTodayDate(),
          durationMinutes: "",
          note: "",
          source: "MANUAL",
        });
      }

      setSuccess("Time log deleted successfully.");

      await loadTimeLogs();
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Unable to delete the time log. Please try again.",
      );
    }
  };

  if (loading) {
    return (
      <main className="weekly-availability-page">
        <section className="weekly-availability-header">
          <div>
            <h1>Time Tracking</h1>
            <p className="page-description">
              Record the actual time you spend on your life areas.
            </p>
          </div>
        </section>

        <div className="weekly-availability-status">
          <p>Loading time tracking...</p>
        </div>
      </main>
    );
  }

  return (
    <main className="weekly-availability-page">
      <section className="weekly-availability-header">
        <div>
          <h1>Time Tracking</h1>
          <p className="page-description">
            Record the actual time you spend on your life areas.
          </p>
        </div>
      </section>

      {success && (
        <div className="weekly-availability-success">
          <p>{success}</p>
        </div>
      )}

      {error && (
        <div className="weekly-availability-error">
          <p>{error}</p>
        </div>
      )}

      <section className="life-area-form-card">
        <div className="life-area-form-header">
          <h2>{editingId ? "Edit Time Log" : "Quick Time Log"}</h2>

          <p>
            {editingId
              ? "Update the details of this time log."
              : "Record how much time you spent on a life area."}
          </p>
        </div>

        <form onSubmit={handleSubmit(handleFormSubmit)} noValidate>
          <div className="form-group">
            <label htmlFor="lifeAreaId">Life Area</label>

            <select
              id="lifeAreaId"
              {...register("lifeAreaId", {
                required: "Life area is required.",
              })}
            >
              <option value="">Select a life area</option>

              {lifeAreas
                .filter((lifeArea) => lifeArea.isActive !== false)
                .map((lifeArea) => (
                  <option key={lifeArea.id} value={lifeArea.id}>
                    {lifeArea.name}
                  </option>
                ))}
            </select>

            {errors.lifeAreaId && (
              <p className="form-error">{errors.lifeAreaId.message}</p>
            )}
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="logDate">Date</label>

              <input
                id="logDate"
                type="date"
                {...register("logDate", {
                  required: "Date is required.",
                })}
              />

              {errors.logDate && (
                <p className="form-error">{errors.logDate.message}</p>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="durationMinutes">Duration (minutes)</label>

              <input
                id="durationMinutes"
                type="number"
                min="1"
                step="1"
                placeholder="e.g. 90"
                {...register("durationMinutes", {
                  required: "Duration is required.",
                  validate: (value) => {
                    const number = Number(value);

                    if (!Number.isInteger(number)) {
                      return "Duration must be a whole number.";
                    }

                    if (number <= 0) {
                      return "Duration must be greater than 0.";
                    }

                    return true;
                  },
                })}
              />

              {errors.durationMinutes && (
                <p className="form-error">{errors.durationMinutes.message}</p>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="source">Source</label>

              <select id="source" {...register("source")}>
                <option value="MANUAL">Manual</option>
              </select>
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="note">Note (optional)</label>

            <textarea
              id="note"
              rows="3"
              maxLength="500"
              placeholder="What did you spend this time on?"
              {...register("note", {
                maxLength: {
                  value: 500,
                  message: "Note must not exceed 500 characters.",
                },
              })}
            />

            {errors.note && <p className="form-error">{errors.note.message}</p>}
          </div>

          <div className="form-actions">
            <button
              type="submit"
              className="primary-button"
              disabled={submitting || lifeAreas.length === 0}
            >
              {submitting
                ? editingId
                  ? "Updating..."
                  : "Logging..."
                : editingId
                  ? "Update Time Log"
                  : "Log Time"}
            </button>

            {editingId && (
              <button
                type="button"
                className="secondary-button"
                onClick={() => {
                  setEditingId(null);
                  setError("");
                  setSuccess("");

                  reset({
                    lifeAreaId: "",
                    logDate: getTodayDate(),
                    durationMinutes: "",
                    note: "",
                    source: "MANUAL",
                  });
                }}
                disabled={submitting}
              >
                Cancel
              </button>
            )}
          </div>
        </form>
      </section>

      <section className="life-areas-section">
        <div className="section-header">
          <h2>This Week's Time Logs</h2>
        </div>

        {timeLogs.length === 0 ? (
          <div className="empty-state">
            <p>No time logs found for this week.</p>
            <p>Use the form above to record your first log.</p>
          </div>
        ) : (
          <div className="life-areas-grid">
            {timeLogs.map((timeLog) => (
              <article className="life-area-card" key={timeLog.id}>
                <div className="life-area-card-header">
                  <div>
                    <h3>{timeLog.lifeAreaName}</h3>

                    <p className="life-area-description">{timeLog.logDate}</p>
                  </div>

                  <span className="status-badge">{timeLog.source}</span>
                </div>

                <div className="life-area-details">
                  <div className="life-area-detail">
                    <span className="detail-label">Duration</span>
                    <strong>{formatDuration(timeLog.durationMinutes)}</strong>
                  </div>

                  <div className="life-area-detail">
                    <span className="detail-label">Minutes</span>
                    <strong>{timeLog.durationMinutes}</strong>
                  </div>

                  <div className="life-area-detail">
                    <span className="detail-label">Logged</span>
                    <strong>
                      {new Date(timeLog.createdAt).toLocaleTimeString([], {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </strong>
                  </div>
                </div>

                {timeLog.note && (
                  <p className="life-area-description">{timeLog.note}</p>
                )}

                <div className="life-area-actions">
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => handleEdit(timeLog)}
                  >
                    Edit
                  </button>

                  <button
                    type="button"
                    className="danger-button"
                    onClick={() => handleDelete(timeLog.id)}
                  >
                    Delete
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}

export default TimeLogsPage;
