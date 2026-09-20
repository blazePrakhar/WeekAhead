import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import {
  getLifeAreas,
  createLifeArea,
  updateLifeArea,
  deleteLifeArea,
} from "../api/lifeAreaApi";

const defaultValues = {
  name: "",
  description: "",
  weight: "",
  minMinutes: "",
  maxMinutes: "",
};

function LifeAreasPage() {
  const [lifeAreas, setLifeAreas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [editingId, setEditingId] = useState(null);

  const {
    register,
    handleSubmit,
    reset,
    getValues,
    formState: { errors },
  } = useForm({
    defaultValues,
  });

  useEffect(() => {
    let cancelled = false;

    const loadInitialLifeAreas = async () => {
      try {
        const data = await getLifeAreas();

        if (!cancelled) {
          setLifeAreas(data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err.response?.data?.message ||
              "Failed to load life areas. Please try again.",
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    loadInitialLifeAreas();

    return () => {
      cancelled = true;
    };
  }, []);

  const loadLifeAreas = async () => {
    try {
      setError("");

      const data = await getLifeAreas();
      setLifeAreas(data);
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Failed to load life areas. Please try again.",
      );
    }
  };

  const handleFormSubmit = async (formData) => {
    try {
      setSubmitting(true);
      setError("");
      setSuccess("");

      const payload = {
        name: formData.name.trim(),
        description: formData.description.trim(),
        weight: Number(formData.weight),
        minMinutes: Number(formData.minMinutes),
        maxMinutes: Number(formData.maxMinutes),
      };

      if (editingId) {
        await updateLifeArea(editingId, payload);
        setSuccess("Life area updated successfully.");
      } else {
        await createLifeArea(payload);
        setSuccess("Life area created successfully.");
      }

      reset(defaultValues);
      setEditingId(null);

      await loadLifeAreas();
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Failed to save life area. Please check your input.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = (lifeArea) => {
    setEditingId(lifeArea.id);
    setError("");
    setSuccess("");

    reset({
      name: lifeArea.name ?? "",
      description: lifeArea.description ?? "",
      weight: lifeArea.weight ?? "",
      minMinutes: lifeArea.minMinutes ?? "",
      maxMinutes: lifeArea.maxMinutes ?? "",
    });

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    reset(defaultValues);
    setError("");
    setSuccess("");
  };

  const handleArchive = async (id) => {
    const confirmed = window.confirm(
      "Are you sure you want to archive this life area?",
    );

    if (!confirmed) {
      return;
    }

    try {
      setError("");
      setSuccess("");

      await deleteLifeArea(id);

      if (editingId === id) {
        setEditingId(null);
        reset(defaultValues);
      }

      setSuccess("Life area archived successfully.");
      await loadLifeAreas();
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Failed to archive life area. Please try again.",
      );
    }
  };

  if (loading) {
    return (
      <main className="life-areas-page">
        <section className="life-areas-header">
          <div>
            <h1>Life Areas</h1>
            <p className="page-description">
              Define the areas of your life and configure their importance and
              time constraints.
            </p>
          </div>
        </section>

        <div className="life-areas-status">
          <p>Loading life areas...</p>
        </div>
      </main>
    );
  }

  return (
    <main className="life-areas-page">
      <section className="life-areas-header">
        <div>
          <h1>Life Areas</h1>
          <p className="page-description">
            Define the areas of your life and configure their importance and
            time constraints.
          </p>
        </div>
      </section>

      {success && (
        <div className="life-areas-success">
          <p>{success}</p>
        </div>
      )}

      <section className="life-area-form-card">
        <div className="life-area-form-header">
          <h2>{editingId ? "Edit Life Area" : "Create Life Area"}</h2>

          <p>
            Set the importance and time constraints for this area of your life.
          </p>
        </div>

        <form onSubmit={handleSubmit(handleFormSubmit)} noValidate>
          <div className="form-group">
            <label htmlFor="name">Name</label>

            <input
              id="name"
              type="text"
              placeholder="e.g. Work, Fitness, Learning"
              {...register("name", {
                required: "Name is required.",
                maxLength: {
                  value: 100,
                  message: "Name must not exceed 100 characters.",
                },
              })}
            />

            {errors.name && <p className="form-error">{errors.name.message}</p>}
          </div>

          <div className="form-group">
            <label htmlFor="description">Description</label>

            <textarea
              id="description"
              rows="3"
              placeholder="Describe this life area..."
              {...register("description", {
                maxLength: {
                  value: 500,
                  message: "Description must not exceed 500 characters.",
                },
              })}
            />

            {errors.description && (
              <p className="form-error">{errors.description.message}</p>
            )}
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="weight">Importance Weight</label>

              <input
                id="weight"
                type="number"
                step="any"
                min="0"
                placeholder="e.g. 5"
                {...register("weight", {
                  required: "Importance weight is required.",
                  validate: (value) =>
                    Number(value) > 0 ||
                    "Importance weight must be greater than 0.",
                })}
              />

              {errors.weight && (
                <p className="form-error">{errors.weight.message}</p>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="minMinutes">Minimum Minutes</label>

              <input
                id="minMinutes"
                type="number"
                min="0"
                step="1"
                placeholder="e.g. 120"
                {...register("minMinutes", {
                  required: "Minimum minutes is required.",
                  validate: (value) => {
                    const number = Number(value);

                    if (!Number.isInteger(number)) {
                      return "Minimum minutes must be a whole number.";
                    }

                    if (number < 0) {
                      return "Minimum minutes cannot be negative.";
                    }

                    return true;
                  },
                })}
              />

              {errors.minMinutes && (
                <p className="form-error">{errors.minMinutes.message}</p>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="maxMinutes">Maximum Minutes</label>

              <input
                id="maxMinutes"
                type="number"
                min="0"
                step="1"
                placeholder="e.g. 600"
                {...register("maxMinutes", {
                  required: "Maximum minutes is required.",
                  validate: (value) => {
                    const number = Number(value);
                    const minMinutes = Number(getValues("minMinutes"));

                    if (!Number.isInteger(number)) {
                      return "Maximum minutes must be a whole number.";
                    }

                    if (number < 0) {
                      return "Maximum minutes cannot be negative.";
                    }

                    if (number < minMinutes) {
                      return "Maximum minutes must be greater than or equal to minimum minutes.";
                    }

                    return true;
                  },
                })}
              />

              {errors.maxMinutes && (
                <p className="form-error">{errors.maxMinutes.message}</p>
              )}
            </div>
          </div>

          {error && <p className="form-error">{error}</p>}

          <div className="form-actions">
            <button
              type="submit"
              className="primary-button"
              disabled={submitting}
            >
              {submitting
                ? "Saving..."
                : editingId
                  ? "Update Life Area"
                  : "Create Life Area"}
            </button>

            {editingId && (
              <button
                type="button"
                className="secondary-button"
                onClick={handleCancelEdit}
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
          <h2>Your Life Areas</h2>
        </div>

        {lifeAreas.length === 0 ? (
          <div className="empty-state">
            <p>No life areas found.</p>
            <p>Create your first life area above.</p>
          </div>
        ) : (
          <div className="life-areas-grid">
            {lifeAreas.map((lifeArea) => (
              <article className="life-area-card" key={lifeArea.id}>
                <div className="life-area-card-header">
                  <div>
                    <h3>{lifeArea.name}</h3>

                    {lifeArea.description && (
                      <p className="life-area-description">
                        {lifeArea.description}
                      </p>
                    )}
                  </div>

                  {lifeArea.isActive === false && (
                    <span className="status-badge">Archived</span>
                  )}
                </div>

                <div className="life-area-details">
                  <div className="life-area-detail">
                    <span className="detail-label">Importance Weight</span>
                    <strong>{lifeArea.weight}</strong>
                  </div>

                  <div className="life-area-detail">
                    <span className="detail-label">Minimum</span>
                    <strong>{lifeArea.minMinutes} min</strong>
                  </div>

                  <div className="life-area-detail">
                    <span className="detail-label">Maximum</span>
                    <strong>{lifeArea.maxMinutes} min</strong>
                  </div>
                </div>

                <div className="life-area-actions">
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => handleEdit(lifeArea)}
                    disabled={lifeArea.isActive === false}
                  >
                    Edit
                  </button>

                  {lifeArea.isActive !== false && (
                    <button
                      type="button"
                      className="danger-button"
                      onClick={() => handleArchive(lifeArea.id)}
                    >
                      Archive
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}

export default LifeAreasPage;
