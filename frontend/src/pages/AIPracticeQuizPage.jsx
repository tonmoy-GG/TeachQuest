import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import DashboardLayoutPage from '../components/DashboardLayoutPage'
import { getStoredUser } from '../utils/appData'
import './AIPracticeQuizPage.css'

const LEVELS = [
  { level: 1, name: 'Easy', detail: 'Recall and core concepts', questionCount: 8, durationSeconds: 600, passThresholdPercent: 70 },
  { level: 2, name: 'Medium', detail: 'Apply concepts to problems', questionCount: 10, durationSeconds: 900, passThresholdPercent: 70 },
  { level: 3, name: 'Hard', detail: 'Reason across concepts and edge cases', questionCount: 12, durationSeconds: 1200, passThresholdPercent: 75 },
  { level: 4, name: 'Mock Test', detail: 'Mixed, exam-style challenge', questionCount: 20, durationSeconds: 1800, passThresholdPercent: 80 },
  { level: 5, name: 'Expert', detail: 'Optional bonus challenge', questionCount: 15, durationSeconds: 1500, passThresholdPercent: 85 },
]

async function request(url, options) {
  const response = await fetch(url, options)
  const raw = await response.text()
  let data
  try { data = raw ? JSON.parse(raw) : null } catch { data = raw }
  if (!response.ok) throw new Error(typeof data === 'string' ? data : data?.message || 'Practice quiz request failed.')
  return data
}

export default function AIPracticeQuizPage() {
  const user = getStoredUser()
  const [courses, setCourses] = useState([])
  const [progress, setProgress] = useState([])
  const [levels, setLevels] = useState(LEVELS)
  const [courseId, setCourseId] = useState('')
  const [history, setHistory] = useState([])
  const [selectedLevel, setSelectedLevel] = useState(1)
  const [settings, setSettings] = useState({ topic: '', questionMix: 'BOTH' })
  const [attempt, setAttempt] = useState(null)
  const [result, setResult] = useState(null)
  const [answers, setAnswers] = useState({})
  const [remaining, setRemaining] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const submittingRef = useRef(false)

  const userId = user?.id
  const selectedCourse = courses.find((course) => String(course.id) === String(courseId))
  const selectedProgress = progress.find((item) => String(item.course.id) === String(courseId))
  const highestUnlocked = selectedProgress?.highestUnlockedLevel || 1
  const unfinishedAttempt = history.find((item) => item.status === 'IN_PROGRESS')
  const selectedLevelConfig = levels.find((level) => level.level === selectedLevel) || levels[0]
  const launchConfig = unfinishedAttempt || selectedLevelConfig

  const loadOverview = useCallback(async () => {
    if (!userId) return
    try {
      const [courseList, progressList] = await Promise.all([
        request(`/api/practice-quizzes/courses?userId=${userId}`),
        request(`/api/practice-quizzes/progress?userId=${userId}`),
      ])
      setCourses(courseList)
      setProgress(progressList)
      setCourseId((current) => current || String(courseList[0]?.id || ''))
    } catch (loadError) { setError(loadError.message) }
  }, [userId])

  const loadHistory = useCallback(async () => {
    if (!userId || !courseId) return
    try {
      setHistory(await request(`/api/practice-quizzes/courses/${courseId}/history?userId=${userId}`))
    } catch (loadError) { setError(loadError.message) }
  }, [courseId, userId])

  useEffect(() => {
    if (!userId) return undefined
    let current = true
    Promise.all([
      request(`/api/practice-quizzes/courses?userId=${userId}`),
      request(`/api/practice-quizzes/progress?userId=${userId}`),
      request('/api/practice-quizzes/config'),
    ]).then(([courseList, progressList, config]) => {
      if (!current) return
      setCourses(courseList)
      setProgress(progressList)
      setLevels(config.levels || LEVELS)
      setCourseId((selected) => selected || String(courseList[0]?.id || ''))
    }).catch((loadError) => { if (current) setError(loadError.message) })
    return () => { current = false }
  }, [userId])

  useEffect(() => {
    if (!userId || !courseId) return undefined
    let current = true
    request(`/api/practice-quizzes/courses/${courseId}/history?userId=${userId}`)
      .then((items) => { if (current) setHistory(items) })
      .catch((loadError) => { if (current) setError(loadError.message) })
    return () => { current = false }
  }, [courseId, userId])

  const submitAttempt = useCallback(async () => {
    if (!attempt || submittingRef.current) return
    submittingRef.current = true
    setBusy(true)
    setError('')
    try {
      const submitted = await request(`/api/practice-quizzes/attempts/${attempt.id}/submit?userId=${userId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ answers: Object.entries(answers).map(([questionId, answer]) => ({ questionId: Number(questionId), answer })) }),
      })
      setAttempt(null)
      setRemaining(null)
      setResult(submitted)
      await Promise.all([loadOverview(), loadHistory()])
    } catch (submitError) { setError(submitError.message) }
    finally { submittingRef.current = false; setBusy(false) }
  }, [answers, attempt, loadHistory, loadOverview, userId])

  useEffect(() => {
    if (!attempt) return undefined
    const timer = window.setInterval(() => {
      const start = new Date(attempt.startedAt).getTime()
      const nextRemaining = Math.max(0, Math.ceil((start + attempt.durationSeconds * 1000 - Date.now()) / 1000))
      setRemaining(nextRemaining)
      if (nextRemaining === 0 && !submittingRef.current) void submitAttempt()
    }, 1000)
    return () => window.clearInterval(timer)
  }, [attempt, submitAttempt])

  if (!user) return <Navigate to="/" replace />

  const startAttempt = async () => {
    setBusy(true)
    setError('')
    setResult(null)
    setAnswers({})
    try {
      const started = await request(`/api/practice-quizzes/courses/${courseId}/attempts?userId=${userId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ level: selectedLevel, topic: settings.topic, questionMix: settings.questionMix }),
      })
      setRemaining(Math.max(0, Math.ceil((new Date(started.startedAt).getTime() + started.durationSeconds * 1000 - Date.now()) / 1000)))
      setAttempt(started)
    } catch (startError) { setError(startError.message) }
    finally { setBusy(false) }
  }

  const resumeAttempt = async () => {
    if (!unfinishedAttempt) return
    setBusy(true)
    setError('')
    try {
      const resumed = await request(`/api/practice-quizzes/attempts/${unfinishedAttempt.id}?userId=${userId}`)
      setSelectedLevel(resumed.level)
      setResult(null)
      setAnswers({})
      setRemaining(Math.max(0, Math.ceil((new Date(resumed.startedAt).getTime() + resumed.durationSeconds * 1000 - Date.now()) / 1000)))
      setAttempt(resumed)
    } catch (resumeError) { setError(resumeError.message) }
    finally { setBusy(false) }
  }

  const remainingLabel = remaining === null ? '' : `${Math.floor(remaining / 60)}:${String(remaining % 60).padStart(2, '0')}`
  const levelHistory = history.filter((item) => item.level === selectedLevel)

  return <DashboardLayoutPage title="AI practice">
    <div className="practice-page">
      {error && <div className="practice-alert" role="alert">{error}<button type="button" onClick={() => setError('')} aria-label="Dismiss error">×</button></div>}

      {attempt ? <section className="practice-session" aria-live="polite">
        <header className="practice-session-heading">
          <div><span className="practice-eyebrow">{selectedCourse?.name} / {settings.topic}</span><h2>{levels[attempt.level - 1]?.name} level</h2></div>
          <div className={`practice-timer ${remaining < 60 ? 'urgent' : ''}`} aria-label={`${remainingLabel} remaining`}>{remainingLabel}</div>
        </header>
        <p className="practice-session-note">Answer each question, then submit before the server-enforced time limit.</p>
        <div className="practice-question-list">
          {attempt.questions.map((question) => <fieldset className="practice-question" key={question.id}>
            <legend><span>{String(question.order).padStart(2, '0')}</span>{question.question}</legend>
            {question.type === 'MCQ' ? <div className="practice-options">
              {question.options.map((option) => <label className={answers[question.id] === option.id ? 'selected' : ''} key={option.id}>
                <input type="radio" name={`answer-${question.id}`} value={option.id} checked={answers[question.id] === option.id} onChange={() => setAnswers((current) => ({ ...current, [question.id]: option.id }))} />
                <b>{option.id}</b><span>{option.text}</span>
              </label>)}
            </div> : <textarea maxLength={4000} rows={4} placeholder="Write your answer" value={answers[question.id] || ''} onChange={(event) => setAnswers((current) => ({ ...current, [question.id]: event.target.value }))} />}
          </fieldset>)}
        </div>
        <footer className="practice-session-footer"><span>{Object.keys(answers).length} of {attempt.questions.length} answered</span><button type="button" className="practice-primary" disabled={busy} onClick={() => void submitAttempt()}>{busy ? 'Grading…' : 'Submit attempt'}</button></footer>
      </section> : result ? <section className="practice-results">
        <header className="practice-results-heading"><span className="practice-eyebrow">{result.status === 'EXPIRED' ? 'Time expired' : result.passed ? 'Level cleared' : 'Attempt complete'}</span><h2>{result.scorePercent?.toFixed(0) ?? 0}<small>%</small></h2><p>{result.passed ? `Level ${result.level} passed. ${result.level >= 4 ? 'Course mastered.' : 'The next level is unlocked.'}` : `You need ${result.passThresholdPercent}% to pass. Try again when you are ready.`}</p>
          {result.certificate && <div className="practice-certificate-award"><strong>{result.certificate.tier} certificate awarded</strong><span>Average score {Number(result.certificate.averageScore).toFixed(2)}%</span><Link to="/certificates">Open certificate trophy case</Link></div>}
        </header>
        {result.questions?.map((question) => <article className="practice-review" key={question.id}><div className="practice-review-state">{question.correct ? 'Correct' : 'Review'}</div><h3>{question.question}</h3><p>Your answer: <strong>{question.answer || 'No answer'}</strong></p>{question.feedback && <p>{question.feedback}</p>}{question.expectedAnswer && <p className="practice-expected">Expected: {question.expectedAnswer}</p>}</article>)}
        <button type="button" className="practice-secondary" onClick={() => { setResult(null); setSelectedLevel(result.level) }}>Back to levels</button>
      </section> : <>
        <section className="practice-setup">
          <div className="practice-intro"><span className="practice-eyebrow">Fresh questions every attempt</span><h2>Build skill, one level at a time.</h2><p>Choose a subject and topic. Pass each checkpoint to unlock the next challenge.</p></div>
          <div className="practice-config">
            <label>Course<select value={courseId} onChange={(event) => { setCourseId(event.target.value); setResult(null) }}>{courses.map((course) => <option key={course.id} value={course.id}>{course.name}</option>)}</select></label>
            <label>Topic<input maxLength={120} value={settings.topic} placeholder="e.g. Binary trees" onChange={(event) => setSettings((current) => ({ ...current, topic: event.target.value }))} /></label>
            <label>Question mix<select value={settings.questionMix} onChange={(event) => setSettings((current) => ({ ...current, questionMix: event.target.value }))}><option value="MCQ_ONLY">Multiple choice</option><option value="SHORT_ANSWER_ONLY">Short answer</option><option value="BOTH">Mixed</option></select></label>
          </div>
        </section>

        <section className="practice-level-section"><header><div><span className="practice-eyebrow">Your progression</span><h2>{selectedCourse?.name || 'Choose a course'}</h2></div><span className={`practice-mastered ${selectedProgress?.status === 'MASTERED' ? 'is-mastered' : ''}`}>{selectedProgress?.status === 'MASTERED' ? 'Mastered' : `Level ${highestUnlocked} unlocked`}</span></header>
          <div className="practice-level-map">{levels.map((level) => {
            const locked = level.level > highestUnlocked
            const completed = level.level < highestUnlocked || history.some((item) => item.level === level.level && item.passed)
            return <button type="button" className={`practice-level ${selectedLevel === level.level ? 'chosen' : ''} ${locked ? 'locked' : ''} ${completed ? 'completed' : ''}`} key={level.level} disabled={locked} onClick={() => { setSelectedLevel(level.level); setResult(null) }}>
              <span className="practice-level-number">{completed ? '✓' : `0${level.level}`}</span><strong>{level.name}</strong><small>{level.detail}</small><small>{level.questionCount} questions · {Math.round(level.durationSeconds / 60)} min · pass {level.passThresholdPercent}%</small><span className="practice-level-lock">{locked ? 'Locked' : completed ? 'Cleared' : 'Available'}</span>
            </button>
          })}</div>
          <div className="practice-launch"><div><strong>{unfinishedAttempt ? `Level ${unfinishedAttempt.level}: attempt in progress` : `Level ${selectedLevel}: ${selectedLevelConfig?.name}`}</strong><span>{unfinishedAttempt ? `The server timer continued while you were away. ${launchConfig.questionCount} questions · ${Math.round(launchConfig.durationSeconds / 60)} min · pass ${launchConfig.passThresholdPercent}%.` : `${launchConfig.questionCount} questions · ${Math.round(launchConfig.durationSeconds / 60)} min · pass ${launchConfig.passThresholdPercent}%. Retries generate a new set.`}</span></div><button type="button" className="practice-primary" disabled={busy || (!unfinishedAttempt && (!courseId || !settings.topic.trim()))} onClick={() => void (unfinishedAttempt ? resumeAttempt() : startAttempt())}>{busy ? (unfinishedAttempt ? 'Resuming…' : 'Generating…') : unfinishedAttempt ? 'Resume quiz' : 'Generate quiz'}</button></div>
        </section>

        <section className="practice-history"><header><div><span className="practice-eyebrow">Course activity</span><h2>Level {selectedLevel} attempts</h2></div></header>{levelHistory.length === 0 ? <p className="practice-empty">No attempts at this level yet.</p> : <div className="practice-history-list">{levelHistory.map((item) => <div className="practice-history-row" key={item.id}><span>{item.createdAt ? new Date(item.createdAt).toLocaleString() : item.submittedAt ? new Date(item.submittedAt).toLocaleString() : item.status}</span><strong>{item.scorePercent == null ? item.status : `${item.scorePercent.toFixed(0)}%`}</strong><span>{item.passed ? 'Passed' : item.status === 'EXPIRED' ? 'Expired' : 'Not passed'}</span></div>)}</div>}</section>
      </>}
    </div>
  </DashboardLayoutPage>
}