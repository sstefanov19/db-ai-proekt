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

export default function TeacherDashboard() {
  const { user, logout } = useAuth();

  const [slots, setSlots] = useState([]);
  const [date, setDate] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [location, setLocation] = useState('');
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [busy, setBusy] = useState(false);

  async function load() {
    try {
      setSlots(await api.slots());
    } catch (e) {
      setError(e.message);
    }
  }

  useEffect(() => { load(); }, []);

  async function onCreate(e) {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    if (!date || !startTime || !endTime || !location.trim()) {
      setError('Всички полета са задължителни.');
      return;
    }
    const startAt = `${date}T${startTime}:00`;
    const endAt = `${date}T${endTime}:00`;
    if (endAt <= startAt) {
      setError('Краят трябва да е след началото.');
      return;
    }
    setBusy(true);
    try {
      await api.createSlot({ startAt, endAt, location: location.trim() });
      setSuccess('Слотът е създаден.');
      setDate(''); setStartTime(''); setEndTime(''); setLocation('');
      await load();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  async function onDelete(id) {
    setError(null);
    setSuccess(null);
    setBusy(true);
    try {
      await api.deleteSlot(id);
      setSuccess('Слотът е изтрит.');
      await load();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <div className="topbar">
        <h1>Преподавател: {user.fullName}</h1>
        <div>
          <span className="user">{user.email}</span>
          <button className="secondary" onClick={logout}>Изход</button>
        </div>
      </div>

      <div className="page">
        {error && <div className="msg error">{error}</div>}
        {success && <div className="msg success">{success}</div>}

        <div className="section">
          <h2>Нов слот за консултация</h2>
          <form className="form-row" onSubmit={onCreate}>
            <div className="field">
              <label>Дата</label>
              <input type="date" value={date} onChange={e => setDate(e.target.value)} />
            </div>
            <div className="field">
              <label>Начален час</label>
              <input type="time" value={startTime} onChange={e => setStartTime(e.target.value)} />
            </div>
            <div className="field">
              <label>Краен час</label>
              <input type="time" value={endTime} onChange={e => setEndTime(e.target.value)} />
            </div>
            <div className="field" style={{ flex: 2 }}>
              <label>Място</label>
              <input type="text" placeholder="Зала 301, Zoom..." value={location} onChange={e => setLocation(e.target.value)} />
            </div>
            <button type="submit" disabled={busy}>Създай</button>
          </form>
        </div>

        <div className="section">
          <h2>Моите слотове</h2>
          <table>
            <thead>
              <tr>
                <th>Начало</th>
                <th>Край</th>
                <th>Място</th>
                <th>Статус</th>
                <th>Записан студент</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {slots.map(s => (
                <tr key={s.id}>
                  <td>{fmt(s.startAt)}</td>
                  <td>{fmt(s.endAt)}</td>
                  <td>{s.location}</td>
                  <td><span className={`badge ${s.status}`}>{s.status}</span></td>
                  <td>{s.bookedByName || '-'}</td>
                  <td>
                    {s.status === 'AVAILABLE' && (
                      <button className="danger" disabled={busy} onClick={() => onDelete(s.id)}>Изтрий</button>
                    )}
                  </td>
                </tr>
              ))}
              {slots.length === 0 && <tr><td colSpan="6">Все още нямате слотове.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
