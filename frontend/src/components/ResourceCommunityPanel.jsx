import { useEffect, useState } from 'react'

export default function ResourceCommunityPanel({ user }) {
  const [period, setPeriod] = useState('all-time')
  const [leaders, setLeaders] = useState([])
  const [summary, setSummary] = useState({ allTime: user?.totalPoints || 0, thisMonth: 0, badges: [] })

  useEffect(() => {
    fetch(`/api/resources/leaderboard?period=${period}`)
      .then((response) => response.ok ? response.json() : [])
      .then((data) => setLeaders(Array.isArray(data) ? data : []))
      .catch(() => setLeaders([]))
  }, [period])

  useEffect(() => {
    if (!user?.id) return
    fetch(`/api/users/${user.id}/resource-points`)
      .then((response) => response.ok ? response.json() : null)
      .then((data) => { if (data) setSummary(data) })
      .catch(() => {})
  }, [user?.id])

  return (
    <section className="resource-community-panel glass-panel">
      <div className="resource-community-summary">
        <div><span className="mini-label">Your contribution</span><strong>{summary.allTime || 0}</strong><small>all-time points</small></div>
        <div><strong>{summary.thisMonth || 0}</strong><small>this month</small></div>
        <div><strong>{summary.uploads || 0}</strong><small>resources shared</small></div>
      </div>
      <div className="resource-community-grid">
        <div>
          <div className="resource-panel-heading"><h3>Milestone badges</h3><span>Rule-based</span></div>
          <div className="resource-badges">
            {(summary.badges || []).map((badge) => <div className={`resource-badge ${badge.earned ? 'earned' : ''}`} key={badge.code}><span className="material-symbols-outlined">{badge.earned ? 'verified' : 'lock'}</span><div><strong>{badge.label}</strong><small>{badge.rule}</small></div></div>)}
          </div>
        </div>
        <div>
          <div className="resource-panel-heading"><h3>Contributor leaderboard</h3><div className="leaderboard-tabs"><button className={period === 'month' ? 'active' : ''} onClick={() => setPeriod('month')}>This Month</button><button className={period === 'all-time' ? 'active' : ''} onClick={() => setPeriod('all-time')}>All Time</button></div></div>
          <div className="leaderboard-list">{leaders.length ? leaders.slice(0, 5).map((entry) => <div className="leaderboard-row" key={entry.userId}><span className="leaderboard-rank">#{entry.rank}</span><strong>{entry.username}</strong><span>{entry.points} pts</span></div>) : <p className="leaderboard-empty">No contribution events yet.</p>}</div>
        </div>
      </div>
    </section>
  )
}
