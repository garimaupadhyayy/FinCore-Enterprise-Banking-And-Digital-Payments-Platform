import React, { useEffect, useState } from "react";
import { accountAPI } from "../api";
import TransferForm from "./TransferForm";
import TransactionHistory from "./TransactionHistory";

function Dashboard() {
  const [accounts, setAccounts] = useState([]);
  const [selectedAccount, setSelectedAccount] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadAccounts = async () => {
    setLoading(true);
    try {
      const res = await accountAPI.myAccounts();
      setAccounts(res.data);
      if (res.data.length > 0) setSelectedAccount(res.data[0]);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAccounts();
  }, []);

  if (loading) return <div className="container">Loading your accounts...</div>;

  return (
    <div className="container">
      <div className="grid-2">
        <div className="card">
          <div>Account Number</div>
          <h3>{selectedAccount?.accountNumber || "—"}</h3>
          <div>Status: <span className="badge badge-success">{selectedAccount?.status}</span></div>
          <div style={{ marginTop: 16 }}>Available Balance</div>
          <div className="balance-amount">
            ₹{selectedAccount ? Number(selectedAccount.balance).toLocaleString("en-IN", { minimumFractionDigits: 2 }) : "0.00"}
          </div>
        </div>

        <div className="card">
          <h3 style={{ marginTop: 0 }}>Send Money</h3>
          <TransferForm
            fromAccountNumber={selectedAccount?.accountNumber}
            onTransferComplete={loadAccounts}
          />
        </div>
      </div>

      {selectedAccount && (
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Transaction History</h3>
          <TransactionHistory accountId={selectedAccount.id} />
        </div>
      )}
    </div>
  );
}

export default Dashboard;
