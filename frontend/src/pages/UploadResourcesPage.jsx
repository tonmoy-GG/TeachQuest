import { useEffect, useState } from 'react'
import { NavLink, Navigate, useNavigate } from 'react-router-dom'
import { getStoredUser, navItems, STORAGE_KEY, trimesterOptions } from '../utils/appData'

export default function UploadResourcesPage() {
  const navigate = useNavigate()
  const [user, setUser] = useState(() => getStoredUser())
  const [isDragging, setIsDragging] = useState(false)
  const [selectedFile, setSelectedFile] = useState(null)

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

  const handleFileSelection = (event) => {
    const file = event.target.files?.[0]
    setSelectedFile(file || null)
  }

  const handleDrop = (event) => {
    event.preventDefault()
    setIsDragging(false)
    const file = event.dataTransfer.files?.[0]
    setSelectedFile(file || null)
  }

  return (
    <div className="student-dashboard-shell upload-resources-shell">
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

          <button type="button" className="logout-button" onClick={() => {
            localStorage.removeItem(STORAGE_KEY)
            navigate('/')
          }}>
            Logout
          </button>
        </div>
      </header>

      <main className="dashboard-canvas upload-page-canvas">
        <header className="resources-page-header upload-page-header">
          <div>
            <span className="mini-label">Student dashboard</span>
            <h2>Upload Resource</h2>
          </div>

          <button type="button" className="primary-soft-button" onClick={() => navigate('/resources')}>Back to Resources</button>
        </header>

        <section className="upload-form-card glass-panel">
          <div className="upload-form-stack">
            <div className="upload-field-group">
              <label htmlFor="department">Select Department</label>
              <select id="department" defaultValue="">
                <option value="" disabled>Select Department</option>
                <option value="cse">Computer Science</option>
                <option value="eee">Electrical Engineering</option>
                <option value="bba">Business Administration</option>
              </select>
            </div>

            <div className="upload-field-group">
              <label htmlFor="category">Select Category</label>
              <select id="category" defaultValue="">
                <option value="" disabled>Select Category</option>
                <option value="notes">Class Notes</option>
                <option value="recording">Class Recording</option>
                <option value="question">CT / MID / Final Questions</option>
              </select>
            </div>

            <div className="upload-field-group">
              <label htmlFor="trimester">Select Trimester/Semester</label>
              <select id="trimester" defaultValue="">
                <option value="" disabled>Select Trimester</option>
                {trimesterOptions.map((option) => (
                  <option key={option} value={option}>{option}</option>
                ))}
              </select>
            </div>

            <div className="upload-field-group">
              <label htmlFor="course">Course Code</label>
              <select id="course" defaultValue="">
                <option value="" disabled>Select Course Code</option>
                <option value="CSE3711">CSE3711</option>
                <option value="CSE3721">CSE3721</option>
                <option value="CSE3731">CSE3731</option>
              </select>
            </div>

            <div className="upload-field-group">
              <label htmlFor="description">Description</label>
              <textarea id="description" rows="5" placeholder="Add context about the resource, topic coverage, or exam preparation notes..." />
            </div>
          </div>

          <div className="upload-action-row">
            <label
              className={`upload-dropzone ${isDragging ? 'dragging' : ''}`}
              onDragOver={(event) => {
                event.preventDefault()
                setIsDragging(true)
              }}
              onDragLeave={(event) => {
                event.preventDefault()
                setIsDragging(false)
              }}
              onDrop={handleDrop}
            >
              <input type="file" hidden onChange={handleFileSelection} />
              <div className="upload-dropzone-content">
                <span className="material-symbols-outlined upload-icon">upload_file</span>
                <div>
                  <strong>{selectedFile ? selectedFile.name : 'Drag & drop your file here'}</strong>
                  <small>{selectedFile ? `${selectedFile.type || 'file'} • ${Math.max(1, Math.round(selectedFile.size / 1024))} KB` : 'or click to browse from your device'}</small>
                </div>
              </div>
            </label>

            <button type="button" className="secondary-action-button">External Link [Videos]</button>
          </div>

          <div className="upload-submit-row">
            <button type="button" className="primary-soft-button upload-submit-btn">Upload File</button>
          </div>
        </section>
      </main>
    </div>
  )
}
