import { useEffect, useState } from 'react'
import { Link, NavLink, Navigate, useNavigate } from 'react-router-dom'
import { getAllPostedJobs, getStoredUser, TEACHER_APPLICATIONS_KEY, getUserScopedStorageKey } from '../utils/appData'

const teacherNavItems = [
  { label: 'Dashboard', to: '/teacher-dashboard' },
  { label: 'Job Board', to: '/teacher-job-board' },
  { label: 'My Applications', to: '/teacher-applications' },
  { label: 'Study Resources', to: '/teacher-resources' },
  { label: 'Upload Resources', to: '/teacher-upload-resources' },
  { label: 'Chat', to: '/teacher-chat' },
  { label: 'Question Bank', to: '/quiz' },
]

export default function TeacherDashboardPage() {
  const navigate = useNavigate()
  const [user, setUser] = useState(() => getStoredUser())
  const [postedJobs, setPostedJobs] = useState(() => getAllPostedJobs())
  const [appliedJobs, setAppliedJobs] = useState(() => {
    try {
      const scopedKey = getUserScopedStorageKey(TEACHER_APPLICATIONS_KEY)
      const raw = localStorage.getItem(scopedKey)
      return raw ? JSON.parse(raw) : []
    } catch {
      return []
    }
  })

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
    setPostedJobs(getAllPostedJobs())
  }, [navigate])

  if (!user) {
    return <Navigate to="/" replace />
  }

  const displayName = user.username || user.email?.split('@')[0] || 'Teacher'

  const quickActions = [
    { title: 'Job Board', description: 'Browse all student tutoring requests and apply to the best fit.', icon: 'travel_explore', to: '/teacher-job-board', accent: 'primary' },
    { title: 'My Applications', description: 'Track your submissions and see the latest hiring decisions.', icon: 'assignment_turned_in', to: '/teacher-applications', accent: 'soft' },
    { title: 'Study Resources', description: 'Access curated notes, course files, and learning material.', icon: 'library_books', to: '/teacher-resources', accent: 'soft' },
    { title: 'Upload Resources', description: 'Share study material with students and peers.', icon: 'upload_file', to: '/teacher-upload-resources', accent: 'outline' },
    { title: 'Live Chat', description: 'Continue conversations with students and coordinators.', icon: 'forum', to: '/teacher-chat', accent: 'soft' },
    { title: 'Question Bank', description: 'Practice and review course questions for better support.', icon: 'quiz', to: '/quiz', accent: 'soft' },
  ]

  const currentTeacherEmail = (user?.email || '').trim().toLowerCase()

  const acceptedJobIds = new Set(
    postedJobs.flatMap((job) => {
      const applicants = Array.isArray(job.applicants) ? job.applicants : []
      return applicants
        .filter((applicant) => {
          const applicantEmail = String(applicant.email || applicant.id || '').trim().toLowerCase()
          const status = String(applicant.status || '').trim().toLowerCase()
          return applicantEmail === currentTeacherEmail && (status === 'hired' || status === 'accepted')
        })
        .map(() => String(job.id))
    })
  )

  const savedAcceptedEntries = Array.isArray(JSON.parse(localStorage.getItem(getUserScopedStorageKey(TEACHER_APPLICATIONS_KEY)) || '[]'))
    ? JSON.parse(localStorage.getItem(getUserScopedStorageKey(TEACHER_APPLICATIONS_KEY)) || '[]')
    : []

  savedAcceptedEntries.forEach((entry) => {
    const matchesTeacher = String(entry.email || entry.id || '').trim().toLowerCase() === currentTeacherEmail
    const status = String(entry.status || '').trim().toLowerCase()
    if (matchesTeacher && (status === 'hired' || status === 'accepted')) {
      acceptedJobIds.add(String(entry.jobId || entry.id || ''))
    }
  })

  const activeTuitions = postedJobs
    .filter((job) => acceptedJobIds.has(String(job.id)))
    .slice(0, 3)
    .map((job) => ({
      id: job.id,
      student: job.title || 'Student Request',
      subjects: job.subject || 'General',
      salary: job.salary || 'Negotiable',
      status: 'Accepted',
    }))

  const recentJobs = postedJobs.map((job) => ({
    id: job.id,
    subject: job.subject || 'General',
    location: job.location || 'Online',
    salary: job.salary || 'Negotiable',
    applied: appliedJobs.includes(job.id) || appliedJobs.includes(String(job.id)),
  }))

  const handleApply = (jobId) => {
    const next = appliedJobs.includes(jobId) ? appliedJobs : [...appliedJobs, jobId]
    setAppliedJobs(next)
    localStorage.setItem(getUserScopedStorageKey(TEACHER_APPLICATIONS_KEY), JSON.stringify(next))
  }

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
        <section className="welcome-section teacher-welcome-section">
          <h2>Welcome back, {displayName}.</h2>
          <p>Review student tutoring requests, apply to new opportunities, and keep track of your active matches.</p>
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

        <section className="teacher-section-block">
          <h2 className="teacher-main-title">
            <span className="title-accent" />
            Active Tuitions
          </h2>

          <div className="teacher-table-card glass-panel">
            <div className="teacher-table-head">
              <span>Student Name</span>
              <span>Subjects</span>
              <span>Salary</span>
              <span>Status</span>
              <span>Action</span>
            </div>

            {activeTuitions.length > 0 && activeTuitions.map((item) => (
              <div key={item.id || `${item.student}-${item.subjects}`} className="teacher-table-row">
                <span>{item.student}</span>
                <span>{item.subjects}</span>
                <span>{item.salary}</span>
                <span>{item.status}</span>
                <span>
                  <button
                    type="button"
                    className="teacher-row-action accepted"
                    disabled
                  >
                    Accepted
                  </button>
                </span>
              </div>
            ))}
          </div>
        </section>

        <section className="teacher-section-block teacher-recent-block">
          <div className="teacher-section-header">
            <h2 className="teacher-main-title compact">
              <span className="title-accent" />
              Recent Job Openings
            </h2>
            <button type="button" className="teacher-view-link">View All →</button>
          </div>

          <div className="teacher-table-card glass-panel">
            <div className="teacher-table-head">
              <span>Subject</span>
              <span>Location</span>
              <span>Salary</span>
              <span>Action</span>
            </div>

            {recentJobs.length === 0 ? (
              <div className="teacher-empty-row">No recent student job openings available.</div>
            ) : (
              recentJobs.map((job) => (
                <div key={job.id || `${job.subject}-${job.location}-${job.salary}`} className="teacher-table-row">
                  <span>{job.subject}</span>
                  <span>{job.location}</span>
                  <span>{job.salary}</span>
                  <span>
                    <button
                      type="button"
                      className={`teacher-row-action ${job.applied ? 'applied' : ''}`}
                      onClick={() => job.id && handleApply(job.id)}
                    >
                      {job.applied ? 'Applied' : 'Apply'}
                    </button>
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
