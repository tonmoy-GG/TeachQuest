import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { findRegisteredUser, getStoredUser, parseResponse, saveRegisteredUsers, STORAGE_KEY } from '../utils/appData'

export default function LoginPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setLoading(true)

    try {
      const registeredUser = findRegisteredUser(form.email, form.password)

      if (registeredUser) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify({
          username: registeredUser.username,
          email: registeredUser.email,
          userType: registeredUser.userType,
          universityId: registeredUser.universityId,
          contactNo: registeredUser.contactNo,
          address: registeredUser.address,
        }))

        if (registeredUser.userType === 'teacher') {
          navigate('/teacher-dashboard')
        } else {
          navigate('/dashboard')
        }
        return
      }

      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: form.email,
          password: form.password,
        }),
      })

      const data = await parseResponse(response)

      if (!response.ok) {
        throw new Error(typeof data === 'string' ? data : data?.message || 'Login failed')
      }

      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        username: data.username || data.name || data.email?.split('@')[0],
        email: data.email,
        userType: data.userType || 'student',
        universityId: data.universityId || '',
        contactNo: data.contactNo || '',
        address: data.address || '',
      }))

      if ((data.userType || 'student') === 'teacher') {
        navigate('/teacher-dashboard')
      } else {
        navigate('/dashboard')
      }
    } catch (err) {
      setError(err.message || 'Unable to connect to the backend.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-shell">
      <div className="bg-grid" />
      <div className="bg-glow bg-glow-1" />
      <div className="bg-glow bg-glow-2" />

      <header className="login-topbar">
        <div className="brand-row-login">
          <div className="brand-mark-login">TQ</div>
          <span>TeachQuest</span>
        </div>

        <button type="button" className="help-button">
          <span className="material-symbols-outlined">help</span>
          Need help signing in?
        </button>
      </header>

      <div className="login-main">
        <section className="hero-column">
          <div className="hero-copy">
            <span className="hero-label">University peer learning network</span>
            <h1>
              Empowering students
              <span> through peer learning.</span>
            </h1>
            <p>
              Connect with skilled peer tutors, discover learning resources,
              and grow together in one collaborative platform.
            </p>
          </div>

          <div className="feature-grid">
            <div className="feature-card glass">
              <span className="material-symbols-outlined">menu_book</span>
              <div>
                <strong>Study resources</strong>
                <small>Curated notes and past papers</small>
              </div>
            </div>

            <div className="feature-card glass">
              <span className="material-symbols-outlined">group</span>
              <div>
                <strong>Tutor requests</strong>
                <small>Matched with verified peers</small>
              </div>
            </div>

            <div className="feature-card glass">
              <span className="material-symbols-outlined">chat_bubble</span>
              <div>
                <strong>Live chat</strong>
                <small>Real-time study sessions</small>
              </div>
            </div>

            <div className="feature-card glass">
              <span className="material-symbols-outlined">insights</span>
              <div>
                <strong>Quiz progress</strong>
                <small>Track mastery over time</small>
              </div>
            </div>
          </div>

          <div className="stats-row">
            <div className="stat-item">
              <strong>12k+</strong>
              <span>Active students</span>
            </div>
            <div className="stat-item">
              <strong>1,400</strong>
              <span>Peer tutors</span>
            </div>
            <div className="stat-item">
              <strong>98%</strong>
              <span>Session rating</span>
            </div>
          </div>
        </section>

        <section className="login-card glass-dark">
          <div className="card-header">
            <h2>Sign in</h2>
            <p>Use your university account to continue.</p>
          </div>

          <form className="login-form" onSubmit={handleSubmit}>
            <div className="field-group">
              <label htmlFor="email">Email Address</label>
              <div className="input-wrap">
                <span className="material-symbols-outlined">mail</span>
                <input
                  id="email"
                  name="email"
                  type="email"
                  value={form.email}
                  onChange={handleChange}
                  placeholder="name@university.edu"
                  required
                />
              </div>
            </div>

            <div className="field-group">
              <div className="label-row">
                <label htmlFor="password">Password</label>
                <a href="#">Forgot?</a>
              </div>
              <div className="input-wrap password-wrap">
                <span className="material-symbols-outlined">lock</span>
                <input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  value={form.password}
                  onChange={handleChange}
                  placeholder="••••••••"
                  required
                />
                <button
                  type="button"
                  className="ghost-icon"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  onClick={() => setShowPassword((prev) => !prev)}
                >
                  <span className="material-symbols-outlined">{showPassword ? 'visibility_off' : 'visibility'}</span>
                </button>
              </div>
            </div>

            <label className="check-row">
              <input type="checkbox" />
              <span>Keep me signed in</span>
            </label>

            {error && <div className="form-error">{error}</div>}

            <button type="submit" className="premium-btn" disabled={loading}>
              {loading ? 'Signing in...' : 'Continue'}
            </button>
          </form>

          <p className="switch-text">
            New here? <Link to="/register">Create an account</Link>
          </p>
        </section>
      </div>

      <footer className="login-footer">
        <span>© 2026 TeachQuest University Platform</span>
        <div className="footer-links">
          <a href="#">Privacy Policy</a>
          <a href="#">Terms of Service</a>
          <a href="#">Status</a>
        </div>
      </footer>
    </main>
  )
}
