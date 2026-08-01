import React, { useEffect, useState } from "react";
import { transactionAPI, accountAPI } from "../api";

function TransactionHistory({ accountId }) {
  const [transactions, setTransactions] = useState([]);

  useEffect(() => {
    const load = async () => {
      try {
        const accRes = await accountAPI.myAccounts();
        const account = accRes.data.find((a) => a.id === accountId);
        if (!account) return;
        const res = await transactionAPI.history(account.accountNumber);
        setTransactions(res.data);
      } catch (err) {
        console.error(err);
      }
    };
    load();
  }, [accountId]);

  if (transactions.length === 0) {
    return <p style={{ color: "#667085" }}>No transactions yet.</p>;
  }

  return (
    <table>
      <thead>
        <tr>
          <th>Date</th>
          <th>Type</th>
          <th>From</th>
          <th>To</th>
          <th>Amount</th>
          <th>Status</th>
        </tr>
      </thead>
      <tbody>
        {transactions.map((t) => (
          <tr key={t.id}>
            <td>{new Date(t.timestamp).toLocaleString()}</td>
            <td>{t.type}</td>
            <td>{t.fromAccount?.accountNumber || "—"}</td>
            <td>{t.toAccount?.accountNumber || "—"}</td>
            <td>₹{Number(t.amount).toLocaleString("en-IN", { minimumFractionDigits: 2 })}</td>
            <td>
              {t.flaggedFraud ? (
                <span className="badge badge-fraud">Flagged</span>
              ) : (
                <span className="badge badge-success">{t.status}</span>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export default TransactionHistory;
