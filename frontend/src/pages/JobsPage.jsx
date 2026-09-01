import { useEffect, useState } from 'react'
import { NavLink, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { getPostedJobs, getStoredUser, navItems, savePostedJobs } from '../utils/appData'

const initialJobForm = {
  title: '',
  subject: '',
  daysPerWeek: '',
  salary: '',
  studentGender: 'male',
  tutorGender: 'any',
  location: '',
  description: '',
}

export default function JobsPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [user, setUser] = useState(() => getStoredUser())
  const [jobForm, setJobForm] = useState(initialJobForm)
  const [editingId, setEditingId] = useState(null)

  useEffect(() => {
    const currentUser = getStoredUser()
    if (!currentUser) {
      navigate('/')
      return
    }

    setUser(currentUser)

    const editId = searchParams.get('edit')
    if (!editId) {
      setJobForm(initialJobForm)
      setEditingId(null)
      return
    }

    const targetJob = getPostedJobs().find((job) => String(job.id) === String(editId))
    if (targetJob) {
      setJobForm({
        title: targetJob.title || '',
        subject: targetJob.subject || '',
        daysPerWeek: targetJob.daysPerWeek || '',
        salary: targetJob.salary || '',
        studentGender: targetJob.studentGender || 'male',
        tutorGender: targetJob.tutorGender || 'any',
        location: targetJob.location || '',
        description: targetJob.description || '',
      })
      setEditingId(targetJob.id)
    }
  }, [navigate, searchParams])

  if (!user) {
    return <Navigate to="/" replace />
  }

  const displayName = user.username || user.email?.split('@')[0] || 'Alex'

  const handleChange = (event) => {
    const { name, value } = event.target
    setJobForm((prev) => ({ ...prev, [name]: value }))
  }

  const handleSubmit = (event) => {
    event.preventDefault()

    const cleanJob = {
      ...jobForm,
      title: jobForm.title.trim(),
      subject: jobForm.subject.trim() || 'General',
      daysPerWeek: jobForm.daysPerWeek.trim() || 'Flexible',
      salary: jobForm.salary.trim() || 'Negotiable',
      location: jobForm.location.trim() || 'Online',
      description: jobForm.description.trim() || 'No description provided.',
      studentGender: jobForm.studentGender || 'male',
      tutorGender: jobForm.tutorGender || 'any',
    }

    const storedJobs = getPostedJobs()
    const nextJobs = editingId
      ? storedJobs.map((job) => (String(job.id) === String(editingId) ? { ...job, ...cleanJob, id: editingId } : job))
      : [{ id: Date.now().toString(), ...cleanJob, applicants: [] }, ...storedJobs]

    savePostedJobs(nextJobs)
    navigate('/posted-jobs')
  }

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

      <main className="dashboard-canvas job-post-main">
        <div className="job-post-card glass-panel">
          <div className="job-post-header">
            <div className="job-post-icon">
              <span className="material-symbols-outlined">post_add</span>
            </div>
            <div>
              <h1>{editingId ? 'Edit Job' : 'Post a Job'}</h1>
              <p>Create a request to find the perfect peer tutor.</p>
            </div>
          </div>

          <form className="job-form" onSubmit={handleSubmit}>
            <div className="job-form-section">
              <h3>Job Details</h3>
              <div className="job-form-grid">
                <div className="job-form-group">
                  <label htmlFor="job-title">Job Title</label>
                  <input id="job-title" className="job-input" name="title" type="text" value={jobForm.title} onChange={handleChange} placeholder="e.g. Need help with Calculus II" required />
                </div>

                <div className="job-form-group">
                  <label htmlFor="subject">Subject</label>
                  <select id="subject" className="job-input job-select" name="subject" value={jobForm.subject} onChange={handleChange} required>
                    <option value="" disabled>Select a subject</option>
                    <option value="Mathematics">Mathematics</option>
                    <option value="Physics">Physics</option>
                    <option value="Computer Science">Computer Science</option>
                    <option value="Languages">Languages</option>
                    <option value="Statistics">Statistics</option>
                  </select>
                </div>

                <div className="job-form-group full-span">
                  <label htmlFor="days-per-week">Days per Week</label>
                  <input id="days-per-week" className="job-input" name="daysPerWeek" type="text" value={jobForm.daysPerWeek} onChange={handleChange} placeholder="e.g. 3" required />
                </div>
              </div>
            </div>

            <div className="job-form-section">
              <h3>Description &amp; Location</h3>
              <div className="job-form-stack">
                <div className="job-form-group">
                  <label htmlFor="requirements">Requirements &amp; Description</label>
                  <textarea id="requirements" className="job-input job-textarea" name="description" rows="4" value={jobForm.description} onChange={handleChange} placeholder="Describe what you need help with, preferred learning style, etc." required />
                </div>

                <div className="job-form-group">
                  <label htmlFor="location">Address / Location</label>
                  <textarea id="location" className="job-input job-textarea job-textarea-small" name="location" rows="2" value={jobForm.location} onChange={handleChange} placeholder="Library room, cafe, or 'Online'" required />
                </div>
              </div>
            </div>

            <div className="job-form-section">
              <h3>Preferences</h3>
              <div className="job-form-stack">
                <div className="job-form-group">
                  <label htmlFor="salary">Expected Salary / Rate ($/hr)</label>
                  <input id="salary" className="job-input" name="salary" type="text" value={jobForm.salary} onChange={handleChange} placeholder="e.g. $15/hr or Negotiable" required />
                </div>

                <div className="job-form-two-col">
                  <div className="job-form-group">
                    <label>Student Gender</label>
                    <div className="radio-row">
                      <label className="radio-option"><input type="radio" name="studentGender" value="male" checked={jobForm.studentGender === 'male'} onChange={handleChange} /> <span>Male</span></label>
                      <label className="radio-option"><input type="radio" name="studentGender" value="female" checked={jobForm.studentGender === 'female'} onChange={handleChange} /> <span>Female</span></label>
                    </div>
                  </div>

                  <div className="job-form-group">
                    <label>Tutor Gender Preference</label>
                    <div className="radio-row radio-row-wrap">
                      <label className="radio-option"><input type="radio" name="tutorGender" value="male" checked={jobForm.tutorGender === 'male'} onChange={handleChange} /> <span>Male</span></label>
                      <label className="radio-option"><input type="radio" name="tutorGender" value="female" checked={jobForm.tutorGender === 'female'} onChange={handleChange} /> <span>Female</span></label>
                      <label className="radio-option"><input type="radio" name="tutorGender" value="any" checked={jobForm.tutorGender === 'any'} onChange={handleChange} /> <span>Doesn't Matter</span></label>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div className="job-submit-row">
              <button type="submit" className="primary-soft-button job-submit-btn">
                <span>{editingId ? 'Update Job' : 'Post Job'}</span>
                <span className="material-symbols-outlined">send</span>
              </button>
            </div>
          </form>
        </div>
      </main>
    </div>
  )
}
