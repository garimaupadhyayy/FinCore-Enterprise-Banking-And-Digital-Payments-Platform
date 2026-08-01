import React, { useState } from "react";
import { authAPI } from "../api";

function Login({ onLogin }) {
  const [isRegister, setIsRegister] = useState(false);
  const [form, setForm] = useState({
    username: "", password: "", fullName: "", email: "", accountType: "SAVINGS",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = isRegister
        ? await authAPI.register(form)
        : await authAPI.login(form.username, form.password);
      onLogin(res.data);
    } catch (err) {
      setError(err.response?.data?.message || "Something went wrong. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-shell">
      <div className="login-card">
        <h2>FinCore {isRegister ? "— Create Account" : "— Sign In"}</h2>
        <form onSubmit={handleSubmit}>
          <label>Username</label>
          <input name="username" value={form.username} onChange={handleChange} required />

          {isRegister && (
            <>
              <label>Full Name</label>
              <input name="fullName" value={form.fullName} onChange={handleChange} required />
              <label>Email</label>
              <input type="email" name="email" value={form.email} onChange={handleChange} required />
              <label>Account Type</label>
              <select name="accountType" value={form.accountType} onChange={handleChange}>
                <option value="SAVINGS">Savings</option>
                <option value="CURRENT">Current</option>
              </select>
            </>
          )}

          <label>Password</label>
          <input type="password" name="password" value={form.password} onChange={handleChange} required />

          {error && <div className="error-text">{error}</div>}

          <button className="primary" type="submit" disabled={loading} style={{ width: "100%" }}>
            {loading ? "Please wait..." : isRegister ? "Create Account" : "Login"}
          </button>
        </form>

        <div className="toggle-link" onClick={() => setIsRegister(!isRegister)}>
          {isRegister ? "Already have an account? Sign in" : "New here? Create an account"}
        </div>
      </div>
    </div>
  );
}

export default Login;
