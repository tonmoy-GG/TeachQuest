import { useEffect, useState } from 'react'
import { NavLink, Navigate, useNavigate } from 'react-router-dom'
import { getStoredUser, STORAGE_KEY, trimesterOptions } from '../utils/appData'

const teacherNavItems = [
  { label: 'Dashboard', to: '/teacher-dashboard' },
  { label: 'Job Board', to: '/teacher-job-board' },
  { label: 'My Applications', to: '/teacher-applications' },
  { label: 'Study Resources', to: '/teacher-resources' },
  { label: 'Community Q&A', to: '/questions' },
  { label: 'Upload Resources', to: '/teacher-upload-resources' },
  { label: 'Chat', to: '/teacher-chat' },
  { label: 'Question Bank', to: '/quiz' },
]

export default function TeacherUploadResourcesPage() {
  const navigate = useNavigate()
  const [user] = useState(() => getStoredUser())
  const [isDragging, setIsDragging] = useState(false)
  const [selectedFile, setSelectedFile] = useState(null)
  const [formData, setFormData] = useState({
    department: '',
    category: '',
    trimester: '',
    course: '',
    description: '',
  })

  useEffect(() => {
    const currentUser = getStoredUser()
    if (!currentUser) {
      navigate('/')
      return
    }

    if (currentUser.userType && currentUser.userType !== 'teacher') {
      navigate('/upload-resources')
      return
    }

  }, [navigate])

  if (!user) {
    return <Navigate to="/" replace />
  }

  const displayName = user.username || user.email?.split('@')[0] || 'Teacher'

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

  const handleInputChange = (event) => {
    const { id, value } = event.target
    setFormData((prev) => ({ ...prev, [id]: value }))
  }

  const handleUpload = () => {
    if (!formData.department || !formData.category || !formData.trimester || !formData.course || !selectedFile) {
      alert('Please complete all fields and choose a file before uploading.')
      return
    }

    const payload = new FormData()
    payload.append('department', formData.department === 'cse' ? 'Computer Science' : formData.department === 'eee' ? 'Electrical Engineering' : 'Business Administration')
    payload.append('category', formData.category)
    payload.append('semester', formData.trimester)
    payload.append('courseCode', formData.course)
    payload.append('description', formData.description || 'Uploaded resource')
    payload.append('uploaderId', String(user.id))
    payload.append('file', selectedFile)

    fetch('/api/resources/upload', { method: 'POST', body: payload })
      .then(async (response) => {
        if (!response.ok) throw new Error(await response.text() || 'Upload failed.')
        navigate('/teacher-resources')
      })
      .catch((error) => alert(error.message))
  }

  return (
    <div className="student-dashboard-shell upload-resources-shell">
      <header className="dashboard-topbar glass-panel">
        <div className="brand-block">
          <div className="brand-mark">TQ</div>
          <h1>TeachQuest</h1>
        </div>

        <nav className="main-nav" aria-label="Teacher navigation">
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
              localStorage.removeItem(STORAGE_KEY)
              navigate('/')
            }}
          >
            Logout
          </button>
        </div>
      </header>

      <main className="dashboard-canvas upload-page-canvas">
        <header className="resources-page-header upload-page-header">
          <div>
            <span className="mini-label">Teacher dashboard</span>
            <h2>Upload Resource</h2>
          </div>

          <button type="button" className="primary-soft-button" onClick={() => navigate('/teacher-resources')}>Back to Resources</button>
        </header>

        <section className="upload-form-card glass-panel">
          <div className="upload-form-stack">
            <div className="upload-field-group">
              <label htmlFor="department">Select Department</label>
              <select id="department" value={formData.department} onChange={handleInputChange}>
                <option value="" disabled>Select Department</option>
                <option value="cse">Computer Science</option>
                <option value="eee">Electrical Engineering</option>
                <option value="bba">Business Administration</option>
              </select>
            </div>

            <div className="upload-field-group">
              <label htmlFor="category">Select Category</label>
              <select id="category" value={formData.category} onChange={handleInputChange}>
                <option value="" disabled>Select Category</option>
                <option value="notes">Class Notes</option>
                <option value="recording">Class Recording</option>
                <option value="question">CT / MID / Final Questions</option>
              </select>
            </div>

            <div className="upload-field-group">
              <label htmlFor="trimester">Select Trimester/Semester</label>
              <select id="trimester" value={formData.trimester} onChange={handleInputChange}>
                <option value="" disabled>Select Trimester</option>
                {trimesterOptions.map((option) => (
                  <option key={option} value={option}>{option}</option>
                ))}
              </select>
            </div>

            <div className="upload-field-group">
              <label htmlFor="course">Course Code</label>
              <select id="course" value={formData.course} onChange={handleInputChange}>
                <option value="" disabled>Select Course Code</option>
                <option value="CSE3711">CSE3711</option>
                <option value="CSE3721">CSE3721</option>
                <option value="CSE3731">CSE3731</option>
              </select>
            </div>

            <div className="upload-field-group">
              <label htmlFor="description">Description</label>
              <textarea id="description" rows="5" value={formData.description} onChange={handleInputChange} placeholder="Add context about the resource, topic coverage, or exam preparation notes..." />
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
            <button type="button" className="primary-soft-button upload-submit-btn" onClick={handleUpload}>Upload File</button>
          </div>
        </section>
      </main>
    </div>
  )
}
