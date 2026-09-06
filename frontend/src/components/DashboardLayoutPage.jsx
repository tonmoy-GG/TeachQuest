import { useEffect, useState } from 'react'
import { NavLink, Navigate, useNavigate } from 'react-router-dom'
import { getStoredUser, navItems } from '../utils/appData'

export default function DashboardLayoutPage({ title, buttonLabel, children }) {
  const navigate = useNavigate()
  const [user, setUser] = useState(() => getStoredUser())

  useEffect(() => {
    const currentUser = getStoredUser()
    if (!currentUser) {
      navigate('/')
      return
    }
    setUser(currentUser)
  }, [navigate])

  if (!user) {
    return <Navigate to="/" replace />
  }

  const displayName = user.username || user.email?.split('@')[0] || 'Alex'

  return (
    <div className="student-dashboard-shell">
      <header className="dashboard-topbar glass-panel">
        <div className="brand-block">
          <div className="brand-mark">TQ</div>
          <h1>TeachQuest</h1>
        </div>

        <nav className="main-nav" aria-label="Main navigation">
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="dashboard-search-box">
          <span className="material-symbols-outlined search-icon">search</span>
          <input type="text" placeholder="Search tutors, courses, or resources..." />
        </div>

        <div className="dashboard-top-actions">
          <button type="button" className="icon-button" aria-label="Notifications">
            <span className="material-symbols-outlined">notifications</span>
          </button>
          <button type="button" className="icon-button" aria-label="Settings">
            <span className="material-symbols-outlined">settings</span>
          </button>

          <div className="topbar-divider" />

          <button type="button" className="user-profile-button">
            <img
              alt="Student avatar"
              src="https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80"
            />
            <div className="user-meta">
              <span>{displayName}</span>
              <small>Computer Science</small>
            </div>
          </button>

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

      <main className="dashboard-canvas dashboard-subpage">
        <header className="page-header-card">
          <div>
            <span className="mini-label">Student dashboard</span>
            <h2>{title}</h2>
          </div>

          <button type="button" className="primary-soft-button">
            {buttonLabel}
          </button>
        </header>

        {children}
      </main>
    </div>
  )
}
