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

export default function StudentDashboard() {
  const { user, logout } = useAuth();

  const [teachers, setTeachers] = useState([]);
  const [teacherId, setTeacherId] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [slots, setSlots] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [busy, setBusy] = useState(false);

  async function loadTeachers() {
    try {
      setTeachers(await api.teachers());
    } catch (e) {
      setError(e.message);
    }
  }

  async function loadSlots() {
    try {
      const params = {};
      if (teacherId) params.teacherId = teacherId;
      if (from) params.from = `${from}T00:00:00`;
      if (to) params.to = `${to}T23:59:59`;
      setSlots(await api.slots(params));
    } catch (e) {
      setError(e.message);
    }
  }

  async function loadBookings() {
    try {
      setBookings(await api.myBookings());
    } catch (e) {
      setError(e.message);
    }
  }

  useEffect(() => {
    loadTeachers();
    loadSlots();
    loadBookings();
    // eslint-disable-next-line
  }, []);

  async function onBook(slotId) {
    setError(null);
    setSuccess(null);
    setBusy(true);
    try {
      await api.book(slotId);
      setSuccess('Записването е успешно.');
      await Promise.all([loadSlots(), loadBookings()]);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  async function onCancel(bookingId) {
    setError(null);
    setSuccess(null);
    setBusy(true);
    try {
      await api.cancelBooking(bookingId);
      setSuccess('Записването е отменено.');
      await Promise.all([loadSlots(), loadBookings()]);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  const now = new Date();
  const upcoming = bookings.filter(b => b.status === 'ACTIVE' && new Date(b.slot.startAt) >= now);
  const past = bookings.filter(b => b.status !== 'ACTIVE' || new Date(b.slot.startAt) < now);

  return (
    <>
      <div className="topbar">
        <h1>Студент: {user.fullName}</h1>
        <div>
          <span className="user">{user.email}</span>
          <button className="secondary" onClick={logout}>Изход</button>
        </div>
      </div>

      <div className="page">
        {error && <div className="msg error">{error}</div>}
        {success && <div className="msg success">{success}</div>}

        <div className="section">
          <h2>Преподаватели</h2>
          <table>
            <thead><tr><th>Име</th><th>Email</th></tr></thead>
            <tbody>
              {teachers.map(t => (
                <tr key={t.id}><td>{t.fullName}</td><td>{t.email}</td></tr>
              ))}
              {teachers.length === 0 && <tr><td colSpan="2">Няма преподаватели.</td></tr>}
            </tbody>
          </table>
        </div>

        <div className="section">
          <h2>Свободни слотове</h2>
          <div className="form-row" style={{ marginBottom: '0.75rem' }}>
            <div className="field">
              <label>Преподавател</label>
              <select value={teacherId} onChange={e => setTeacherId(e.target.value)}>
                <option value="">Всички</option>
                {teachers.map(t => (
                  <option key={t.id} value={t.id}>{t.fullName}</option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>От дата</label>
              <input type="date" value={from} onChange={e => setFrom(e.target.value)} />
            </div>
            <div className="field">
              <label>До дата</label>
              <input type="date" value={to} onChange={e => setTo(e.target.value)} />
            </div>
            <div style={{ display: 'flex', gap: '0.4rem' }}>
              <button onClick={loadSlots}>Филтрирай</button>
              <button className="secondary" onClick={() => {
                setTeacherId(''); setFrom(''); setTo('');
                setTimeout(loadSlots, 0);
              }}>Изчисти</button>
            </div>
          </div>
          <table>
            <thead>
              <tr>
                <th>Преподавател</th>
                <th>Начало</th>
                <th>Край</th>
                <th>Място</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {slots.map(s => (
                <tr key={s.id}>
                  <td>{s.teacherName}</td>
                  <td>{fmt(s.startAt)}</td>
                  <td>{fmt(s.endAt)}</td>
                  <td>{s.location}</td>
                  <td>
                    <button disabled={busy} onClick={() => onBook(s.id)}>Запиши</button>
                  </td>
                </tr>
              ))}
              {slots.length === 0 && <tr><td colSpan="5">Няма свободни слотове.</td></tr>}
            </tbody>
          </table>
        </div>

        <div className="section">
          <h2>Моите записвания</h2>
          <h3 style={{ fontSize: '0.95rem', margin: '0.25rem 0' }}>Предстоящи</h3>
          <table>
            <thead>
              <tr>
                <th>Преподавател</th>
                <th>Начало</th>
                <th>Място</th>
                <th>Статус</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {upcoming.map(b => (
                <tr key={b.id}>
                  <td>{b.slot.teacherName}</td>
                  <td>{fmt(b.slot.startAt)}</td>
                  <td>{b.slot.location}</td>
                  <td><span className={`badge ${b.status}`}>{b.status}</span></td>
                  <td>
                    <button className="danger" disabled={busy} onClick={() => onCancel(b.id)}>Откажи</button>
                  </td>
                </tr>
              ))}
              {upcoming.length === 0 && <tr><td colSpan="5">Няма предстоящи записвания.</td></tr>}
            </tbody>
          </table>

          <h3 style={{ fontSize: '0.95rem', margin: '1rem 0 0.25rem 0' }}>Минали</h3>
          <table>
            <thead>
              <tr>
                <th>Преподавател</th>
                <th>Начало</th>
                <th>Място</th>
                <th>Статус</th>
              </tr>
            </thead>
            <tbody>
              {past.map(b => (
                <tr key={b.id}>
                  <td>{b.slot.teacherName}</td>
                  <td>{fmt(b.slot.startAt)}</td>
                  <td>{b.slot.location}</td>
                  <td><span className={`badge ${b.status}`}>{b.status}</span></td>
                </tr>
              ))}
              {past.length === 0 && <tr><td colSpan="4">Няма минали записвания.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
