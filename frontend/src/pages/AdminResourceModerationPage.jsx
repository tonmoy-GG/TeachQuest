import { useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { getStoredUser } from '../utils/appData'

export default function AdminResourceModerationPage() {
  const navigate = useNavigate()
  const [user] = useState(() => getStoredUser())
  const [flags, setFlags] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  const loadFlags = async () => {
    if (!user || user.userType !== 'admin') return

    try {
      const response = await fetch(`/api/admin/resource-flags?adminId=${user.id}`)
      const payload = await response.text()
      if (!response.ok) throw new Error(payload || 'Unable to load moderation queue.')
      const data = payload ? JSON.parse(payload) : []
      setFlags(Array.isArray(data) ? data : [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadFlags()
  }, [user])

  if (!user) return <Navigate to="/" replace />
  if (user.userType !== 'admin') return <Navigate to="/dashboard" replace />

  const dismiss = async (flagId) => {
    try {
      const response = await fetch(`/api/admin/resource-flags/${flagId}/dismiss?adminId=${user.id}`, { method: 'POST' })
      const payload = await response.text()
      if (!response.ok) throw new Error(payload || 'Unable to dismiss report.')
      setFlags((current) => current.filter((flag) => flag.id !== flagId))
    } catch (err) {
      setError(err.message)
    }
  }

  const remove = async (resourceId) => {
    try {
      const response = await fetch(`/api/admin/resources/${resourceId}?adminId=${user.id}`, { method: 'DELETE' })
      const payload = await response.text()
      if (!response.ok) throw new Error(payload || 'Unable to remove resource.')
      setFlags((current) => current.filter((flag) => flag.resourceId !== resourceId))
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <main className="admin-dashboard-shell">
      <header className="admin-dashboard-header">
        <div className="admin-brand">
          <div className="admin-brand-mark">TQ</div>
          <div>
            <span>TeachQuest</span>
            <small>Administration workspace</small>
          </div>
        </div>

        <div className="admin-header-actions">
          <span className="admin-user-chip"><span className="material-symbols-outlined">shield_person</span>{user.username}</span>
          <button type="button" className="secondary-action-button" onClick={() => navigate('/admin')}>Back to dashboard</button>
          <button
            type="button"
            className="logout-button"
            onClick={() => {
              localStorage.removeItem('teachquest_user')
              navigate('/')
            }}
          >
            Logout
          </button>
        </div>
      </header>

      <div className="admin-dashboard-layout">
        <aside className="admin-sidebar">
          <span className="admin-kicker">Control center</span>
          <h1>Keep the learning network healthy.</h1>
          <p>Review flagged resources before they affect the study library or community trust.</p>

          <nav className="admin-view-nav" aria-label="Admin sections">
            <button type="button" onClick={() => navigate('/admin')}><span className="material-symbols-outlined">monitoring</span>Analytics overview</button>
            <button type="button" onClick={() => navigate('/admin')}><span className="material-symbols-outlined">manage_accounts</span>User management</button>
            <button type="button" className="active"><span className="material-symbols-outlined">flag</span>Moderation queue</button>
          </nav>
        </aside>

        <section className="admin-content">
          <div className="admin-section-heading">
            <div>
              <span className="admin-kicker">Moderator queue</span>
              <h2>Resource moderation</h2>
            </div>
            <span className="admin-updated"><span className="material-symbols-outlined">flag</span>{flags.length} awaiting review</span>
          </div>

          {error && <div className="admin-alert" role="alert">{error}<button type="button" onClick={() => setError('')} aria-label="Dismiss error"><span className="material-symbols-outlined">close</span></button></div>}

          <section className="admin-users-panel">
            {loading ? <div className="admin-loading">Loading reports...</div> : flags.length === 0 ? <div className="admin-empty-cell">No open resource reports.</div> : flags.map((flag) => (
              <article className="moderation-item" key={flag.id}>
                <div>
                  <span className="question-category">{flag.reason}</span>
                  <h3>{flag.resourceTitle || `Resource #${flag.resourceId}`}</h3>
                  <p>Resource ID: {flag.resourceId} · Reporter ID: {flag.reporterId}</p>
                  <small>Submitted {flag.createdAt ? new Date(flag.createdAt).toLocaleString() : 'recently'}</small>
                </div>

                <div className="moderation-actions">
                  <button type="button" className="secondary-action-button" onClick={() => dismiss(flag.id)}>Dismiss</button>
                  <button type="button" className="danger-action-button" onClick={() => remove(flag.resourceId)}>Remove resource</button>
                </div>
              </article>
            ))}
          </section>
        </section>
      </div>
    </main>
  )
}
