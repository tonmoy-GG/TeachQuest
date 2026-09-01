import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getRegisteredUsers, parseResponse, saveRegisteredUsers, stats, STORAGE_KEY } from '../utils/appData'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    username: '',
    universityId: '',
    contactNo: '',
    email: '',
    password: '',
    address: '',
    userType: 'student',
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setLoading(true)

    try {
      const existingUsers = getRegisteredUsers()
      const alreadyExists = existingUsers.some((user) => (user.email || '').trim().toLowerCase() === form.email.trim().toLowerCase())

      if (alreadyExists) {
        throw new Error('An account with this email already exists. Please sign in instead.')
      }

      const newUser = {
        username: form.username,
        universityId: form.universityId,
        contactNo: form.contactNo,
        email: form.email,
        password: form.password,
        address: form.address,
        userType: form.userType,
      }

      saveRegisteredUsers([...existingUsers, newUser])

      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: form.username,
          universityId: form.universityId,
          contactNo: form.contactNo,
          email: form.email,
          password: form.password,
          address: form.address,
          userType: form.userType,
        }),
      })

      const data = await parseResponse(response)

      if (!response.ok) {
        throw new Error(typeof data === 'string' ? data : data?.message || 'Registration failed')
      }

      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        username: form.username,
        email: form.email,
        userType: form.userType,
        universityId: form.universityId,
        contactNo: form.contactNo,
        address: form.address,
      }))

      if (form.userType === 'teacher') {
        navigate('/teacher-dashboard')
      } else {
        navigate('/dashboard')
      }
    } catch (err) {
      setError(err.message || 'Unable to create account.')
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

      <div className="login-main register-layout">
        <section className="hero-column">
          <div className="hero-copy">
            <span className="hero-label">Study together</span>
            <h1>
              Learn faster with
              <span> your community.</span>
            </h1>
            <p>
              Access peer support, job opportunities, and targeted resources built for modern student life.
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
            {stats.map((stat) => (
              <div key={stat.label} className="stat-item">
                <strong>{stat.value}</strong>
                <span>{stat.label}</span>
              </div>
            ))}
          </div>
        </section>

        <section className="login-card glass-dark register-card">
          <div className="card-header">
            <h2>Create account</h2>
            <p>Join the TeachQuest community.</p>
          </div>

          <form className="login-form" onSubmit={handleSubmit}>
            <div className="two-col">
              <div className="field-group">
                <label htmlFor="username">Username</label>
                <div className="input-wrap">
                  <span className="material-symbols-outlined">person</span>
                  <input id="username" name="username" type="text" value={form.username} onChange={handleChange} placeholder="John" required />
                </div>
              </div>

              <div className="field-group">
                <label htmlFor="universityId">University ID</label>
                <div className="input-wrap">
                  <span className="material-symbols-outlined">badge</span>
                  <input id="universityId" name="universityId" type="text" value={form.universityId} onChange={handleChange} placeholder="20240123" required />
                </div>
              </div>
            </div>

            <div className="field-group">
              <label htmlFor="contactNo">Contact Number</label>
              <div className="input-wrap">
                <span className="material-symbols-outlined">call</span>
                <input id="contactNo" name="contactNo" type="tel" value={form.contactNo} onChange={handleChange} placeholder="01XXXXXXXXX" required />
              </div>
            </div>

            <div className="field-group">
              <label htmlFor="reg-email">University Email</label>
              <div className="input-wrap">
                <span className="material-symbols-outlined">mail</span>
                <input id="reg-email" name="email" type="email" value={form.email} onChange={handleChange} placeholder="name@university.edu" required />
              </div>
            </div>

            <div className="field-group">
              <label htmlFor="address">Address</label>
              <div className="input-wrap">
                <span className="material-symbols-outlined">location_on</span>
                <input id="address" name="address" type="text" value={form.address} onChange={handleChange} placeholder="Your address" />
              </div>
            </div>

            <div className="field-group">
              <label htmlFor="reg-password">Password</label>
              <div className="input-wrap">
                <span className="material-symbols-outlined">lock</span>
                <input id="reg-password" name="password" type="password" value={form.password} onChange={handleChange} placeholder="Create a strong password" required />
              </div>
            </div>

            <div className="field-group">
              <label htmlFor="userType">Role</label>
              <div className="input-wrap select-wrap">
                <span className="material-symbols-outlined">school</span>
                <select id="userType" name="userType" value={form.userType} onChange={handleChange}>
                  <option value="student">Student</option>
                  <option value="teacher">Tutor</option>
                  <option value="jobposter">Job Poster</option>
                </select>
              </div>
            </div>

            {error && <div className="form-error">{error}</div>}

            <button type="submit" className="premium-btn" disabled={loading}>
              {loading ? 'Creating account...' : 'Continue'}
            </button>
          </form>

          <div className="divider-row"><span>OR</span></div>

          <button type="button" className="sso-btn">
            <span className="material-symbols-outlined">school</span>
            Continue with University SSO
          </button>

          <p className="switch-text">
            Already have an account? <Link to="/">Sign in</Link>
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
