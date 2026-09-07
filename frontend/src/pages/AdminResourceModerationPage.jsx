import { useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { getStoredUser } from '../utils/appData'

export default function AdminResourceModerationPage() {
  const navigate = useNavigate()
  const [user] = useState(() => getStoredUser())
  const [flags, setFlags] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user || user.userType !== 'admin') return
    fetch(`/api/admin/resource-flags?adminId=${user.id}`)
      .then(async (response) => {
        if (!response.ok) throw new Error(await response.text() || 'Unable to load moderation queue.')
        return response.json()
      })
      .then(setFlags)
      .catch((err) => setError(err.message))
  }, [user])

  if (!user) return <Navigate to="/" replace />
  if (user.userType !== 'admin') return <Navigate to="/dashboard" replace />

  const dismiss = async (flagId) => {
    const response = await fetch(`/api/admin/resource-flags/${flagId}/dismiss?adminId=${user.id}`, { method: 'POST' })
    if (response.ok) setFlags((current) => current.filter((flag) => flag.id !== flagId))
  }

  const remove = async (resourceId) => {
    const response = await fetch(`/api/admin/resources/${resourceId}?adminId=${user.id}`, { method: 'DELETE' })
    if (response.ok) setFlags((current) => current.filter((flag) => flag.resourceId !== resourceId))
  }

  return (
    <main className="admin-moderation-page dashboard-canvas">
      <header className="admin-page-header"><div><span className="mini-label">Administrator workspace</span><h1>Resource moderation</h1><p>Review community reports before they affect the resource library.</p></div><button type="button" className="secondary-action-button" onClick={() => navigate('/dashboard')}>Back to dashboard</button></header>
      {error && <p className="questions-error">{error}</p>}
      <section className="admin-queue glass-panel">
        <div className="admin-queue-heading"><h2>Open reports</h2><span>{flags.length} awaiting review</span></div>
        {flags.length === 0 ? <div className="empty-state"><p>No open resource reports.</p></div> : flags.map((flag) => <article className="moderation-item" key={flag.id}><div><span className="question-category">{flag.reason}</span><h3>{flag.resourceTitle || `Resource #${flag.resourceId}`}</h3><p>Resource ID: {flag.resourceId} · Reporter ID: {flag.reporterId}</p><small>Submitted {new Date(flag.createdAt).toLocaleString()}</small></div><div className="moderation-actions"><button type="button" className="secondary-action-button" onClick={() => dismiss(flag.id)}>Dismiss</button><button type="button" className="danger-action-button" onClick={() => remove(flag.resourceId)}>Remove resource</button></div></article>)}
      </section>
    </main>
  )
}
