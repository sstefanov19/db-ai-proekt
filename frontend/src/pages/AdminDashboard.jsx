import { useEffect, useState } from 'react';
import { useAuth } from '../auth/AuthContext.jsx';
import { api } from '../api.js';

function fmt(dt) {
  if (!dt) return '';
  return new Date(dt).toLocaleString('bg-BG', {
    dateStyle: 'short',
    timeStyle: 'short',
  });
}

export default function AdminDashboard() {
  const { user, logout } = useAuth();
  const [users, setUsers] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.adminUsers().then(setUsers).catch(e => setError(e.message));
    api.adminBookings().then(setBookings).catch(e => setError(e.message));
  }, []);

  return (
    <>
      <div className="topbar">
        <h1>Администратор: {user.fullName}</h1>
        <div>
          <span className="user">{user.email}</span>
          <button className="secondary" onClick={logout}>Изход</button>
        </div>
      </div>

      <div className="page">
        {error && <div className="msg error">{error}</div>}

        <div className="section">
          <h2>Всички потребители</h2>
          <table>
            <thead>
              <tr>
                <th>ID</th><th>Име</th><th>Email</th><th>Роля</th>
              </tr>
            </thead>
            <tbody>
              {users.map(u => (
                <tr key={u.id}>
                  <td>{u.id}</td>
                  <td>{u.fullName}</td>
                  <td>{u.email}</td>
                  <td>{u.role}</td>
                </tr>
              ))}
              {users.length === 0 && <tr><td colSpan="4">Няма потребители.</td></tr>}
            </tbody>
          </table>
        </div>

        <div className="section">
          <h2>Всички записвания</h2>
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Студент</th>
                <th>Преподавател</th>
                <th>Начало</th>
                <th>Място</th>
                <th>Статус</th>
                <th>Създадено</th>
              </tr>
            </thead>
            <tbody>
              {bookings.map(b => (
                <tr key={b.id}>
                  <td>{b.id}</td>
                  <td>{b.studentName}</td>
                  <td>{b.slot.teacherName}</td>
                  <td>{fmt(b.slot.startAt)}</td>
                  <td>{b.slot.location}</td>
                  <td><span className={`badge ${b.status}`}>{b.status}</span></td>
                  <td>{fmt(b.createdAt)}</td>
                </tr>
              ))}
              {bookings.length === 0 && <tr><td colSpan="7">Няма записвания.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
