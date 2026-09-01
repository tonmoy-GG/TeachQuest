import { useEffect, useState } from 'react'
import { NavLink, Navigate, useNavigate } from 'react-router-dom'
import { getPostedJobs, getStoredUser, getUserScopedStorageKey, navItems, savePostedJobs, STORAGE_KEY, TEACHER_APPLICATIONS_KEY } from '../utils/appData'

export default function PostedJobsPage() {
  const navigate = useNavigate()
  const [user, setUser] = useState(() => getStoredUser())
  const [jobs, setJobs] = useState(() => getPostedJobs())
  const [expandedJobId, setExpandedJobId] = useState(null)

  useEffect(() => {
    const currentUser = getStoredUser()
    if (!currentUser) {
      navigate('/')
      return
    }

    if (currentUser.userType === 'teacher') {
      navigate('/teacher-dashboard')
      return
    }

    setUser(currentUser)
    setJobs(getPostedJobs())
  }, [navigate])

  if (!user) {
    return <Navigate to="/" replace />
  }

  const handleDelete = (jobId) => {
    const nextJobs = getPostedJobs().filter((job) => job.id !== jobId)
    localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
    savePostedJobs(nextJobs)
    setJobs(nextJobs)
  }

  const handleEdit = (jobId) => {
    navigate(`/jobs?edit=${jobId}`)
  }

  const handleHireApplicant = (jobId, applicantId, action = 'hire') => {
    const nextJobs = getPostedJobs().map((job) => {
      if (String(job.id) !== String(jobId)) return job

      const nextApplicants = (job.applicants || []).map((applicant) => {
        const matchKey = applicant.email || applicant.id || applicant.name
        if (String(matchKey) !== String(applicantId)) return applicant

        return {
          ...applicant,
          status: action === 'hire' ? 'Hired' : 'Rejected',
        }
      })

      return {
        ...job,
        applicants: nextApplicants,
      }
    })

    const teacherApplicationsKey = getUserScopedStorageKey(TEACHER_APPLICATIONS_KEY)
    const teacherApplications = JSON.parse(localStorage.getItem(teacherApplicationsKey) || '[]')
    const normalizedApplicantId = String(applicantId)

    const syncedTeacherApplications = teacherApplications.map((entry) => {
      if (String(entry.jobId) !== String(jobId)) return entry
      const isMatch = String(entry.email || entry.id || '').trim().toLowerCase() === normalizedApplicantId.trim().toLowerCase()
      if (!isMatch) return entry
      return { ...entry, status: action === 'hire' ? 'Hired' : 'Rejected' }
    })

    savePostedJobs(nextJobs)
    localStorage.setItem(teacherApplicationsKey, JSON.stringify(syncedTeacherApplications))
    setJobs(nextJobs)
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

        <div className="dashboard-top-actions">
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
            className="primary-soft-button logout-button-small"
            onClick={() => {
              localStorage.removeItem(STORAGE_KEY)
              navigate('/')
            }}
          >
            Logout
          </button>
        </div>
      </header>

      <main className="dashboard-canvas posted-jobs-page">
        <section className="posted-jobs-hero glass-panel">
          <div className="posted-job-hero-copy">
            <span className="mini-label">Talent Requests</span>
            <h2>Your Posted Job Opportunities</h2>
          </div>

          <button type="button" className="primary-soft-button" onClick={() => navigate('/jobs')}>
            + Post a New Job
          </button>
        </section>

        <section className="posted-job-list">
          {jobs.length === 0 ? (
            <div className="empty-job-state glass-panel">
              <h3>No jobs posted yet</h3>
              <p>Create your first job request and start matching with tutors.</p>
              <button type="button" className="primary-soft-button" onClick={() => navigate('/jobs')}>
                Post a job
              </button>
            </div>
          ) : (
            jobs.map((job) => (
              <article key={job.id} className="posted-job-card glass-panel">
                <div className="posted-job-header-row">
                  <h3>{job.title}</h3>
                  <span className="posted-job-status">Open</span>
                </div>

                <div className="posted-job-meta-grid">
                  <div className="meta-item">
                    <span>Subject</span>
                    <strong>{job.subject}</strong>
                  </div>
                  <div className="meta-item">
                    <span>Days per Week</span>
                    <strong>{job.daysPerWeek}</strong>
                  </div>
                  <div className="meta-item">
                    <span>Expected Salary</span>
                    <strong>{job.salary}</strong>
                  </div>
                  <div className="meta-item">
                    <span>Student Gender</span>
                    <strong>{job.studentGender}</strong>
                  </div>
                  <div className="meta-item">
                    <span>Tutor Gender</span>
                    <strong>{job.tutorGender}</strong>
                  </div>
                  <div className="meta-item">
                    <span>Location</span>
                    <strong>{job.location}</strong>
                  </div>
                </div>

                <div className="posted-job-description-wrap">
                  <span>Description</span>
                  <p>{job.description || 'No description provided yet.'}</p>
                </div>

                <div className="applicant-box">
                  <div className="applicant-heading-row">
                    <span>Applicants</span>
                    <strong>{job.applicants?.length || 0}</strong>
                  </div>

                  {expandedJobId === job.id ? (
                    job.applicants?.length ? (
                      <ul className="applicant-list">
                        {job.applicants.map((applicant) => (
                          <li key={`${job.id}-${applicant.email || applicant.id || applicant.name}`}>
                            <span className="applicant-pill">{applicant.name}</span>
                            <div className="applicant-detail-box">
                              <small><strong>Email:</strong> {applicant.email || 'Not provided'}</small>
                              <small><strong>University ID:</strong> {applicant.universityId || 'Not provided'}</small>
                              <small><strong>Phone:</strong> {applicant.contactNo || 'Not provided'}</small>
                              <small><strong>Address:</strong> {applicant.address || 'Not provided'}</small>
                              <small className={`applicant-status ${applicant.status === 'Hired' ? 'hired' : 'pending'}`}>
                                <strong>Status:</strong> {applicant.status || 'Pending'}
                              </small>
                            </div>
                            <div className="applicant-decision-row">
                              <button
                                type="button"
                                className={`applicant-approve-btn ${applicant.status === 'Hired' ? 'selected' : ''}`}
                                onClick={() => handleHireApplicant(job.id, applicant.email || applicant.id || applicant.name, 'hire')}
                              >
                                {applicant.status === 'Hired' ? 'Hired' : 'Hire'}
                              </button>
                              <button
                                type="button"
                                className={`applicant-reject-btn ${applicant.status === 'Rejected' ? 'selected' : ''}`}
                                onClick={() => handleHireApplicant(job.id, applicant.email || applicant.id || applicant.name, 'reject')}
                              >
                                {applicant.status === 'Rejected' ? 'Rejected' : 'Reject'}
                              </button>
                            </div>
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <p className="no-applicants">No applicants yet.</p>
                    )
                  ) : null}
                </div>

                <div className="posted-job-actions">
                  <button type="button" className="blue-action-btn" onClick={() => setExpandedJobId(expandedJobId === job.id ? null : job.id)}>
                    {expandedJobId === job.id ? 'Hide Applicants' : 'Applicants'}
                  </button>
                  <button type="button" className="yellow-action-btn" onClick={() => handleEdit(job.id)}>
                    Edit
                  </button>
                  <button type="button" className="red-action-btn" onClick={() => handleDelete(job.id)}>
                    Delete
                  </button>
                </div>
              </article>
            ))
          )}
        </section>
      </main>
    </div>
  )
}
