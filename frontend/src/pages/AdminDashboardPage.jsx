import { useCallback, useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { getStoredUser, parseResponse } from '../utils/appData'
import './AdminDashboardPage.css'

const API = '/api/admin'

function adminHeaders(user) {
  return { 'X-User-Id': String(user.id) }
}

function MetricCard({ label, value, detail, icon, accent }) {
  return (
    <article className={`admin-metric-card ${accent || ''}`}>
      <div className="admin-metric-icon"><span className="material-symbols-outlined">{icon}</span></div>
      <div>
        <p>{label}</p>
        <strong>{value}</strong>
        <small>{detail}</small>
      </div>
    </article>
  )
}

function ChartPanel({ title, eyebrow, children }) {
  return (
    <section className="admin-chart-panel">
      <div className="admin-panel-heading"><div><span>{eyebrow}</span><h2>{title}</h2></div></div>
      <div className="admin-chart-area">{children}</div>
    </section>
  )
}

function UserDetailModal({ detail, onClose }) {
  if (!detail) return null
  const profile = detail.profile
  return (
    <div className="admin-modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="admin-detail-modal" role="dialog" aria-modal="true" aria-labelledby="user-detail-title" onMouseDown={(event) => event.stopPropagation()}>
        <div className="admin-modal-header">
          <div><span className="admin-kicker">User profile</span><h2 id="user-detail-title">{profile.name}</h2><p>{profile.email}</p></div>
          <button type="button" className="icon-button" aria-label="Close user details" onClick={onClose}><span className="material-symbols-outlined">close</span></button>
        </div>
        <div className="admin-profile-grid">
          <div><span>Role</span><strong>{profile.role}</strong></div>
          <div><span>Status</span><strong>{profile.status}</strong></div>
          <div><span>Points</span><strong>{detail.totalPoints}</strong></div>
          <div><span>Average rating</span><strong>{detail.averageRating == null ? 'Not rated' : Number(detail.averageRating).toFixed(1)}</strong></div>
          <div><span>University ID</span><strong>{profile.universityId || 'Not provided'}</strong></div>
          <div><span>Contact</span><strong>{profile.contactNo || 'Not provided'}</strong></div>
        </div>
        <div className="admin-detail-columns">
          <div><h3>Sessions taught</h3>{detail.sessionsTaught.length === 0 ? <p className="admin-muted">No tutoring sessions.</p> : detail.sessionsTaught.slice(0, 5).map((session) => <div className="admin-detail-row" key={`taught-${session.id}`}><span>{session.courseCode}</span><b>{session.status}</b></div>)}</div>
          <div><h3>Sessions taken</h3>{detail.sessionsTaken.length === 0 ? <p className="admin-muted">No attended sessions.</p> : detail.sessionsTaken.slice(0, 5).map((session) => <div className="admin-detail-row" key={`taken-${session.id}`}><span>{session.courseCode}</span><b>{session.status}</b></div>)}</div>
          <div><h3>Resources uploaded</h3>{detail.resourcesUploaded.length === 0 ? <p className="admin-muted">No resources uploaded.</p> : detail.resourcesUploaded.slice(0, 5).map((resource) => <div className="admin-detail-row" key={resource.id}><span>{resource.courseCode}</span><b>{resource.moderationStatus}</b></div>)}</div>
        </div>
      </section>
    </div>
  )
}

export default function AdminDashboardPage() {
  const navigate = useNavigate()
  const [user] = useState(() => getStoredUser())
  const [view, setView] = useState('analytics')
  const [overview, setOverview] = useState(null)
  const [registrations, setRegistrations] = useState([])
  const [demand, setDemand] = useState([])
  const [users, setUsers] = useState({ content: [], totalPages: 0, totalElements: 0, number: 0 })
  const [filters, setFilters] = useState({ role: '', status: '', search: '' })
  const [page, setPage] = useState(0)
  const [detail, setDetail] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  const request = useCallback(async (url, options = {}) => {
    const response = await fetch(url, { ...options, headers: { ...adminHeaders(user), ...(options.headers || {}) } })
    const data = await parseResponse(response)
    if (!response.ok) throw new Error(typeof data === 'string' ? data : data?.message || 'Admin request failed.')
    return data
  }, [user])

  useEffect(() => {
    if (!user || user.userType !== 'admin') return
    Promise.all([
      request(`${API}/analytics/overview`),
      request(`${API}/analytics/registrations`),
      request(`${API}/analytics/tutoring-demand?limit=5`),
    ]).then(([nextOverview, nextRegistrations, nextDemand]) => {
      setOverview(nextOverview)
      setRegistrations(nextRegistrations)
      setDemand(nextDemand)
    }).catch((err) => setError(err.message)).finally(() => setLoading(false))
  }, [user, request])

  useEffect(() => {
    if (!user || user.userType !== 'admin' || view !== 'users') return
    const params = new URLSearchParams({ page: String(page), size: '12' })
    Object.entries(filters).forEach(([key, value]) => { if (value) params.set(key, value) })
    request(`${API}/users?${params.toString()}`).then(setUsers).catch((err) => setError(err.message))
  }, [user, view, page, filters, request])

  if (!user) return <Navigate to="/" replace />
  if (user.userType !== 'admin') return <Navigate to="/dashboard" replace />

  const loadDetail = async (id) => {
    try { setDetail(await request(`${API}/users/${id}`)) } catch (err) { setError(err.message) }
  }

  const changeStatus = async (selectedUser) => {
    const nextStatus = selectedUser.status === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED'
    const action = nextStatus === 'SUSPENDED' ? 'suspend' : 'reactivate'
    if (!window.confirm(`Are you sure you want to ${action} ${selectedUser.name}?`)) return
    try {
      await request(`${API}/users/${selectedUser.id}/status`, { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ status: nextStatus }) })
      setUsers((current) => ({ ...current, content: current.content.map((item) => item.id === selectedUser.id ? { ...item, status: nextStatus } : item) }))
    } catch (err) { setError(err.message) }
  }

  const changeRole = async (selectedUser, role) => {
    if (role === selectedUser.role || !window.confirm(`Change ${selectedUser.name}'s role to ${role}?`)) return
    try {
      await request(`${API}/users/${selectedUser.id}/role`, { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ role }) })
      setUsers((current) => ({ ...current, content: current.content.map((item) => item.id === selectedUser.id ? { ...item, role } : item) }))
    } catch (err) { setError(err.message) }
  }

  return (
    <main className="admin-dashboard-shell">
      <header className="admin-dashboard-header">
        <div className="admin-brand"><div className="admin-brand-mark">TQ</div><div><span>TeachQuest</span><small>Administration workspace</small></div></div>
        <div className="admin-header-actions"><span className="admin-user-chip"><span className="material-symbols-outlined">shield_person</span>{user.username}</span><button type="button" className="secondary-action-button" onClick={() => navigate('/dashboard')}>Exit admin</button></div>
      </header>
      <div className="admin-dashboard-layout">
        <aside className="admin-sidebar">
          <span className="admin-kicker">Control center</span>
          <h1>Keep the learning network healthy.</h1>
          <p>Monitor activity, resolve risk, and make account decisions from one place.</p>
          <nav className="admin-view-nav" aria-label="Admin sections">
            <button className={view === 'analytics' ? 'active' : ''} type="button" onClick={() => setView('analytics')}><span className="material-symbols-outlined">monitoring</span>Analytics overview</button>
            <button className={view === 'users' ? 'active' : ''} type="button" onClick={() => setView('users')}><span className="material-symbols-outlined">manage_accounts</span>User management</button>
            <button type="button" onClick={() => navigate('/admin/resources')}><span className="material-symbols-outlined">flag</span>Moderation queue</button>
          </nav>
        </aside>
        <section className="admin-content">
          {error && <div className="admin-alert" role="alert">{error}<button type="button" onClick={() => setError('')} aria-label="Dismiss error"><span className="material-symbols-outlined">close</span></button></div>}
          {view === 'analytics' ? <>
            <div className="admin-section-heading"><div><span className="admin-kicker">Live snapshot</span><h2>Analytics overview</h2></div><span className="admin-updated"><span className="material-symbols-outlined">autorenew</span>Updated just now</span></div>
            {loading ? <div className="admin-loading">Loading network metrics...</div> : <>
              <div className="admin-metric-grid">
                <MetricCard label="Registered users" value={overview?.totalUsers ?? 0} detail={`${overview?.studentCount ?? 0} students · ${overview?.tutorCount ?? 0} tutors`} icon="groups" accent="coral" />
                <MetricCard label="Active tutors" value={overview?.activeTutorCount ?? 0} detail="Completed a session in 30 days" icon="school" accent="teal" />
                <MetricCard label="Sessions completed" value={overview?.completedSessionsThisMonth ?? 0} detail="This calendar month" icon="task_alt" accent="blue" />
                <MetricCard label="Resources uploaded" value={overview?.resourcesUploadedThisMonth ?? 0} detail="This calendar month" icon="library_add" accent="gold" />
                <MetricCard label="Open flags" value={overview?.unresolvedFlaggedContent ?? 0} detail="Awaiting moderation" icon="flag" accent="rose" />
                <MetricCard label="Tutor applications" value={overview?.pendingTutorApplications ?? 0} detail="Pending review" icon="pending_actions" accent="violet" />
              </div>
              <div className="admin-chart-grid">
                <ChartPanel eyebrow="Registration pulse" title="New users, last 8 weeks"><ResponsiveContainer width="100%" height="100%"><LineChart data={registrations} margin={{ top: 12, right: 12, left: -18, bottom: 0 }}><CartesianGrid strokeDasharray="3 3" stroke="#d9e1e4" vertical={false} /><XAxis dataKey="weekStart" tickFormatter={(value) => value.slice(5)} stroke="#71808a" tickLine={false} axisLine={false} /><YAxis allowDecimals={false} stroke="#71808a" tickLine={false} axisLine={false} /><Tooltip /><Line type="monotone" dataKey="registrations" stroke="#e46b54" strokeWidth={3} dot={{ r: 4, fill: '#e46b54' }} activeDot={{ r: 6 }} /></LineChart></ResponsiveContainer></ChartPanel>
                <ChartPanel eyebrow="Demand signals" title="Top courses by tutoring demand"><ResponsiveContainer width="100%" height="100%"><BarChart data={demand} layout="vertical" margin={{ top: 4, right: 16, left: 12, bottom: 0 }}><CartesianGrid strokeDasharray="3 3" stroke="#d9e1e4" horizontal={false} /><XAxis type="number" allowDecimals={false} stroke="#71808a" tickLine={false} axisLine={false} /><YAxis type="category" dataKey="courseCode" width={72} stroke="#71808a" tickLine={false} axisLine={false} /><Tooltip /><Bar dataKey="requests" fill="#168b83" radius={[0, 5, 5, 0]} barSize={22} /></BarChart></ResponsiveContainer></ChartPanel>
              </div>
            </>}
          </> : <>
            <div className="admin-section-heading"><div><span className="admin-kicker">Account controls</span><h2>User management</h2></div><span className="admin-updated">{users.totalElements || 0} registered accounts</span></div>
            <section className="admin-users-panel">
              <div className="admin-filter-bar"><label><span className="material-symbols-outlined">search</span><input value={filters.search} placeholder="Search name or email" onChange={(event) => { setPage(0); setFilters({ ...filters, search: event.target.value }) }} /></label><select value={filters.role} onChange={(event) => { setPage(0); setFilters({ ...filters, role: event.target.value }) }}><option value="">All roles</option><option value="student">Students</option><option value="teacher">Tutors</option><option value="admin">Admins</option></select><select value={filters.status} onChange={(event) => { setPage(0); setFilters({ ...filters, status: event.target.value }) }}><option value="">All statuses</option><option value="ACTIVE">Active</option><option value="SUSPENDED">Suspended</option></select></div>
              <div className="admin-table-wrap"><table className="admin-user-table"><thead><tr><th>User</th><th>Role</th><th>Registered</th><th>Status</th><th>Actions</th></tr></thead><tbody>{users.content.length === 0 ? <tr><td colSpan="5" className="admin-empty-cell">No users match these filters.</td></tr> : users.content.map((item) => <tr key={item.id}><td><button type="button" className="admin-user-name" onClick={() => loadDetail(item.id)}><strong>{item.name}</strong><span>{item.email}</span></button></td><td><select className="admin-role-select" value={item.role === 'tutor' ? 'teacher' : item.role} onChange={(event) => changeRole(item, event.target.value)}><option value="student">Student</option><option value="teacher">Tutor</option><option value="admin">Admin</option></select></td><td>{item.registrationDate ? new Date(item.registrationDate).toLocaleDateString() : 'Unknown'}</td><td><span className={`admin-status ${item.status.toLowerCase()}`}>{item.status}</span></td><td><button type="button" className="admin-row-action" onClick={() => changeStatus(item)}>{item.status === 'SUSPENDED' ? 'Reactivate' : 'Suspend'}</button></td></tr>)}</tbody></table></div>
              <div className="admin-pagination"><span>Page {users.number + 1} of {Math.max(users.totalPages, 1)}</span><div><button type="button" disabled={page === 0} onClick={() => setPage(page - 1)} aria-label="Previous page"><span className="material-symbols-outlined">chevron_left</span></button><button type="button" disabled={page + 1 >= users.totalPages} onClick={() => setPage(page + 1)} aria-label="Next page"><span className="material-symbols-outlined">chevron_right</span></button></div></div>
            </section>
          </>}
        </section>
      </div>
      <UserDetailModal detail={detail} onClose={() => setDetail(null)} />
    </main>
  )
}
