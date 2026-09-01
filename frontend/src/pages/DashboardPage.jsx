import { useEffect, useState } from 'react'
import { Link, NavLink, Navigate, useNavigate } from 'react-router-dom'
import { fallbackJobs, getPostedJobs, getStoredUser, navItems, parseResponse } from '../utils/appData'

const normalizeStatus = (value) => String(value || '').trim().toLowerCase()

export default function DashboardPage() {
  const navigate = useNavigate()
  const [user, setUser] = useState(() => getStoredUser())
  const [jobs, setJobs] = useState(fallbackJobs)
  const [postedJobs, setPostedJobs] = useState(() => getPostedJobs())
  const [currentHires, setCurrentHires] = useState([])
  const [hiringProgress, setHiringProgress] = useState([])

  useEffect(() => {
    const syncDashboardData = () => {
      const currentUser = getStoredUser()
      if (!currentUser) {
        navigate('/')
        return
      }

      if (currentUser.userType === 'teacher') {
        navigate('/teacher-dashboard')
        return
      }

      const nextPostedJobs = getPostedJobs()
      const nextCurrentHires = nextPostedJobs.flatMap((job) => {
        const hiredApplicants = (job.applicants || []).filter((applicant) => normalizeStatus(applicant.status) === 'hired')
        return hiredApplicants.map((applicant) => ({
          id: `${job.id}-${applicant.email || applicant.id || applicant.name}`,
          name: applicant.name || 'Tutor',
          subject: job.subject || job.title || 'General tutoring',
          classTime: job.daysPerWeek || 'Flexible',
          status: 'Hired',
        }))
      })

      const nextHiringProgress = nextPostedJobs
        .filter((job) => Array.isArray(job.applicants) && job.applicants.length > 0)
        .map((job) => {
          const applicants = job.applicants || []
          const hired = applicants.filter((applicant) => normalizeStatus(applicant.status) === 'hired').length
          const pending = applicants.filter((applicant) => normalizeStatus(applicant.status) === 'pending' || normalizeStatus(applicant.status) === '').length
          const rejected = applicants.filter((applicant) => normalizeStatus(applicant.status) === 'rejected').length

          return {
            id: job.id,
            title: job.title || 'Tutor request',
            subject: job.subject || 'General tutoring',
            hired,
            pending,
            rejected,
          }
        })

      setUser(currentUser)
      setPostedJobs(nextPostedJobs)
      setCurrentHires(nextCurrentHires)
      setHiringProgress(nextHiringProgress)
    }

    syncDashboardData()

    fetch('/api/jobs/all')
      .then(async (response) => {
        if (!response.ok) return
        const data = await parseResponse(response)
        if (Array.isArray(data) && data.length > 0) {
          setJobs(data.map((job) => ({
            title: job.title || 'Tutor Opportunity',
            subject: job.subject || 'General',
            salary: job.salary || '$0/hr',
            address: job.address || 'Remote',
            days: job.days ? `${job.days} days/week` : 'Flexible',
          })))
        }
      })
      .catch(() => {
        setJobs(fallbackJobs)
      })
  }, [navigate])

  if (!user) {
    return <Navigate to="/" replace />
  }

  const displayName = user.username || user.email?.split('@')[0] || 'Alex'

  const quickActions = [
    { title: 'Hire Tutor', description: 'Find verified peer experts for 1-on-1 sessions.', icon: 'how_to_reg', to: '/jobs', accent: 'primary' },
    { title: 'My Posted Jobs', description: 'Review, edit, or remove your active requests.', icon: 'assignment', to: '/posted-jobs', accent: 'soft' },
    { title: 'Live chat', description: 'Resume your active study sessions.', icon: 'forum', to: '/chat', accent: 'soft' },
    { title: 'Quiz progress', description: 'Track mastery over time with community quizzes.', icon: 'lightbulb', to: '/quiz', accent: 'soft' },
    { title: 'Study Resources', description: 'Curated notes for deeper learning.', icon: 'library_books', to: '/resources', accent: 'soft' },
    { title: 'Upload Resources', description: 'Share your notes for others.', icon: 'upload_file', to: '/upload-resources', accent: 'outline' },
  ]

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
            <img alt="Student avatar" src="https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80" />
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

      <main className="dashboard-canvas">
        <section className="welcome-section">
          <h2>Welcome back, {displayName}.</h2>
          <p>Ready to master your next module? Connect with peers and elevate your learning.</p>
        </section>

        <section className="quick-actions-panel">
          <h3>Quick Actions</h3>
          <div className="quick-actions-grid">
            {quickActions.map((action) => (
              <Link key={action.title} to={action.to} className={`quick-action-card ${action.accent}`}>
                <div className="quick-action-icon">
                  <span className="material-symbols-outlined">{action.icon}</span>
                </div>
                <div className="quick-action-copy">
                  <h4>{action.title}</h4>
                  <p>{action.description}</p>
                </div>
              </Link>
            ))}
          </div>
        </section>

        <section className="dashboard-posted-jobs glass-panel">
          <div className="panel-header-row">
            <h3>Posted Jobs</h3>
            <button type="button" className="panel-link" onClick={() => navigate('/posted-jobs')}>View All</button>
          </div>

          <div className="dashboard-posted-jobs-list">
            {postedJobs.length === 0 ? (
              <div className="empty-mini-state">
                <p>No jobs posted yet.</p>
                <button type="button" className="primary-soft-button" onClick={() => navigate('/jobs')}>Post a job</button>
              </div>
            ) : (
              postedJobs.slice(0, 3).map((job) => (
                <div key={job.id} className="dashboard-posted-job-item">
                  <div>
                    <h4>{job.title}</h4>
                    <p>{job.subject}</p>
                  </div>
                  <div className="dashboard-posted-job-meta">
                    <span>{job.daysPerWeek} days</span>
                    <strong>{job.applicants?.length || 0} applicants</strong>
                  </div>
                </div>
              ))
            )}
          </div>
        </section>

        <div className="hire-grid">
          <section className="glass-panel hire-list-panel">
            <div className="panel-header-row">
              <h3>Current Hires</h3>
              <button type="button" className="panel-link">View All</button>
            </div>

            <div className="hire-list">
              {currentHires.length === 0 ? (
                <div className="empty-mini-state compact">
                  <p>No tutors hired yet.</p>
                  <button type="button" className="primary-soft-button" onClick={() => navigate('/jobs')}>Hire a tutor</button>
                </div>
              ) : (
                currentHires.map((hire) => (
                  <div key={hire.id} className="hire-item">
                    <div className="hire-profile">
                      <div className="hire-avatar">{hire.name.split(' ').map((part) => part[0]).slice(0, 2).join('')}</div>
                      <div className="hire-copy">
                        <h4>{hire.name}</h4>
                        <p>{hire.subject}</p>
                      </div>
                    </div>

                    <div className="hire-meta">
                      <div className="hire-meta-text">
                        <span>Work Pattern</span>
                        <strong>{hire.classTime}</strong>
                      </div>
                      <span className="status-pill due">HIRED</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </section>

          <section className="glass-panel hire-list-panel">
            <div className="panel-header-row">
              <h3>Hiring Progress</h3>
            </div>

            <div className="hire-list">
              {hiringProgress.length === 0 ? (
                <div className="empty-mini-state compact">
                  <p>No hiring activity yet.</p>
                  <button type="button" className="primary-soft-button" onClick={() => navigate('/jobs')}>Post a requirement</button>
                </div>
              ) : (
                hiringProgress.map((job) => (
                  <div key={job.id} className="hire-item previous-item">
                    <div className="hire-profile">
                      <div className="hire-avatar alternate">{job.title.split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase()}</div>
                      <div className="hire-copy">
                        <h4>{job.title}</h4>
                        <p>{job.hired} hired • {job.pending} pending • {job.rejected} rejected</p>
                      </div>
                    </div>

                    <div className="activity-badge">
                      {job.hired > 0 ? `${job.hired} hired` : `${job.pending} waiting`}
                    </div>
                  </div>
                ))
              )}
            </div>
          </section>
        </div>
      </main>
    </div>
  )
}
