import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import {
  createLifeArea,
  deleteLifeArea,
  getLifeAreas,
  updateLifeArea,
} from "../api/lifeAreaApi";

function LifeAreasPage() {
  const [lifeAreas, setLifeAreas] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingLifeArea, setEditingLifeArea] = useState(null);
  const [deletingLifeAreaId, setDeletingLifeAreaId] = useState(null);
  const [serverError, setServerError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: {
      name: "",
      weight: "",
    },
  });

  const loadLifeAreas = async () => {
    try {
      setIsLoading(true);
      setServerError("");

      const data = await getLifeAreas();
      setLifeAreas(data);
    } catch {
      setServerError("Unable to load life areas.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    let isMounted = true;

    async function loadInitialLifeAreas() {
      try {
        setIsLoading(true);
        setServerError("");

        const data = await getLifeAreas();

        if (isMounted) {
          setLifeAreas(data);
        }
      } catch {
        if (isMounted) {
          setServerError("Unable to load life areas.");
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    loadInitialLifeAreas();

    return () => {
      isMounted = false;
    };
  }, []);

  const openCreateForm = () => {
    setEditingLifeArea(null);
    setServerError("");
    setSuccessMessage("");

    reset({
      name: "",
      weight: "",
    });

    setIsFormOpen(true);
  };

  const openEditForm = (lifeArea) => {
    setEditingLifeArea(lifeArea);
    setServerError("");
    setSuccessMessage("");

    reset({
      name: lifeArea.name,
      weight: lifeArea.weight,
    });

    setIsFormOpen(true);
  };

  const closeForm = () => {
    if (isSubmitting) {
      return;
    }

    setEditingLifeArea(null);
    setServerError("");

    reset({
      name: "",
      weight: "",
    });

    setIsFormOpen(false);
  };

  const onSubmit = async (data) => {
    setServerError("");
    setSuccessMessage("");

    const lifeAreaData = {
      name: data.name.trim(),
      weight: Number(data.weight),
    };

    try {
      if (editingLifeArea) {
        await updateLifeArea(editingLifeArea.id, lifeAreaData);

        await loadLifeAreas();

        setEditingLifeArea(null);
        setIsFormOpen(false);

        reset({
          name: "",
          weight: "",
        });

        setSuccessMessage("Life area updated successfully.");
      } else {
        await createLifeArea(lifeAreaData);

        await loadLifeAreas();

        setIsFormOpen(false);

        reset({
          name: "",
          weight: "",
        });

        setSuccessMessage("Life area created successfully.");
      }
    } catch (error) {
      setServerError(
        error.response?.data?.message ||
          `Unable to ${
            editingLifeArea ? "update" : "create"
          } life area. Please try again.`,
      );
    }
  };

  const handleDelete = async (lifeArea) => {
    const confirmed = window.confirm(
      `Are you sure you want to delete "${lifeArea.name}"?`,
    );

    if (!confirmed) {
      return;
    }

    setServerError("");
    setSuccessMessage("");
    setDeletingLifeAreaId(lifeArea.id);

    try {
      await deleteLifeArea(lifeArea.id);

      await loadLifeAreas();

      setSuccessMessage("Life area deleted successfully.");
    } catch (error) {
      setServerError(
        error.response?.data?.message ||
          "Unable to delete life area. Please try again.",
      );
    } finally {
      setDeletingLifeAreaId(null);
    }
  };

  return (
    <main className="app-shell">
      <section className="life-areas-page">
        <div className="life-areas-header">
          <div>
            <p className="eyebrow">WeekAhead</p>

            <h1>Life Areas</h1>

            <p className="page-description">
              Organize the important areas of your life and define their
              importance.
            </p>
          </div>

          {!isFormOpen && (
            <button
              type="button"
              className="primary-button"
              onClick={openCreateForm}
              disabled={deletingLifeAreaId !== null}
            >
              + Add Life Area
            </button>
          )}
        </div>

        {isFormOpen && (
          <div className="life-area-form-card">
            <div className="life-area-form-header">
              <div>
                <h2>{editingLifeArea ? "Edit Life Area" : "Add Life Area"}</h2>

                <p>
                  {editingLifeArea
                    ? "Update the life area and its importance weight."
                    : "Create a life area and assign its importance weight."}
                </p>
              </div>
            </div>

            <form onSubmit={handleSubmit(onSubmit)} noValidate>
              <div className="form-field">
                <label htmlFor="life-area-name">Name</label>

                <input
                  id="life-area-name"
                  type="text"
                  placeholder="e.g. Health"
                  autoComplete="off"
                  {...register("name", {
                    required: "Name is required.",
                    validate: (value) =>
                      value.trim().length > 0 || "Name cannot be empty.",
                  })}
                />

                {errors.name && (
                  <p className="form-error">{errors.name.message}</p>
                )}
              </div>

              <div className="form-field">
                <label htmlFor="life-area-weight">Importance weight</label>

                <input
                  id="life-area-weight"
                  type="number"
                  min="1"
                  step="1"
                  placeholder="e.g. 5"
                  {...register("weight", {
                    required: "Importance weight is required.",
                    validate: (value) => {
                      const weight = Number(value);

                      if (!Number.isInteger(weight)) {
                        return "Importance weight must be a whole number.";
                      }

                      if (weight <= 0) {
                        return "Importance weight must be greater than 0.";
                      }

                      return true;
                    },
                  })}
                />

                {errors.weight && (
                  <p className="form-error">{errors.weight.message}</p>
                )}
              </div>

              {serverError && <p className="form-error">{serverError}</p>}

              <div className="life-area-form-actions">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={closeForm}
                  disabled={isSubmitting}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="primary-button"
                  disabled={isSubmitting}
                >
                  {isSubmitting
                    ? editingLifeArea
                      ? "Updating..."
                      : "Creating..."
                    : editingLifeArea
                      ? "Update Life Area"
                      : "Create Life Area"}
                </button>
              </div>
            </form>
          </div>
        )}

        {successMessage && (
          <div className="life-areas-success">
            <p>{successMessage}</p>
          </div>
        )}

        {serverError && !isFormOpen && (
          <div className="life-areas-status life-areas-status-error">
            <p>{serverError}</p>
          </div>
        )}

        {isLoading ? (
          <div className="life-areas-status">
            <p>Loading life areas...</p>
          </div>
        ) : lifeAreas.length === 0 ? (
          <div className="life-areas-empty">
            <h2>No life areas yet</h2>

            <p>
              Add your first life area to start organizing your weekly time
              priorities.
            </p>

            {!isFormOpen && (
              <button
                type="button"
                className="primary-button"
                onClick={openCreateForm}
                disabled={deletingLifeAreaId !== null}
              >
                + Add Life Area
              </button>
            )}
          </div>
        ) : (
          <div className="life-areas-list">
            {lifeAreas.map((lifeArea) => {
              const isDeleting = deletingLifeAreaId === lifeArea.id;

              return (
                <article className="life-area-card" key={lifeArea.id}>
                  <div className="life-area-content">
                    <h2>{lifeArea.name}</h2>

                    <p>
                      Importance weight: <strong>{lifeArea.weight}</strong>
                    </p>
                  </div>

                  <div className="life-area-actions">
                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => openEditForm(lifeArea)}
                      disabled={isFormOpen || deletingLifeAreaId !== null}
                    >
                      Edit
                    </button>

                    <button
                      type="button"
                      className="danger-button"
                      onClick={() => handleDelete(lifeArea)}
                      disabled={isFormOpen || deletingLifeAreaId !== null}
                    >
                      {isDeleting ? "Deleting..." : "Delete"}
                    </button>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>
    </main>
  );
}

export default LifeAreasPage;
