import React, { useState } from "react";
import Login from "./components/Login";
import Dashboard from "./components/Dashboard";
import AdminPanel from "./components/AdminPanel";
import "./App.css";

function App() {
  const [user, setUser] = useState(() => {
    const token = localStorage.getItem("fincore_token");
    const username = localStorage.getItem("fincore_username");
    const role = localStorage.getItem("fincore_role");
    return token ? { username, role } : null;
  });

  const handleLogin = (authResponse) => {
    localStorage.setItem("fincore_token", authResponse.token);
    localStorage.setItem("fincore_username", authResponse.username);
    localStorage.setItem("fincore_role", authResponse.role);
    setUser({ username: authResponse.username, role: authResponse.role });
  };

  const handleLogout = () => {
    localStorage.clear();
    setUser(null);
  };

  if (!user) {
    return <Login onLogin={handleLogin} />;
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <h1>FinCore</h1>
        <div className="header-right">
          <span>{user.username} ({user.role})</span>
          <button onClick={handleLogout}>Logout</button>
        </div>
      </header>

      {user.role === "ADMIN" ? <AdminPanel /> : <Dashboard />}
    </div>
  );
}

export default App;
