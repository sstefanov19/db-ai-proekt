async function request(path, { method = 'GET', body, headers } = {}) {
  const res = await fetch(path, {
    method,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(headers || {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 204) return null;

  const text = await res.text();
  const data = text ? JSON.parse(text) : null;

  if (!res.ok) {
    const err = new Error(data?.message || `HTTP ${res.status}`);
    err.status = res.status;
    err.payload = data;
    throw err;
  }
  return data;
}

function qs(params) {
  const usp = new URLSearchParams();
  Object.entries(params || {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') usp.set(k, v);
  });
  const s = usp.toString();
  return s ? `?${s}` : '';
}

export const api = {
  login: (email, password) =>
    request('/api/auth/login', { method: 'POST', body: { email, password } }),
  logout: () => request('/api/auth/logout', { method: 'POST' }),
  me: () => request('/api/auth/me'),

  teachers: () => request('/api/teachers'),

  slots: (params) => request(`/api/slots${qs(params)}`),
  createSlot: (body) => request('/api/slots', { method: 'POST', body }),
  deleteSlot: (id) => request(`/api/slots/${id}`, { method: 'DELETE' }),

  myBookings: () => request('/api/bookings/me'),
  book: (slotId) => request('/api/bookings', { method: 'POST', body: { slotId } }),
  cancelBooking: (id) => request(`/api/bookings/${id}`, { method: 'DELETE' }),

  adminUsers: () => request('/api/admin/users'),
  adminBookings: () => request('/api/admin/bookings'),
};
