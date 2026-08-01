import axios from "axios";

const API_BASE_URL = "http://localhost:8080/api";

const api = axios.create({
  baseURL: API_BASE_URL,
});

// attach JWT token to every request automatically, once logged in
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("fincore_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const authAPI = {
  login: (username, password) => api.post("/auth/login", { username, password }),
  register: (data) => api.post("/auth/register", data),
};

export const accountAPI = {
  myAccounts: () => api.get("/accounts/my"),
  getByNumber: (accountNumber) => api.get(`/accounts/${accountNumber}`),
};

export const transactionAPI = {
  transfer: (fromAccountNumber, toAccountNumber, amount) =>
    api.post("/transactions/transfer", { fromAccountNumber, toAccountNumber, amount }),
  history: (accountNumber) => api.get(`/transactions/history/${accountNumber}`),
};

export const adminAPI = {
  pendingAccounts: () => api.get("/admin/accounts/pending"),
  approveAccount: (id) => api.put(`/admin/accounts/${id}/approve`),
  freezeAccount: (id) => api.put(`/admin/accounts/${id}/freeze`),
  flaggedTransactions: () => api.get("/admin/transactions/flagged"),
};

export default api;
