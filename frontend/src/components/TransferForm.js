import React, { useState } from "react";
import { transactionAPI } from "../api";

function TransferForm({ fromAccountNumber, onTransferComplete }) {
  const [toAccount, setToAccount] = useState("");
  const [amount, setAmount] = useState("");
  const [status, setStatus] = useState(null); // { type: 'success' | 'error', message }
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setStatus(null);
    setLoading(true);
    try {
      await transactionAPI.transfer(fromAccountNumber, toAccount, parseFloat(amount));
      setStatus({ type: "success", message: "Transfer completed successfully." });
      setToAccount("");
      setAmount("");
      onTransferComplete && onTransferComplete();
    } catch (err) {
      setStatus({ type: "error", message: err.response?.data?.message || "Transfer failed." });
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <label>Recipient Account Number</label>
      <input value={toAccount} onChange={(e) => setToAccount(e.target.value)} required />

      <label>Amount (₹)</label>
      <input
        type="number"
        min="1"
        step="0.01"
        value={amount}
        onChange={(e) => setAmount(e.target.value)}
        required
      />

      {status && (
        <div className={status.type === "error" ? "error-text" : "error-text"}
             style={{ color: status.type === "error" ? "#c62828" : "#0d8a4c" }}>
          {status.message}
        </div>
      )}

      <button className="primary" type="submit" disabled={loading}>
        {loading ? "Processing..." : "Transfer"}
      </button>
    </form>
  );
}

export default TransferForm;
