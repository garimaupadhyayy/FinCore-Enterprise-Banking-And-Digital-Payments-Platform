import React, { useEffect, useState } from "react";
import { adminAPI } from "../api";

function AdminPanel() {
  const [pending, setPending] = useState([]);
  const [flagged, setFlagged] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    setLoading(true);
    try {
      const [pendingRes, flaggedRes] = await Promise.all([
        adminAPI.pendingAccounts(),
        adminAPI.flaggedTransactions(),
      ]);
      setPending(pendingRes.data);
      setFlagged(flaggedRes.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleApprove = async (id) => {
    await adminAPI.approveAccount(id);
    loadData();
  };

  const handleFreeze = async (id) => {
    await adminAPI.freezeAccount(id);
    loadData();
  };

  if (loading) return <div className="container">Loading admin panel...</div>;

  return (
    <div className="container">
      <div className="card">
        <h3 style={{ marginTop: 0 }}>Pending Account Approvals</h3>
        {pending.length === 0 ? (
          <p style={{ color: "#667085" }}>No pending approvals.</p>
        ) : (
          <table>
            <thead>
              <tr><th>Account #</th><th>Type</th><th>Customer</th><th>Status</th><th>Action</th></tr>
            </thead>
            <tbody>
              {pending.map((a) => (
                <tr key={a.id}>
                  <td>{a.accountNumber}</td>
                  <td>{a.accountType}</td>
                  <td>{a.customer?.fullName}</td>
                  <td><span className="badge badge-pending">{a.status}</span></td>
                  <td>
                    <button className="primary" onClick={() => handleApprove(a.id)}>Approve</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <h3 style={{ marginTop: 0 }}>Fraud-Flagged Transactions</h3>
        {flagged.length === 0 ? (
          <p style={{ color: "#667085" }}>No flagged transactions.</p>
        ) : (
          <table>
            <thead>
              <tr><th>Date</th><th>From</th><th>To</th><th>Amount</th><th>Action</th></tr>
            </thead>
            <tbody>
              {flagged.map((t) => (
                <tr key={t.id}>
                  <td>{new Date(t.timestamp).toLocaleString()}</td>
                  <td>{t.fromAccount?.accountNumber}</td>
                  <td>{t.toAccount?.accountNumber}</td>
                  <td>₹{Number(t.amount).toLocaleString("en-IN")}</td>
                  <td>
                    {t.fromAccount && (
                      <button className="primary" onClick={() => handleFreeze(t.fromAccount.id)}>
                        Freeze Sender
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

export default AdminPanel;
