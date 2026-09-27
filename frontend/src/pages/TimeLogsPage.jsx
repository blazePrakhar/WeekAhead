import {
  CalendarDays,
  Clock3,
  FileText,
  Pencil,
  Plus,
  Timer,
  Trash2,
} from "lucide-react";
import { motion } from "motion/react";
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
      <main className="time-logs-page">
        <section className="time-logs-loading">
          <div className="time-logs-loading-icon">
            <Timer size={22} strokeWidth={1.8} />
          </div>

          <div>
            <h1>Time Tracking</h1>
            <p>Loading your weekly time logs...</p>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="time-logs-page">
      <motion.header
        className="time-logs-header"
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
      >
        <div>
          <div className="time-logs-eyebrow">
            <Timer size={15} strokeWidth={1.9} />
            <span>TRACK YOUR WEEK</span>
          </div>

          <h1>Time Tracking</h1>

          <p className="time-logs-description">
            Record the actual time you spend across your life areas.
          </p>
        </div>

        <div className="time-logs-week-badge">
          <CalendarDays size={15} strokeWidth={1.8} />
          <span>This Week</span>
        </div>
      </motion.header>

      {success && (
        <motion.div
          className="time-logs-feedback time-logs-feedback-success"
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <Clock3 size={18} strokeWidth={1.8} />
          <p>{success}</p>
        </motion.div>
      )}

      {error && (
        <motion.div
          className="time-logs-feedback time-logs-feedback-error"
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <Clock3 size={18} strokeWidth={1.8} />
          <p>{error}</p>
        </motion.div>
      )}

      <motion.section
        className="time-logs-form-card"
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, delay: 0.05 }}
      >
        <div className="time-logs-form-header">
          <div className="time-logs-form-heading">
            <span className="time-logs-form-icon">
              {editingId ? (
                <Pencil size={19} strokeWidth={1.8} />
              ) : (
                <Plus size={19} strokeWidth={1.8} />
              )}
            </span>

            <div>
              <span className="time-logs-section-label">
                {editingId ? "UPDATE ENTRY" : "QUICK ENTRY"}
              </span>

              <h2>{editingId ? "Edit Time Log" : "Quick Time Log"}</h2>

              <p>
                {editingId
                  ? "Update the details of this time log."
                  : "Record how much time you spent on a life area."}
              </p>
            </div>
          </div>
        </div>

        <form onSubmit={handleSubmit(handleFormSubmit)} noValidate>
          <div className="time-logs-form-grid">
            <div className="form-group time-logs-field-wide">
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
              <label htmlFor="durationMinutes">Duration</label>

              <div className="time-logs-duration-wrapper">
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

                <span>min</span>
              </div>

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

            <div className="form-group time-logs-note-field">
              <label htmlFor="note">Note</label>

              <div className="time-logs-note-wrapper">
                <FileText size={16} strokeWidth={1.8} />

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
              </div>

              {errors.note && (
                <p className="form-error">{errors.note.message}</p>
              )}
            </div>
          </div>

          <div className="time-logs-form-actions">
            <button
              type="submit"
              className="primary-button"
              disabled={submitting || lifeAreas.length === 0}
            >
              {submitting ? (
                <>
                  <Timer size={16} strokeWidth={2} className="time-logs-spin" />
                  {editingId ? "Updating..." : "Logging..."}
                </>
              ) : (
                <>
                  {editingId ? (
                    <Pencil size={16} strokeWidth={1.9} />
                  ) : (
                    <Plus size={16} strokeWidth={1.9} />
                  )}

                  {editingId ? "Update Time Log" : "Log Time"}
                </>
              )}
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
      </motion.section>

      <section className="time-logs-list-section">
        <div className="time-logs-list-header">
          <div>
            <span className="time-logs-section-label">WEEKLY JOURNAL</span>
            <h2>This Week's Time Logs</h2>
            <p>Review the time you've recorded across your life areas.</p>
          </div>

          <div className="time-logs-count">
            <strong>{timeLogs.length}</strong>
            <span>{timeLogs.length === 1 ? "entry" : "entries"}</span>
          </div>
        </div>

        {timeLogs.length === 0 ? (
          <div className="time-logs-empty">
            <div className="time-logs-empty-icon">
              <Timer size={22} strokeWidth={1.8} />
            </div>

            <h3>No time logs yet</h3>

            <p>
              Use the form above to record your first time entry for this week.
            </p>
          </div>
        ) : (
          <div className="time-logs-grid">
            {timeLogs.map((timeLog, index) => (
              <motion.article
                className="time-log-card"
                key={timeLog.id}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{
                  duration: 0.25,
                  delay: index * 0.04,
                }}
              >
                <div className="time-log-card-header">
                  <div className="time-log-card-title">
                    <span className="time-log-card-icon">
                      <Clock3 size={17} strokeWidth={1.8} />
                    </span>

                    <div>
                      <h3>{timeLog.lifeAreaName}</h3>
                      <p>{timeLog.logDate}</p>
                    </div>
                  </div>

                  <span className="time-log-source-badge">
                    {timeLog.source}
                  </span>
                </div>

                <div className="time-log-duration">
                  <span>Duration</span>
                  <strong>{formatDuration(timeLog.durationMinutes)}</strong>
                </div>

                <div className="time-log-details">
                  <div>
                    <span>Minutes</span>
                    <strong>{timeLog.durationMinutes}</strong>
                  </div>

                  <div>
                    <span>Logged</span>
                    <strong>
                      {new Date(timeLog.createdAt).toLocaleTimeString([], {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </strong>
                  </div>
                </div>

                {timeLog.note && (
                  <div className="time-log-note">
                    <FileText size={15} strokeWidth={1.8} />
                    <p>{timeLog.note}</p>
                  </div>
                )}

                <div className="time-log-actions">
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => handleEdit(timeLog)}
                  >
                    <Pencil size={15} strokeWidth={1.8} />
                    Edit
                  </button>

                  <button
                    type="button"
                    className="danger-button"
                    onClick={() => handleDelete(timeLog.id)}
                  >
                    <Trash2 size={15} strokeWidth={1.8} />
                    Delete
                  </button>
                </div>
              </motion.article>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}

export default TimeLogsPage;
