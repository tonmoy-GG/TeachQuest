import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import DashboardLayoutPage from '../components/DashboardLayoutPage'
import { getStoredUser } from '../utils/appData'
import './CertificatesPage.css'

async function loadCertificates(userId) {
  const response = await fetch(`/api/certificates?userId=${userId}`)
  const data = await response.json()
  if (!response.ok) throw new Error(typeof data === 'string' ? data : 'Could not load certificates.')
  return data
}

async function downloadCertificate(certificateId, format, userId) {
  const response = await fetch(`/api/certificates/${certificateId}/${format}?userId=${userId}`)
  if (!response.ok) throw new Error(await response.text() || 'Download failed.')
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `teachquest-${certificateId}.${format === 'pdf' ? 'pdf' : 'png'}`
  link.click()
  URL.revokeObjectURL(url)
}

function formatDate(value) {
  return value ? new Date(value).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' }) : 'Date unavailable'
}

export default function CertificatesPage() {
  const user = getStoredUser()
  const [certificates, setCertificates] = useState([])
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState('')

  useEffect(() => {
    if (!user?.id) return undefined
    let active = true
    loadCertificates(user.id)
      .then((items) => { if (active) setCertificates(items) })
      .catch((loadError) => { if (active) setError(loadError.message) })
    return () => { active = false }
  }, [user?.id])

  if (!user) return <Navigate to="/" replace />

  const download = async (certificate, format) => {
    setBusyId(certificate.certificateId)
    setError('')
    try { await downloadCertificate(certificate.certificateId, format, user.id) }
    catch (downloadError) { setError(downloadError.message) }
    finally { setBusyId('') }
  }

  const copyVerificationLink = async (url) => {
    try { await navigator.clipboard.writeText(url) }
    catch { setError('Your browser could not copy the verification link.') }
  }

  return <DashboardLayoutPage title="Certificates">
    <div className="certificate-page">
      {error && <div className="certificate-alert" role="alert">{error}<button type="button" onClick={() => setError('')} aria-label="Dismiss error">×</button></div>}
      <section className="certificate-overview">
        <div><span className="certificate-kicker">Trophy case</span><h2>Proof of progress.</h2></div>
        <p>Course awards and their verification records, including previous editions.</p>
        <div className="certificate-count"><strong>{certificates.filter((item) => item.status === 'CURRENT').length}</strong><span>current awards</span></div>
      </section>
      {certificates.length === 0 ? <section className="certificate-empty"><span className="material-symbols-outlined">workspace_premium</span><h2>No certificates yet</h2><p>Pass the first four levels of a course to earn your first award.</p></section> : <section className="certificate-list" aria-label="Earned certificates">
        {certificates.map((certificate) => <article className={`certificate-item tier-${certificate.tier.toLowerCase()} ${certificate.status === 'SUPERSEDED' ? 'is-archived' : ''}`} key={certificate.certificateId}>
          <div className="certificate-medal" aria-hidden="true"><span className="material-symbols-outlined">workspace_premium</span></div>
          <div className="certificate-main">
            <div className="certificate-title-row"><div><span className="certificate-tier">{certificate.tier}</span><h2>{certificate.courseName}</h2></div><span className={`certificate-status ${certificate.status === 'CURRENT' ? 'current' : ''}`}>{certificate.status === 'CURRENT' ? 'Current' : 'Archived edition'}</span></div>
            <p className="certificate-meta">Issued {formatDate(certificate.issuedAt)} <span>·</span> Average {Number(certificate.averageScore).toFixed(2)}%</p>
            <div className="certificate-breakdown">{Object.entries(certificate.scoreBreakdown || {}).map(([level, score]) => <div key={level}><span>Level {level}</span><strong>{Number(score).toFixed(1)}%</strong></div>)}</div>
            <div className="certificate-actions">
              <button type="button" onClick={() => void download(certificate, 'pdf')} disabled={busyId === certificate.certificateId}><span className="material-symbols-outlined">picture_as_pdf</span>Download PDF</button>
              <button type="button" onClick={() => void download(certificate, 'social-card')} disabled={busyId === certificate.certificateId}><span className="material-symbols-outlined">image</span>Social card</button>
              <a href={certificate.verificationUrl} target="_blank" rel="noreferrer"><span className="material-symbols-outlined">verified</span>Verify</a>
              <button type="button" className="certificate-copy" onClick={() => void copyVerificationLink(certificate.verificationUrl)} aria-label="Copy verification link" title="Copy verification link"><span className="material-symbols-outlined">link</span></button>
            </div>
          </div>
        </article>)}
      </section>}
    </div>
  </DashboardLayoutPage>
}