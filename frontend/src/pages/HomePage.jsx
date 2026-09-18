import { useAuth } from "../context/useAuth";

function HomePage() {
  const { currentUser, logout } = useAuth();

  return (
    <main className="app-shell">
      <section className="foundation-card">
        <p className="eyebrow">WeekAhead</p>
        <h1>Welcome to WeekAhead</h1>

        {currentUser && <p>Signed in as {currentUser.email}</p>}

        <p>Your authenticated application area is ready.</p>

        <button type="button" onClick={logout}>
          Logout
        </button>
      </section>
    </main>
  );
}

export default HomePage;
