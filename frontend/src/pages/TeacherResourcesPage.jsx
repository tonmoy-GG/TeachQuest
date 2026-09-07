import { useEffect, useState } from 'react'
import { NavLink, Navigate, useNavigate } from 'react-router-dom'
import ResourceTypeIcon from '../components/ResourceTypeIcon'
import ResourceCommunityPanel from '../components/ResourceCommunityPanel'
import { getResourceBookmarks, getStoredUser, normalizeStudyResource, resourceCategories, saveResourceBookmarks, trimesterOptions } from '../utils/appData'

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

export default function TeacherResourcesPage() {
  const navigate = useNavigate()
  const [user] = useState(() => getStoredUser())
  const [selectedDept, setSelectedDept] = useState('all-depts')
  const [selectedTrim, setSelectedTrim] = useState('all-trims')
  const [expandedCourseKey, setExpandedCourseKey] = useState(null)
  const [uploadedResources, setUploadedResources] = useState([])
  const [bookmarks, setBookmarks] = useState(() => getResourceBookmarks())
  const [showBookmarked, setShowBookmarked] = useState(false)
  const [points, setPoints] = useState(() => ({ totalPoints: getStoredUser()?.totalPoints || 0 }))

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

    fetch('/api/resources/all')
      .then((response) => response.ok ? response.json() : [])
      .then((resources) => setUploadedResources(Array.isArray(resources) ? resources.map(normalizeStudyResource).filter(Boolean) : []))
      .catch(() => setUploadedResources([]))
  }, [navigate])

  useEffect(() => {
    if (!user?.id) return
    fetch(`/api/users/${user.id}/points`)
      .then((response) => response.ok ? response.json() : null)
      .then((data) => { if (data) setPoints(data) })
      .catch(() => {})
  }, [user?.id])

  if (!user) {
    return <Navigate to="/" replace />
  }

  const displayName = user.username || user.email?.split('@')[0] || 'Teacher'
  const upvoteResource = async (resourceId) => {
    try {
      const response = await fetch(`/api/resources/${resourceId}/upvote?userId=${user.id}`, { method: 'POST' })
      if (!response.ok) throw new Error(await response.text() || 'Unable to upvote resource.')
      const result = await response.json()
      setUploadedResources((current) => current.map((resource) => resource.id === resourceId ? { ...resource, upvotePoints: result.upvotePoints } : resource))
    } catch (error) {
      alert(error.message)
    }
  }

  const verifyResource = async (resourceId) => {
    try {
      const response = await fetch(`/api/resources/${resourceId}/verify?verifierId=${user.id}`, { method: 'POST' })
      if (!response.ok) throw new Error(await response.text() || 'Unable to verify resource.')
      setUploadedResources((current) => current.map((resource) => resource.id === resourceId ? { ...resource, verified: true } : resource))
    } catch (error) {
      alert(error.message)
    }
  }

  const flagResource = async (resourceId, reason, event) => {
    if (!reason) return
    try {
      const response = await fetch(`/api/resources/${resourceId}/flags`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ reporterId: user.id, reason }) })
      if (!response.ok) throw new Error(await response.text() || 'Unable to submit report.')
      event.target.value = ''
      alert('Report submitted for moderator review.')
    } catch (error) {
      alert(error.message)
    }
  }

  const toggleBookmark = (resourceId) => {
    const normalizedId = String(resourceId)
    const nextBookmarks = bookmarks.includes(normalizedId)
      ? bookmarks.filter((id) => id !== normalizedId)
      : [...bookmarks, normalizedId]
    setBookmarks(nextBookmarks)
    saveResourceBookmarks(nextBookmarks)
  }

  const openResource = (course) => {
    if (!course?.fileUrl) return

    const link = document.createElement('a')
    link.href = course.fileUrl
    link.download = course.fileName || course.code || 'resource'
    link.target = '_blank'
    link.rel = 'noopener noreferrer'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }

  const filteredCourses = uploadedResources.filter((course) => {
    const departmentName = String(course.department || '').toLowerCase()
    const deptMatch =
      selectedDept === 'all-depts' ||
      (selectedDept === 'cse' && departmentName.includes('computer science')) ||
      (selectedDept === 'bba' && departmentName.includes('business'))

    const trimMatch =
      selectedTrim === 'all-trims' ||
      String(course.trimester || '').toLowerCase() === String(selectedTrim).toLowerCase()

    return deptMatch && trimMatch && (!showBookmarked || bookmarks.includes(String(course.id)))
  })

  const groupedCourses = Object.values(
    filteredCourses.reduce((groups, course) => {
      const key = `${course.department}::${course.trimester}::${course.code}`
      if (!groups[key]) {
        groups[key] = {
          key,
          code: course.code,
          department: course.department,
          trimester: course.trimester,
          categories: {},
        }
      }

      const categoryKey = course.category || 'resource'
      if (!groups[key].categories[categoryKey]) {
        groups[key].categories[categoryKey] = []
      }

      groups[key].categories[categoryKey].push(course)
      return groups
    }, {}),
  )

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
        <ResourceCommunityPanel user={user} />
        <header className="resources-page-header">
          <div>
            <span className="mini-label">Teacher dashboard</span>
            <h2>Study Resources</h2>
            <p className="resource-points-summary"><span className="material-symbols-outlined">stars</span> {points.totalPoints} points <span className="resource-level-badge">{points.totalPoints >= 100 ? 'Resource Champion' : points.totalPoints >= 30 ? 'Knowledge Builder' : 'New Contributor'}</span></p>
          </div>

          <div className="resources-header-tools">
            <div className="resources-filter-bar">
              <select
                aria-label="Department filter"
                value={selectedDept}
                onChange={(event) => setSelectedDept(event.target.value)}
              >
                <option value="all-depts">All Depts</option>
                <option value="cse">Computer Science</option>
                <option value="bba">BBA</option>
              </select>
              <select
                aria-label="Trimester filter"
                value={selectedTrim}
                onChange={(event) => setSelectedTrim(event.target.value)}
              >
                <option value="all-trims">All Trim</option>
                {trimesterOptions.map((option) => (
                  <option key={option} value={option}>{option}</option>
                ))}
              </select>
              <button type="button" className="filter-button" onClick={() => {}}>Filter</button>
            </div>
            <button type="button" className={`bookmark-filter-button ${showBookmarked ? 'active' : ''}`} onClick={() => setShowBookmarked((value) => !value)}><span className="material-symbols-outlined">bookmark</span> Saved ({bookmarks.length})</button>
            <button type="button" className="primary-soft-button" onClick={() => navigate('/teacher-upload-resources')}>Upload file</button>
          </div>
        </header>

        <div className="resources-layout-panel glass-panel">
          <aside className="resources-sidebar-panel">
            <h3>All Trimesters</h3>
            <div className="trimester-list">
              <button type="button" className={`trimester-button ${selectedTrim === 'all-trims' ? 'active' : ''}`} onClick={() => setSelectedTrim('all-trims')}>
                All Trimesters
              </button>
              {trimesterOptions.map((option) => (
                <button
                  key={option}
                  type="button"
                  className={`trimester-button ${selectedTrim === option ? 'active' : ''}`}
                  onClick={() => setSelectedTrim(option)}
                >
                  {option}
                </button>
              ))}
            </div>
          </aside>

          <section className="resources-main-panel">
            <div className="resource-card-grid">
              {groupedCourses.length === 0 ? (
                <div className="empty-state glass-panel">
                  <p>No resources match the selected filters.</p>
                </div>
              ) : (
                groupedCourses.map((courseGroup) => (
                  <article key={courseGroup.key} className="resource-card glass-panel">
                    <div className="resource-card-header">
                      <h3>{courseGroup.code}</h3>
                    </div>

                    <p className="resource-meta">Dept: {courseGroup.department} | Trim: {courseGroup.trimester}</p>

                    <div className="resource-folder-grid">
                      {Object.entries(courseGroup.categories).map(([categoryKey, resources]) => {
                        const categoryMeta = resources[0]
                        const categoryLabel = categoryMeta.categoryLabel || resourceCategories.find((item) => item.icon === categoryMeta.categoryIcon)?.label || 'Resource'
                        const categoryIcon = categoryMeta.categoryIcon || 'notes'
                        const isExpanded = showBookmarked || expandedCourseKey === `${courseGroup.key}-${categoryKey}`

                        return (
                          <div key={`${courseGroup.key}-${categoryKey}`} className="resource-category-block">
                            <button
                              type="button"
                              className="resource-folder-item"
                              onClick={() => setExpandedCourseKey(isExpanded ? null : `${courseGroup.key}-${categoryKey}`)}
                              style={{ width: '100%', cursor: 'pointer', textAlign: 'left' }}
                            >
                              <span className="resource-folder-icon">
                                <ResourceTypeIcon type={categoryIcon} />
                              </span>
                              <span>{categoryLabel}</span>
                            </button>

                            {isExpanded && (
                              <div className="resource-file-list">
                                {resources.map((resource) => (
                                  <div
                                    key={resource.id || `${resource.fileName}-${resource.fileUrl}`}
                                    className="resource-file-item"
                                  >
                                    <span className="resource-file-name">{resource.fileName || `${categoryLabel} File`}</span>
                                    <button type="button" className="resource-open-label" onClick={() => openResource(resource)}>Open</button>
                                    <button type="button" className="resource-upvote-button" onClick={() => upvoteResource(resource.id)}><span className="material-symbols-outlined">arrow_upward</span>{resource.upvotePoints || 0}</button>
                                    {!resource.verified && <button type="button" className="resource-verify-button" onClick={() => verifyResource(resource.id)}>Verify</button>}
                                    <button type="button" className={`resource-bookmark-button ${bookmarks.includes(String(resource.id)) ? 'saved' : ''}`} aria-label={bookmarks.includes(String(resource.id)) ? 'Remove bookmark' : 'Bookmark resource'} onClick={() => toggleBookmark(resource.id)}><span className="material-symbols-outlined">{bookmarks.includes(String(resource.id)) ? 'bookmark' : 'bookmark_border'}</span></button>
                                    <select className="resource-flag-select" aria-label="Report resource" defaultValue="" onChange={(event) => flagResource(resource.id, event.target.value, event)}><option value="">Report</option><option value="OUTDATED">Outdated</option><option value="INCORRECT">Incorrect</option><option value="DUPLICATE">Duplicate</option></select>
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>
                        )
                      })}
                    </div>
                  </article>
                ))
              )}
            </div>
          </section>
        </div>
      </main>
    </div>
  )
}
