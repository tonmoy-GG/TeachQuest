import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import './CertificatesPage.css'

export default function CertificateVerificationPage() {
  const { certificateId } = useParams()
  const [verification, setVerification] = useState(null)
  const [notFound, setNotFound] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    fetch(`/api/certificates/verify/${encodeURIComponent(certificateId)}`)
      .then(async (response) => {
        if (!response.ok) throw new Error('not-found')
        return response.json()
      })
      .then((data) => { if (active) setVerification(data) })
      .catch(() => { if (active) setNotFound(true) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [certificateId])

  return <main className="verification-page">
    <header className="verification-brand"><span className="verification-mark">TQ</span><span>TeachQuest</span></header>
    {loading ? <section className="verification-panel"><p>Checking certificate…</p></section> : notFound ? <section className="verification-panel verification-invalid"><span className="material-symbols-outlined">cancel</span><h1>Not found</h1><p>This certificate ID is not valid.</p></section> : <section className="verification-panel verification-valid">
      <span className="material-symbols-outlined">verified</span>
      <p className="certificate-kicker">Certificate verified</p>
      <h1>Valid certificate issued to {verification.name} for {verification.course}, tier {verification.tier}, on {new Date(verification.issuedAt).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })}.</h1>
      {verification.superseded && <p className="verification-history-note">This edition has since been superseded by a newer certificate.</p>}
    </section>}
    <footer><Link to="/">TeachQuest learning</Link></footer>
  </main>
}