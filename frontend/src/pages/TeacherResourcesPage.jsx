import { useEffect, useState } from 'react'
import { NavLink, Navigate, useNavigate } from 'react-router-dom'
import ResourceTypeIcon from '../components/ResourceTypeIcon'
import { courseCards, getStoredUser, resourceCategories, trimesterOptions } from '../utils/appData'

const teacherNavItems = [
  { label: 'Dashboard', to: '/teacher-dashboard' },
  { label: 'Job Board', to: '/teacher-job-board' },
  { label: 'My Applications', to: '/teacher-applications' },
  { label: 'Study Resources', to: '/teacher-resources' },
  { label: 'Upload Resources', to: '/teacher-upload-resources' },
  { label: 'Chat', to: '/teacher-chat' },
  { label: 'Question Bank', to: '/quiz' },
]

export default function TeacherResourcesPage() {
  const navigate = useNavigate()
  const [user, setUser] = useState(() => getStoredUser())

  useEffect(() => {
    const currentUser = getStoredUser()
    if (!currentUser) {
      navigate('/')
      return
    }

    if (currentUser.userType && currentUser.userType !== 'teacher') {
      navigate('/resources')
      return
    }

    setUser(currentUser)
  }, [navigate])

  if (!user) {
    return <Navigate to="/" replace />
  }

  const displayName = user.username || user.email?.split('@')[0] || 'Teacher'

  return (
    <div className="student-dashboard-shell">
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
              localStorage.removeItem('teachquest_user')
              navigate('/')
            }}
          >
            Logout
          </button>
        </div>
      </header>

      <main className="dashboard-canvas resources-page-canvas">
        <header className="resources-page-header">
          <div>
            <span className="mini-label">Teacher dashboard</span>
            <h2>Study Resources</h2>
          </div>

          <div className="resources-header-tools">
            <div className="resources-filter-bar">
              <select aria-label="Department filter" defaultValue="all-depts">
                <option value="all-depts">All Depts</option>
                <option value="cse">Computer Science</option>
                <option value="bba">BBA</option>
              </select>
              <select aria-label="Trimester filter" defaultValue="all-trims">
                <option value="all-trims">All Trim</option>
                {trimesterOptions.map((option) => (
                  <option key={option} value={option}>{option}</option>
                ))}
              </select>
              <button type="button" className="filter-button">Filter</button>
            </div>
            <button type="button" className="primary-soft-button" onClick={() => navigate('/teacher-upload-resources')}>Upload file</button>
          </div>
        </header>

        <div className="resources-layout-panel glass-panel">
          <aside className="resources-sidebar-panel">
            <h3>All Trimesters</h3>
            <div className="trimester-list">
              {trimesterOptions.map((option, index) => (
                <button key={option} type="button" className={`trimester-button ${index === 0 ? 'active' : ''}`}>
                  {option}
                </button>
              ))}
            </div>
          </aside>

          <section className="resources-main-panel">
            <div className="resource-card-grid">
              {courseCards.map((course) => (
                <article key={course.code} className="resource-card glass-panel">
                  <div className="resource-card-header">
                    <h3>{course.code}</h3>
                  </div>

                  <p className="resource-meta">Dept: {course.dept} | Trim: {course.trim}</p>

                  <div className="resource-folder-grid">
                    {resourceCategories.map((category) => (
                      <div key={`${course.code}-${category.label}`} className="resource-folder-item">
                        <span className="resource-folder-icon">
                          <ResourceTypeIcon type={category.icon} />
                        </span>
                        <span>{category.label}</span>
                      </div>
                    ))}
                  </div>
                </article>
              ))}
            </div>
          </section>
        </div>
      </main>
    </div>
  )
}
