export const STORAGE_KEY = 'teachquest_user'
export const POSTED_JOBS_KEY = 'teachquest_posted_jobs'
export const TEACHER_APPLICATIONS_KEY = 'teachquest_teacher_applications'
export const USERS_KEY = 'teachquest_users'
export const CHAT_MESSAGES_KEY = 'teachquest_chat_messages'

export function getUserScopedStorageKey(prefix) {
  const currentUser = getStoredUser()
  const userEmail = String(currentUser?.email || '').trim().toLowerCase()
  return userEmail ? `${prefix}_${userEmail}` : prefix
}

export const navItems = [
  { label: 'Dashboard', to: '/dashboard' },
  { label: 'My Posted Jobs', to: '/posted-jobs' },
  { label: 'Post a New Job', to: '/jobs' },
  { label: 'Study Resources', to: '/resources' },
  { label: 'Chat', to: '/chat' },
]

export const floatingTags = ['📚 Study Resources', '👨‍🏫 Tutor Requests', '💬 Live Chat', '📝 Quiz Progress']

export const stats = [
  { value: '12.4k', label: 'Active students' },
  { value: '860', label: 'Tutors online' },
  { value: '3.2k', label: 'Job matches' },
  { value: '94%', label: 'Success rate' },
]

export const currentHires = [
  { name: 'MR Rafiq', subject: 'Data Structure and Algorithms', classTime: '10:30 PM', days: 'SAT SUN' },
  { name: 'MR Sujon', subject: 'Discrete Mathematics', classTime: '7:15 PM', days: 'MON TUES WED' },
  { name: 'MR Kuddus', subject: 'Python And Java', classTime: '7:00 AM', days: 'FRI MON' },
]

export const previousHires = [
  { name: 'MR Rahim', subject: 'Statistics', payment: 'DUE', rating: 4 },
  { name: 'MR Sattar', subject: 'Calculus', payment: 'PAID', rating: 5 },
]

export const dashboardCards = [
  { title: 'Course Progress', value: '08', subtitle: 'Active this term' },
  { title: 'Quiz Score', value: '92%', subtitle: 'Average performance', highlight: true },
  { title: 'Learning Time', value: '26.5h', subtitle: 'Past 30 days' },
]

export const tasks = [
  { title: 'Web Development Session', time: '10:00 AM' },
  { title: 'Algebra Revision', time: '1:30 PM' },
  { title: 'Job application review', time: '4:00 PM' },
]

export const fallbackJobs = [
  { title: 'Math Tutor Needed', subject: 'Mathematics', salary: '$25/hr', address: 'Dhaka', days: '4 days/week' },
  { title: 'Physics Problem Solver', subject: 'Physics', salary: '$20/hr', address: 'Chattogram', days: '3 days/week' },
  { title: 'Web Design Mentor', subject: 'UI/UX', salary: '$30/hr', address: 'Online', days: '2 days/week' },
]

export const resources = [
  { title: 'Modern Java Basics', type: 'PDF', date: 'Updated 2 days ago' },
  { title: 'Chemistry Practice Set', type: 'Quiz', date: 'Updated 1 week ago' },
  { title: 'Programming Notes', type: 'Lecture', date: 'Updated today' },
]

export const trimesterOptions = Array.from({ length: 12 }, (_, index) => `Trimester ${index + 1}`)

export const resourceCategories = [
  { label: 'Class Recording', icon: 'recording' },
  { label: 'Class Notes', icon: 'notes' },
  { label: 'CT Questions', icon: 'quiz' },
  { label: 'MID Questions', icon: 'exam' },
  { label: 'Final Questions', icon: 'final' },
]

export const courseCards = [
  { code: 'CSE3711', dept: 'Computer Science', trim: 10 },
  { code: 'CSE3721', dept: 'Computer Science', trim: 10 },
  { code: 'CSE3731', dept: 'Computer Science', trim: 10 },
]

export function getStoredUser() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function getRegisteredUsers() {
  try {
    const raw = localStorage.getItem(USERS_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

export function getUniqueRegisteredUsers() {
  const seen = new Set()

  return getRegisteredUsers().filter((user) => {
    const email = (user.email || '').trim().toLowerCase()
    if (!email || seen.has(email)) return false
    seen.add(email)
    return true
  })
}

export async function syncRegisteredUsers() {
  try {
    const response = await fetch('/api/users')
    if (!response.ok) return getUniqueRegisteredUsers()

    const users = await response.json()
    if (!Array.isArray(users)) return getUniqueRegisteredUsers()

    saveRegisteredUsers(users)
    return getUniqueRegisteredUsers()
  } catch {
    return getUniqueRegisteredUsers()
  }
}

export function getChatMessages() {
  try {
    const raw = localStorage.getItem(CHAT_MESSAGES_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object') return {}

    return Object.fromEntries(
      Object.entries(parsed).map(([roomKey, messages]) => [
        roomKey,
        Array.isArray(messages) ? messages.filter((message) => message && message.senderEmail) : [],
      ]),
    )
  } catch {
    return {}
  }
}

export function saveChatMessages(messages) {
  localStorage.setItem(CHAT_MESSAGES_KEY, JSON.stringify(messages))
}

export function saveRegisteredUsers(users) {
  localStorage.setItem(USERS_KEY, JSON.stringify(users))
}

export function findRegisteredUser(email, password) {
  const normalizedEmail = (email || '').trim().toLowerCase()
  const users = getRegisteredUsers()

  return users.find((user) => {
    const emailMatches = (user.email || '').trim().toLowerCase() === normalizedEmail
    const passwordMatches = String(user.password || '') === String(password || '')
    return emailMatches && passwordMatches
  }) || null
}

export function getPostedJobs() {
  try {
    const scopedKey = getUserScopedStorageKey(POSTED_JOBS_KEY)
    const raw = localStorage.getItem(scopedKey)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

export function getAllPostedJobs() {
  try {
    const entries = []
    const seen = new Set()
    const legacyKey = POSTED_JOBS_KEY
    const legacyRaw = localStorage.getItem(legacyKey)

    if (legacyRaw) {
      const legacyJobs = JSON.parse(legacyRaw)
      if (Array.isArray(legacyJobs)) {
        legacyJobs.forEach((job) => {
          const key = String(job.id || `${job.title}-${job.subject}-${job.location}`)
          if (!seen.has(key)) {
            seen.add(key)
            entries.push(job)
          }
        })
      }
    }

    for (const key of Object.keys(localStorage)) {
      if (!key.startsWith(`${POSTED_JOBS_KEY}_`)) continue

      const raw = localStorage.getItem(key)
      if (!raw) continue

      const parsed = JSON.parse(raw)
      if (!Array.isArray(parsed)) continue

      parsed.forEach((job) => {
        const jobKey = String(job.id || `${job.title}-${job.subject}-${job.location}`)
        if (!seen.has(jobKey)) {
          seen.add(jobKey)
          entries.push(job)
        }
      })
    }

    return entries
  } catch {
    return []
  }
}

export function findPostedJobStorageKey(jobId) {
  const targetId = String(jobId)

  for (const key of Object.keys(localStorage)) {
    if (!key.startsWith(`${POSTED_JOBS_KEY}_`) && key !== POSTED_JOBS_KEY) continue

    try {
      const raw = localStorage.getItem(key)
      if (!raw) continue
      const parsed = JSON.parse(raw)
      if (!Array.isArray(parsed)) continue

      if (parsed.some((job) => String(job.id) === targetId)) {
        return key
      }
    } catch {
      // ignore malformed entries
    }
  }

  return getUserScopedStorageKey(POSTED_JOBS_KEY)
}

export function savePostedJobs(jobs) {
  const scopedKey = getUserScopedStorageKey(POSTED_JOBS_KEY)
  localStorage.setItem(scopedKey, JSON.stringify(jobs))
}

export async function parseResponse(response) {
  const text = await response.text()
  if (!text) return null

  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}
