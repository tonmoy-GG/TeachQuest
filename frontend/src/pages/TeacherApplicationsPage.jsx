import { useEffect, useState } from 'react'
import { NavLink, Navigate, useNavigate } from 'react-router-dom'
import { getAllPostedJobs, getStoredUser, getUserScopedStorageKey, TEACHER_APPLICATIONS_KEY } from '../utils/appData'

const getTeacherApplications = (currentUser) => {
  if (!currentUser?.email) return []

  const currentEmail = String(currentUser.email).trim().toLowerCase()

  try {
    const scopedKey = getUserScopedStorageKey(TEACHER_APPLICATIONS_KEY)
    const raw = localStorage.getItem(scopedKey)
    const fromKey = raw ? JSON.parse(raw) : []
    const byKey = Array.isArray(fromKey)
      ? fromKey.filter((app) => String(app.email || '').trim().toLowerCase() === currentEmail)
      : []

    const fromJobs = getAllPostedJobs()
      .flatMap((job) => (Array.isArray(job.applicants) ? job.applicants.map((applicant) => ({
        id: String(applicant.email || applicant.id || applicant.name || ''),
        jobId: String(job.id),
        title: job.title || 'Tutor Request',
        subject: job.subject || 'General',
        salary: job.salary || 'Negotiable',
        email: applicant.email || '',
        status: applicant.status || 'Pending',
      })) : []))
      .filter((app) => String(app.email || '').trim().toLowerCase() === currentEmail)

    if (byKey.length > 0) {
      return byKey.map((entry) => ({
        ...entry,
        status: fromJobs.find((jobApp) => String(jobApp.jobId) === String(entry.jobId))?.status || entry.status || 'Pending',
      }))
    }

    return fromJobs
  } catch {
    return []
  }
}

export default function TeacherApplicationsPage() {
  const navigate = useNavigate()
  const [user, setUser] = useState(() => getStoredUser())
  const [applications, setApplications] = useState(() => getTeacherApplications(getStoredUser()))

  useEffect(() => {
    const currentUser = getStoredUser()
    if (!currentUser) {
      navigate('/')
      return
    }

    if (currentUser.userType && currentUser.userType !== 'teacher') {
      navigate('/dashboard')
      return
    }

    setUser(currentUser)
    setApplications(getTeacherApplications(currentUser))
  }, [navigate])

  if (!user) {
    return <Navigate to="/" replace />
  }

  const displayName = user.username || user.email?.split('@')[0] || 'Teacher'

  const teacherNavItems = [
    { label: 'Dashboard', to: '/teacher-dashboard' },
    { label: 'Job Board', to: '/teacher-job-board' },
    { label: 'My Applications', to: '/teacher-applications' },
    { label: 'Study Resources', to: '/teacher-resources' },
    { label: 'Upload Resources', to: '/teacher-upload-resources' },
    { label: 'Chat', to: '/teacher-chat' },
    { label: 'Question Bank', to: '/quiz' },
  ]

  return (
    <div className="student-dashboard-shell teacher-dashboard-shell">
      <header className="dashboard-topbar glass-panel">
        <div className="brand-block">
          <div className="brand-mark">TQ</div>
          <h1>TeachQuest</h1>
        </div>

        <nav className="main-nav" aria-label="Main navigation">
          {teacherNavItems.map((item) => (
            <NavLink key={item.to} to={item.to} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="dashboard-top-actions">
          <button type="button" className="user-profile-button">
            <img alt="Teacher avatar" src="https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80" />
            <div className="user-meta">
              <span>{displayName}</span>
              <small>Teacher</small>
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

      <main className="dashboard-canvas teacher-dashboard-canvas">
        <section className="teacher-section-block">
          <h2 className="teacher-main-title">
            <span className="title-accent" />
            My Applications
          </h2>

          <div className="teacher-table-card glass-panel">
            <div className="teacher-table-head">
              <span>Job Title</span>
              <span>Subject</span>
              <span>Salary</span>
              <span>Status</span>
            </div>

            {applications.length === 0 ? (
              <div className="teacher-empty-row">You have not applied to any jobs yet.</div>
            ) : (
              applications.map((application) => (
                <div key={application.id || application.jobId} className="teacher-table-row">
                  <span>{application.title || 'Tutor Request'}</span>
                  <span>{application.subject || 'General'}</span>
                  <span>{application.salary || 'Negotiable'}</span>
                  <span>
                    <span className={`applicant-status ${String(application.status || 'Pending').toLowerCase() === 'hired' ? 'hired' : 'pending'}`}>
                      {application.status || 'Pending'}
                    </span>
                  </span>
                </div>
              ))
            )}
          </div>
        </section>
      </main>
    </div>
  )
}
