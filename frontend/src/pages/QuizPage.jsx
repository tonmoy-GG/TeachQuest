import { useCallback, useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import DashboardLayoutPage from '../components/DashboardLayoutPage'
import { getStoredUser } from '../utils/appData'

const newQuestion = () => ({ text: '', type: 'MULTIPLE_CHOICE', options: ['', '', '', ''], correctAnswer: '', explanation: '' })

async function api(url, options) {
  const response = await fetch(url, options)
  const raw = await response.text()
  let data
  try { data = raw ? JSON.parse(raw) : null } catch { data = raw }
  if (!response.ok) throw new Error(typeof data === 'string' ? data : data?.message || 'Quiz request failed.')
  return data
}

function TutorQuizHub({ user }) {
  const [form, setForm] = useState({ title: '', course: '', topic: '', timeLimitMinutes: '', attemptsAllowed: 1, dueDate: '', assignmentType: 'OPEN', studentIds: [], groupId: '', questions: [newQuestion()] })
  const [students, setStudents] = useState([])
  const [groups, setGroups] = useState([])
  const [quizzes, setQuizzes] = useState([])
  const [results, setResults] = useState([])
  const [selectedQuiz, setSelectedQuiz] = useState(null)
  const [error, setError] = useState('')
  const [saved, setSaved] = useState('')

  const load = useCallback(async () => {
    try {
      const [eligible, rooms, authored] = await Promise.all([
        api(`/api/chat/groups/eligible-students?tutorId=${user.id}`),
        api(`/api/chat/groups?userId=${user.id}`),
        api(`/api/authored-quizzes/tutor/${user.id}`),
      ])
      setStudents(eligible)
      setGroups(rooms)
      setQuizzes(authored)
    } catch (err) { setError(err.message) }
  }, [user.id])

  useEffect(() => { const initialLoad = window.setTimeout(() => void load(), 0); return () => window.clearTimeout(initialLoad) }, [load])

  const updateQuestion = (index, key, value) => setForm((current) => ({ ...current, questions: current.questions.map((question, itemIndex) => itemIndex === index ? { ...question, [key]: value } : question) }))
  const updateOption = (questionIndex, optionIndex, value) => setForm((current) => ({ ...current, questions: current.questions.map((question, index) => index === questionIndex ? { ...question, options: question.options.map((option, itemIndex) => itemIndex === optionIndex ? value : option) } : question) }))
  const toggleStudent = (id) => setForm((current) => ({ ...current, studentIds: current.studentIds.includes(id) ? current.studentIds.filter((item) => item !== id) : [...current.studentIds, id] }))

  const createQuiz = async (event) => {
    event.preventDefault()
    setError('')
    try {
      await api('/api/authored-quizzes', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...form, authorId: user.id, timeLimitMinutes: form.timeLimitMinutes ? Number(form.timeLimitMinutes) : null, attemptsAllowed: Number(form.attemptsAllowed), dueDate: form.dueDate || null, studentIds: form.assignmentType === 'STUDENTS' ? form.studentIds : [], groupId: form.assignmentType === 'GROUP' ? Number(form.groupId) : null }),
      })
      setSaved('Quiz created successfully.')
      setForm({ title: '', course: '', topic: '', timeLimitMinutes: '', attemptsAllowed: 1, dueDate: '', assignmentType: 'OPEN', studentIds: [], groupId: '', questions: [newQuestion()] })
      await load()
    } catch (err) { setError(err.message) }
  }

  const showResults = async (quiz) => {
    setSelectedQuiz(quiz)
    try { setResults(await api(`/api/authored-quizzes/${quiz.id}/results?tutorId=${user.id}`)) } catch (err) { setError(err.message) }
  }

  const reviewAnswer = async (result, answer) => {
    const score = Number(window.prompt('Score this short answer: 0 or 1', '1'))
    if (![0, 1].includes(score)) return
    const feedback = window.prompt('Optional feedback', '') || ''
    try {
      await api(`/api/authored-quizzes/attempts/${result.id}/review?questionId=${answer.questionId}&tutorId=${user.id}&score=${score}&feedback=${encodeURIComponent(feedback)}`, { method: 'POST' })
      await showResults(selectedQuiz)
    } catch (err) { setError(err.message) }
  }

  return <DashboardLayoutPage title="Tutor quizzes" buttonLabel="Manual quiz">
    <section className="quiz-author-layout">
      <form className="panel glass authored-quiz-form" onSubmit={createQuiz}>
        <span className="mini-label">Tutor authored</span><h2>Create a quiz</h2>
        {error && <div className="quiz-error">{error}</div>}{saved && <div className="quiz-success">{saved}</div>}
        <div className="quiz-form-grid">
          <input required placeholder="Quiz title" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} />
          <input required placeholder="Course" value={form.course} onChange={(event) => setForm({ ...form, course: event.target.value })} />
          <input placeholder="Topic tag" value={form.topic} onChange={(event) => setForm({ ...form, topic: event.target.value })} />
          <input type="number" min="1" placeholder="Time limit (minutes)" value={form.timeLimitMinutes} onChange={(event) => setForm({ ...form, timeLimitMinutes: event.target.value })} />
          <input required type="number" min="1" placeholder="Attempts allowed" value={form.attemptsAllowed} onChange={(event) => setForm({ ...form, attemptsAllowed: event.target.value })} />
          <input type="datetime-local" value={form.dueDate} onChange={(event) => setForm({ ...form, dueDate: event.target.value })} />
        </div>
        <div className="quiz-assignment-row">
          <select value={form.assignmentType} onChange={(event) => setForm({ ...form, assignmentType: event.target.value })}><option value="OPEN">Open to course students</option><option value="STUDENTS">Selected hired students</option><option value="GROUP">Group chat members</option></select>
          {form.assignmentType === 'GROUP' && <select required value={form.groupId} onChange={(event) => setForm({ ...form, groupId: event.target.value })}><option value="">Choose group</option>{groups.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}</select>}
          {form.assignmentType === 'STUDENTS' && <div className="quiz-check-list">{students.map((student) => <label key={student.id}><input type="checkbox" checked={form.studentIds.includes(student.id)} onChange={() => toggleStudent(student.id)} />{student.name}</label>)}</div>}
        </div>
        <div className="authored-question-list">
          {form.questions.map((question, index) => <fieldset className="authored-question" key={index}><legend>Question {index + 1}</legend><input required placeholder="Question text" value={question.text} onChange={(event) => updateQuestion(index, 'text', event.target.value)} /><select value={question.type} onChange={(event) => updateQuestion(index, 'type', event.target.value)}><option value="MULTIPLE_CHOICE">Multiple choice</option><option value="TRUE_FALSE">True / false</option><option value="SHORT_ANSWER">Short answer</option></select>{question.type === 'MULTIPLE_CHOICE' && <div className="quiz-options-grid">{question.options.map((option, optionIndex) => <input required key={optionIndex} placeholder={`Option ${String.fromCharCode(65 + optionIndex)}`} value={option} onChange={(event) => updateOption(index, optionIndex, event.target.value)} />)}</div>}{question.type === 'TRUE_FALSE' && <select value={question.correctAnswer} onChange={(event) => updateQuestion(index, 'correctAnswer', event.target.value)}><option value="">Correct answer</option><option value="true">True</option><option value="false">False</option></select>}{question.type === 'MULTIPLE_CHOICE' && <input required placeholder="Correct option text" value={question.correctAnswer} onChange={(event) => updateQuestion(index, 'correctAnswer', event.target.value)} />}<input placeholder="Optional explanation" value={question.explanation} onChange={(event) => updateQuestion(index, 'explanation', event.target.value)} />{form.questions.length > 1 && <button type="button" className="text-action" onClick={() => setForm({ ...form, questions: form.questions.filter((_, itemIndex) => itemIndex !== index) })}>Remove question</button>}</fieldset>)}
        </div>
        <div className="quiz-form-actions"><button type="button" className="secondary-soft-button" onClick={() => setForm({ ...form, questions: [...form.questions, newQuestion()] })}>Add question</button><button className="primary-soft-button" type="submit">Create quiz</button></div>
      </form>
      <aside className="panel glass authored-quiz-list"><span className="mini-label">Your library</span><h2>Authored quizzes</h2>{quizzes.length === 0 ? <p className="quiz-muted">No manual quizzes yet.</p> : quizzes.map((quiz) => <button type="button" className={`authored-quiz-item ${selectedQuiz?.id === quiz.id ? 'active' : ''}`} key={quiz.id} onClick={() => showResults(quiz)}><strong>{quiz.title}</strong><span>{quiz.course} • {quiz.questionCount} questions</span></button>)}{selectedQuiz && <div className="quiz-results"><h3>Results and review queue</h3>{results.length === 0 ? <p className="quiz-muted">No attempts yet.</p> : results.map((result) => <div className="quiz-result-card" key={result.id}><div className="quiz-result-row"><span>Student {result.studentId}</span><strong>{result.totalScore} pts</strong><small>{result.status}</small></div>{result.answers?.map((answer) => { const question = result.questions?.find((item) => item.id === answer.questionId); return <div className="review-item" key={answer.id}><span>{question?.question}</span><small>{answer.answer || 'No answer'}</small>{question?.questionType === 'SHORT_ANSWER' && !answer.reviewed && <button type="button" className="text-action" onClick={() => reviewAnswer(result, answer)}>Review answer</button>}</div> })}</div>)}</div>}</aside>
    </section>
  </DashboardLayoutPage>
}

function StudentQuizHub({ user }) {
  const [quizzes, setQuizzes] = useState([])
  const [active, setActive] = useState(null)
  const [attempt, setAttempt] = useState(null)
  const [answers, setAnswers] = useState({})
  const [seconds, setSeconds] = useState(null)
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')

  const load = useCallback(async () => { try { setQuizzes(await api(`/api/authored-quizzes/student/${user.id}`)) } catch (err) { setError(err.message) } }, [user.id])
  useEffect(() => { const initialLoad = window.setTimeout(() => void load(), 0); return () => window.clearTimeout(initialLoad) }, [load])
  useEffect(() => { if (!attempt?.expiresAt) return undefined; const tick = () => setSeconds(Math.max(0, Math.floor((new Date(attempt.expiresAt).getTime() - Date.now()) / 1000))); tick(); const interval = window.setInterval(tick, 1000); return () => window.clearInterval(interval) }, [attempt])

  const openQuiz = async (quiz) => { try { setActive(await api(`/api/authored-quizzes/${quiz.id}?studentId=${user.id}`)); setAttempt(null); setResult(null); setAnswers({}) } catch (err) { setError(err.message) } }
  const start = async () => { try { setAttempt(await api(`/api/authored-quizzes/${active.id}/attempts?studentId=${user.id}`, { method: 'POST' })) } catch (err) { setError(err.message) } }
  const submit = async () => { try { const next = await api(`/api/authored-quizzes/${active.id}/attempts/${attempt.id}/submit?studentId=${user.id}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(answers) }); setResult(next); setAttempt(null); await load() } catch (err) { setError(err.message) } }
  const remaining = seconds === null ? '' : `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`

  return <DashboardLayoutPage title="My quizzes" buttonLabel="Quiz hub"><section className="student-quiz-layout"><div className="panel glass"><span className="mini-label">Assigned and open</span><h2>My quizzes</h2>{error && <div className="quiz-error">{error}</div>}{quizzes.length === 0 ? <p className="quiz-muted">No quizzes are available.</p> : quizzes.map((quiz) => <button type="button" className="student-quiz-item" key={quiz.id} onClick={() => openQuiz(quiz)}><span><strong>{quiz.title}</strong><small>{quiz.course} • {quiz.questionCount} questions</small></span><b className={`quiz-status ${String(quiz.status).toLowerCase()}`}>{quiz.status.replace('_', ' ')}</b></button>)}</div><div className="panel glass quiz-taking-panel">{result ? <><span className="mini-label">Submitted</span><h2>Results</h2><div className="quiz-score">{result.totalScore} points</div>{result.questions?.map((question) => { const answer = result.answers?.find((item) => item.questionId === question.id); return <div className="feedback-item" key={question.id}><strong>{question.question}</strong><span>Your answer: {answer?.answer || 'No answer'}</span><span>Correct answer: {question.correctAnswer}</span>{question.explanation && <small>{question.explanation}</small>}</div> })}</> : active ? <><div className="quiz-taking-header"><div><span className="mini-label">{active.course}</span><h2>{active.title}</h2></div>{attempt && <strong className={seconds !== null && seconds < 60 ? 'timer-warning' : ''}>{remaining}</strong>}</div>{!attempt ? <><p>{active.questions.length} questions. Attempts are enforced by the server.</p>{active.dueDate && new Date(active.dueDate) < new Date() ? <p className="quiz-error">This quiz is overdue and can no longer be started.</p> : <button className="primary-soft-button" type="button" onClick={start}>Start quiz</button>}</> : <>{active.questions.map((question, index) => <fieldset className="student-question" key={question.id}><legend>{index + 1}. {question.question}</legend>{question.questionType === 'SHORT_ANSWER' ? <textarea value={answers[question.id] || ''} onChange={(event) => setAnswers({ ...answers, [question.id]: event.target.value })} /> : question.questionType === 'TRUE_FALSE' ? <select value={answers[question.id] || ''} onChange={(event) => setAnswers({ ...answers, [question.id]: event.target.value })}><option value="">Choose answer</option><option value="true">True</option><option value="false">False</option></select> : question.options.map((option) => <label key={option.id}><input type="radio" name={`question-${question.id}`} checked={answers[question.id] === option.optionText} onChange={() => setAnswers({ ...answers, [question.id]: option.optionText })} />{option.optionText}</label>)}</fieldset>)}<button className="primary-soft-button" type="button" onClick={submit} disabled={seconds === 0}>Submit quiz</button></>}</> : <p className="quiz-muted">Choose a quiz to see its questions.</p>}</div></section></DashboardLayoutPage>
}

export default function QuizPage() {
  const user = getStoredUser()
  if (!user) return <Navigate to="/" replace />
  return user.userType === 'teacher' ? <TutorQuizHub user={user} /> : <StudentQuizHub user={user} />
}
