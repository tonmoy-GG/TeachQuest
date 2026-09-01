import { useEffect, useState } from 'react'
import { NavLink, Navigate, useNavigate } from 'react-router-dom'
import { findPostedJobStorageKey, getAllPostedJobs, getRegisteredUsers, getStoredUser, savePostedJobs, getUserScopedStorageKey, TEACHER_APPLICATIONS_KEY } from '../utils/appData'

const teacherNavItems = [
  { label: 'Dashboard', to: '/teacher-dashboard' },
  { label: 'Job Board', to: '/teacher-job-board' },
  { label: 'My Applications', to: '/teacher-applications' },
  { label: 'Study Resources', to: '/teacher-resources' },
  { label: 'Upload Resources', to: '/teacher-upload-resources' },
  { label: 'Chat', to: '/teacher-chat' },
  { label: 'Question Bank', to: '/quiz' },
]

export default function TeacherJobBoardPage() {
  const navigate = useNavigate()
  const [user, setUser] = useState(() => getStoredUser())
  const [postedJobs, setPostedJobs] = useState(() => getAllPostedJobs())
  const [appliedJobs, setAppliedJobs] = useState(() => {
    try {
      const scopedKey = getUserScopedStorageKey(TEACHER_APPLICATIONS_KEY)
      const raw = localStorage.getItem(scopedKey)
      const saved = raw ? JSON.parse(raw) : []
      if (!Array.isArray(saved)) return []

      return saved.map((entry) => String(entry.jobId || entry.id || '')).filter(Boolean)
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

  const handleApply = (jobId) => {
    const currentUser = getStoredUser()
    if (!currentUser) return

    const registeredProfile = getRegisteredUsers().find((candidate) =>
      (candidate.email || '').trim().toLowerCase() === (currentUser.email || '').trim().toLowerCase()
    ) || currentUser

    const applicant = {
      id: currentUser.email || `teacher-${jobId}`,
      name: currentUser.username || 'Teacher',
      email: currentUser.email || '',
      universityId: registeredProfile.universityId || 'N/A',
      contactNo: registeredProfile.contactNo || 'N/A',
      address: registeredProfile.address || 'N/A',
      userType: 'teacher',
      status: 'Pending',
    }

    const nextApplied = appliedJobs.includes(String(jobId)) ? appliedJobs : [...appliedJobs, String(jobId)]
    const ownedJobKey = findPostedJobStorageKey(jobId)
    const ownerJobs = JSON.parse(localStorage.getItem(ownedJobKey) || '[]')
    const ownerJobsList = Array.isArray(ownerJobs) ? ownerJobs : []

    const nextOwnerJobs = ownerJobsList.map((job) => {
      if (String(job.id) !== String(jobId)) return job

      const existingApplicants = Array.isArray(job.applicants) ? job.applicants : []
      const alreadyAppliedToJob = existingApplicants.some((person) =>
        (person.email || person.id || '').toString().trim().toLowerCase() === (currentUser.email || '').toString().trim().toLowerCase()
      )

      return {
        ...job,
        applicants: alreadyAppliedToJob ? existingApplicants : [...existingApplicants, applicant],
      }
    })

    const scopedTeacherApplicationsKey = getUserScopedStorageKey(TEACHER_APPLICATIONS_KEY)
    const teacherApplications = JSON.parse(localStorage.getItem(scopedTeacherApplicationsKey) || '[]')
    const targetJob = ownerJobsList.find((job) => String(job.id) === String(jobId)) || null
    const applicationRecord = {
      id: currentUser.email || `teacher-${jobId}`,
      jobId: String(jobId),
      title: targetJob?.title || 'Tutor Request',
      subject: targetJob?.subject || 'General',
      salary: targetJob?.salary || 'Negotiable',
      email: currentUser.email || '',
      status: 'Pending',
    }

    const nextTeacherApplications = teacherApplications.some((entry) => String(entry.jobId) === String(jobId) && String(entry.email || entry.id || '').trim().toLowerCase() === String(currentUser.email || '').trim().toLowerCase())
      ? teacherApplications.map((entry) => String(entry.jobId) === String(jobId) && String(entry.email || entry.id || '').trim().toLowerCase() === String(currentUser.email || '').trim().toLowerCase() ? { ...entry, ...applicationRecord } : entry)
      : [...teacherApplications, applicationRecord]

    localStorage.setItem(ownedJobKey, JSON.stringify(nextOwnerJobs))
    setAppliedJobs(nextApplied)
    localStorage.setItem(scopedTeacherApplicationsKey, JSON.stringify(nextTeacherApplications))
    setPostedJobs(getAllPostedJobs())
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
        <section className="teacher-section-block">
          <h2 className="teacher-main-title">
            <span className="title-accent" />
            Job Board
          </h2>

          <div className="teacher-table-card glass-panel">
            <div className="teacher-table-head">
              <span>Title</span>
              <span>Subject</span>
              <span>Location</span>
              <span>Salary</span>
              <span>Action</span>
            </div>

            {postedJobs.length === 0 ? (
              <div className="teacher-empty-row">No student job posts are available right now.</div>
            ) : (
              postedJobs.map((job) => (
                <div key={job.id || `${job.title}-${job.subject}`} className="teacher-table-row">
                  <span>{job.title || 'Student Request'}</span>
                  <span>{job.subject || 'General'}</span>
                  <span>{job.location || 'Online'}</span>
                  <span>{job.salary || 'Negotiable'}</span>
                  <span>
                    <button
                      type="button"
                      className={`teacher-row-action ${appliedJobs.includes(job.id) ? 'applied' : ''}`}
                      onClick={() => job.id && handleApply(job.id)}
                    >
                      {appliedJobs.includes(job.id) ? 'Applied' : 'Apply'}
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
