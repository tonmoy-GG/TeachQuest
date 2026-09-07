import { useEffect, useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { getStoredUser, navItems } from '../utils/appData'
import './QuestionsPage.css'

const teacherNavItems = [
  { label: 'Dashboard', to: '/teacher-dashboard' },
  { label: 'Job Board', to: '/teacher-job-board' },
  { label: 'My Applications', to: '/teacher-applications' },
  { label: 'Study Resources', to: '/teacher-resources' },
  { label: 'Community Q&A', to: '/questions' },
  { label: 'Chat', to: '/teacher-chat' },
]

const emptyQuestion = { title: '', body: '', category: 'General' }

function formatDate(value) {
  if (!value) return 'Just now'
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

export default function QuestionsPage() {
  const navigate = useNavigate()
  const [user] = useState(() => getStoredUser())
  const [questions, setQuestions] = useState([])
  const [selected, setSelected] = useState(null)
  const [questionForm, setQuestionForm] = useState(emptyQuestion)
  const [answer, setAnswer] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const isTeacher = user?.userType === 'teacher'
  const currentNavItems = isTeacher ? teacherNavItems : navItems

  useEffect(() => {
    let active = true
    fetch('/api/questions')
      .then(async (response) => {
        if (!response.ok) throw new Error('Unable to load community questions.')
        return response.json()
      })
      .then((data) => {
        if (active) setQuestions(data)
      })
      .catch((err) => {
        if (active) setError(err.message)
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => { active = false }
  }, [])

  const openQuestion = async (question) => {
    setError('')
    setSelected({ ...question, answers: [] })
    try {
      const response = await fetch(`/api/questions/${question.id}`)
      if (!response.ok) throw new Error('Unable to open this question.')
      setSelected(await response.json())
    } catch (err) {
      setError(err.message)
    }
  }

  const handleQuestionChange = (event) => {
    const { name, value } = event.target
    setQuestionForm((current) => ({ ...current, [name]: value }))
  }

  const submitQuestion = async (event) => {
    event.preventDefault()
    setError('')
    setSaving(true)
    try {
      const response = await fetch('/api/questions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(questionForm),
      })
      if (!response.ok) throw new Error(await response.text() || 'Unable to post your question.')
      const created = await response.json()
      setQuestions((current) => [created, ...current])
      setQuestionForm(emptyQuestion)
      setSelected({ ...created, answers: [] })
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  const submitAnswer = async (event) => {
    event.preventDefault()
    if (!user) {
      navigate('/')
      return
    }
    setError('')
    setSaving(true)
    try {
      const response = await fetch(`/api/questions/${selected.id}/answers`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: user.email, body: answer }),
      })
      if (!response.ok) throw new Error(await response.text() || 'Unable to post your answer.')
      const created = await response.json()
      setSelected((current) => ({ ...current, answers: [...(current.answers || []), created] }))
      setQuestions((current) => current.map((question) => question.id === selected.id
        ? { ...question, answerCount: (question.answerCount || 0) + 1 }
        : question))
      setAnswer('')
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className={`student-dashboard-shell questions-shell ${isTeacher ? 'teacher-dashboard-shell' : ''}`}>
      <header className="dashboard-topbar glass-panel">
        <div className="brand-block">
          <div className="brand-mark">TQ</div>
          <h1>TeachQuest</h1>
        </div>
        {user && <nav className="main-nav" aria-label="Main navigation">
          {currentNavItems.map((item) => <NavLink key={item.to} to={item.to} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>{item.label}</NavLink>)}
        </nav>}
        <div className="dashboard-top-actions">
          {user ? <>
            <div className="user-meta"><span>{user.username || user.email?.split('@')[0]}</span><small>{isTeacher ? 'Teacher' : 'Student'}</small></div>
            <button type="button" className="logout-button" onClick={() => { localStorage.removeItem('teachquest_user'); navigate('/') }}>Logout</button>
          </> : <Link className="primary-soft-button" to="/">Sign in to answer</Link>}
        </div>
      </header>

      <main className="dashboard-canvas questions-canvas">
        <section className="questions-hero">
          <div>
            <span className="mini-label">Community knowledge exchange</span>
            <h1>Ask the room.</h1>
            <p>Post anonymously, get practical answers, and help someone else move forward.</p>
          </div>
          <span className="questions-stat"><strong>{questions.length}</strong> questions shared</span>
        </section>

        {error && <p className="questions-error" role="alert">{error}</p>}

        <div className="questions-layout">
          <section className="questions-feed">
            <form className="question-composer glass-panel" onSubmit={submitQuestion}>
              <div className="composer-heading"><span className="material-symbols-outlined">edit_note</span><div><h2>Ask anonymously</h2><p>Your identity is never shown on your question.</p></div></div>
              <input name="title" value={questionForm.title} onChange={handleQuestionChange} placeholder="What would you like to know?" maxLength="180" required />
              <textarea name="body" value={questionForm.body} onChange={handleQuestionChange} placeholder="Add context so the community can give a useful answer..." rows="4" required />
              <div className="composer-actions"><select name="category" value={questionForm.category} onChange={handleQuestionChange}><option>General</option><option>Programming</option><option>Mathematics</option><option>Study skills</option><option>Career</option></select><button className="primary-soft-button" type="submit" disabled={saving}>{saving ? 'Posting...' : 'Post question'} <span className="material-symbols-outlined">send</span></button></div>
            </form>

            <div className="feed-heading"><h2>Latest questions</h2><span>{loading ? 'Loading...' : 'Open to every registered learner'}</span></div>
            {!loading && questions.length === 0 && <div className="empty-question-state glass-panel"><span className="material-symbols-outlined">forum</span><p>No questions yet. Start the conversation.</p></div>}
            {questions.map((question) => <button type="button" className={`question-card glass-panel ${selected?.id === question.id ? 'selected' : ''}`} key={question.id} onClick={() => openQuestion(question)}><div className="question-card-top"><span className="question-category">{question.category}</span><span>{formatDate(question.createdAt)}</span></div><h3>{question.title}</h3><p>{question.body}</p><div className="question-card-bottom"><span className="anonymous-label"><span className="material-symbols-outlined">visibility_off</span> Anonymous</span><span><span className="material-symbols-outlined">chat_bubble_outline</span>{question.answerCount || 0} answers</span></div></button>)}
          </section>

          <aside className="question-detail glass-panel">
            {!selected ? <div className="detail-placeholder"><span className="material-symbols-outlined">forum</span><h2>Select a question</h2><p>Choose a post to read the conversation and share your answer.</p></div> : <><div className="detail-top"><span className="question-category">{selected.category}</span><span>{formatDate(selected.createdAt)}</span></div><h2>{selected.title}</h2><p className="detail-body">{selected.body}</p><div className="answers-heading"><h3>{selected.answers?.length || 0} answers</h3><span>Registered users</span></div><div className="answers-list">{selected.answers?.length ? selected.answers.map((item) => <article className="answer-item" key={item.id}><div className="answer-avatar">{(item.author || 'U').charAt(0).toUpperCase()}</div><div><strong>{item.author || 'Registered user'}</strong><small>{formatDate(item.createdAt)}</small><p>{item.body}</p></div></article>) : <p className="no-answers">Be the first registered user to answer.</p>}</div><form className="answer-form" onSubmit={submitAnswer}><textarea value={answer} onChange={(event) => setAnswer(event.target.value)} placeholder={user ? 'Share what you know...' : 'Sign in to answer this question'} rows="3" required disabled={!user} /><button className="primary-soft-button" type="submit" disabled={saving || !user}>{user ? 'Post answer' : 'Sign in to answer'} <span className="material-symbols-outlined">reply</span></button></form></>}
          </aside>
        </div>
      </main>
    </div>
  )
}
