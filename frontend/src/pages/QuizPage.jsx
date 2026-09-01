import DashboardLayoutPage from '../components/DashboardLayoutPage'

export default function QuizPage() {
  return (
    <DashboardLayoutPage title="Quiz hub" buttonLabel="Create quiz">
      <div className="quiz-grid">
        <article className="panel glass quiz-panel">
          <h2>Recommended challenge</h2>
          <p>Adaptive math sprint</p>
          <strong>12 questions • 18 minutes</strong>
          <button type="button" className="premium-btn quiz-btn">Start now</button>
        </article>

        <article className="panel glass">
          <h2>Recent results</h2>
          <ul className="mini-list">
            <li><span>Algebra test</span><strong>93%</strong></li>
            <li><span>Physics recap</span><strong>88%</strong></li>
            <li><span>Programming quiz</span><strong>96%</strong></li>
          </ul>
        </article>
      </div>
    </DashboardLayoutPage>
  )
}
