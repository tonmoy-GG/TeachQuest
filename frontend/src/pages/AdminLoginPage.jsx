import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { getStoredUser, parseResponse, STORAGE_KEY } from '../utils/appData'
import './AdminLoginPage.css'

export default function AdminLoginPage() {
  const navigate = useNavigate()
  const currentUser = getStoredUser()
  const [form, setForm] = useState({ email: 'admin@teachquest.com', password: 'admin123' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  if (currentUser?.userType === 'admin') return <Navigate to="/admin" replace />

  const submit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form),
      })
      const data = await parseResponse(response)
      if (!response.ok) throw new Error(typeof data === 'string' ? data : data?.message || 'Unable to sign in.')
      if (data.userType !== 'admin') throw new Error('This sign-in is for administrators only.')
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        id: data.id,
        username: data.username || data.email?.split('@')[0],
        email: data.email,
        userType: data.userType,
        universityId: data.universityId || '',
        contactNo: data.contactNo || '',
        address: data.address || '',
        totalPoints: data.totalPoints || 0,
        status: data.status || 'ACTIVE',
      }))
      navigate('/admin')
    } catch (loginError) {
      setError(loginError.message || 'Unable to connect to the backend.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="admin-login-shell">
      <div className="admin-login-grid" />
      <section className="admin-login-card">
        <div className="admin-login-mark">TQ</div>
        <span className="admin-login-kicker">TeachQuest control center</span>
        <h1>Administrator sign in</h1>
        <p>Access analytics, account controls, and moderation tools.</p>
        <form onSubmit={submit}>
          <label htmlFor="admin-email">Admin email</label>
          <input id="admin-email" type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} required />
          <label htmlFor="admin-password">Password</label>
          <input id="admin-password" type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} required />
          {error && <div className="admin-login-error" role="alert">{error}</div>}
          <button type="submit" disabled={loading}>{loading ? 'Signing in...' : 'Sign in to admin'}</button>
        </form>
        <Link to="/">Return to standard sign in</Link>
      </section>
    </main>
  )
}
