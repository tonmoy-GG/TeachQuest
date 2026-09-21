import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { syncLocalHiresToBackend, syncRegisteredUsers } from '../utils/appData'

function formatTime(value) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })
}

async function requestJson(url, options) {
  const response = await fetch(url, options)
  const data = await response.json().catch(() => null)
  if (!response.ok) throw new Error(typeof data === 'string' ? data : data?.message || 'Group chat request failed.')
  return data
}

export default function GroupChatPanel({ user, isTeacher }) {
  const [groups, setGroups] = useState([])
  const [eligibleStudents, setEligibleStudents] = useState([])
  const [selectedGroupId, setSelectedGroupId] = useState(null)
  const [messages, setMessages] = useState([])
  const [draft, setDraft] = useState('')
  const [groupName, setGroupName] = useState('')
  const [selectedStudentIds, setSelectedStudentIds] = useState([])
  const [pendingAttachment, setPendingAttachment] = useState(null)
  const [error, setError] = useState('')
  const [creating, setCreating] = useState(false)
  const fileInputRef = useRef(null)
  const userId = user?.id

  const selectedGroup = groups.find((group) => group.id === selectedGroupId) || null
  const memberIds = useMemo(() => new Set((selectedGroup?.members || []).map((member) => member.id)), [selectedGroup])

  const loadGroups = useCallback(async (selectFirst = false) => {
    if (!userId) return
    try {
      const nextGroups = await requestJson(`/api/chat/groups?userId=${userId}`)
      setGroups(nextGroups)
      setSelectedGroupId((current) => {
        if (current && nextGroups.some((group) => group.id === current)) return current
        return selectFirst || nextGroups.length ? nextGroups[0]?.id || null : null
      })
      setError('')
    } catch (err) {
      setError(err.message)
    }
  }, [userId])

  const loadMessages = useCallback(async (groupId = selectedGroupId) => {
    if (!userId || !groupId) return
    try {
      const nextMessages = await requestJson(`/api/chat/groups/${groupId}/messages?userId=${userId}`)
      setMessages(nextMessages)
    } catch (err) {
      setError(err.message)
    }
  }, [selectedGroupId, userId])

  useEffect(() => {
    const initialLoad = window.setTimeout(async () => {
      try {
        if (isTeacher) {
          await syncRegisteredUsers()
          await syncLocalHiresToBackend()
        }
        await loadGroups(true)
        if (isTeacher) {
          const students = await requestJson(`/api/chat/groups/eligible-students?tutorId=${userId}`)
          setEligibleStudents(students)
        }
      } catch (err) {
        setError(err.message)
      }
    }, 0)
    return () => window.clearTimeout(initialLoad)
  }, [isTeacher, loadGroups, userId])

  useEffect(() => {
    const initialLoad = window.setTimeout(() => void loadMessages(), 0)
    if (!selectedGroupId) return () => window.clearTimeout(initialLoad)
    const interval = window.setInterval(() => void loadMessages(), 4000)
    return () => {
      window.clearTimeout(initialLoad)
      window.clearInterval(interval)
    }
  }, [loadMessages, selectedGroupId])

  const createGroup = async (event) => {
    event.preventDefault()
    if (!groupName.trim() || !userId) return
    setCreating(true)
    try {
      const group = await requestJson('/api/chat/groups', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ownerId: userId, name: groupName.trim(), studentIds: selectedStudentIds }),
      })
      setGroups((current) => [group, ...current])
      setSelectedGroupId(group.id)
      setGroupName('')
      setSelectedStudentIds([])
      setError('')
    } catch (err) {
      setError(err.message)
    } finally {
      setCreating(false)
    }
  }

  const addStudent = async (studentId) => {
    try {
      const group = await requestJson(`/api/chat/groups/${selectedGroup.id}/members`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ requesterId: userId, userId: studentId }),
      })
      setGroups((current) => current.map((item) => item.id === group.id ? group : item))
      setError('')
    } catch (err) {
      setError(err.message)
    }
  }

  const removeStudent = async (studentId) => {
    try {
      const group = await requestJson(`/api/chat/groups/${selectedGroup.id}/members/${studentId}?requesterId=${userId}`, { method: 'DELETE' })
      setGroups((current) => current.map((item) => item.id === group.id ? group : item))
      setError('')
    } catch (err) {
      setError(err.message)
    }
  }

  const leaveGroup = async () => {
    if (!selectedGroup) return
    try {
      await requestJson(`/api/chat/groups/${selectedGroup.id}/leave?userId=${userId}`, { method: 'POST' })
      await loadGroups()
    } catch (err) {
      setError(err.message)
    }
  }

  const sendMessage = async (event) => {
    event?.preventDefault()
    if (!selectedGroup || (!draft.trim() && !pendingAttachment)) return
    const formData = new FormData()
    formData.append('senderId', userId)
    formData.append('message', draft.trim())
    if (pendingAttachment) formData.append('file', pendingAttachment)
    try {
      await requestJson(`/api/chat/groups/${selectedGroup.id}/messages`, { method: 'POST', body: formData })
      setDraft('')
      setPendingAttachment(null)
      await loadMessages()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <section className="group-chat-panel glass-panel">
      <div className="group-chat-heading">
        <div>
          <span className="mini-label">Shared rooms</span>
          <h2>Study groups</h2>
        </div>
        <span className="group-chat-status">Updates every few seconds</span>
      </div>

      {error && <div className="group-chat-error" role="alert">{error}</div>}

      {isTeacher && (
        <form className="group-create-form" onSubmit={createGroup}>
          <input value={groupName} onChange={(event) => setGroupName(event.target.value)} placeholder="Group name" maxLength={150} aria-label="Group name" />
          <div className="group-student-picker">
            {eligibleStudents.length === 0 ? <span>No hired students available.</span> : eligibleStudents.map((student) => (
              <label key={student.id}>
                <input type="checkbox" checked={selectedStudentIds.includes(student.id)} onChange={() => setSelectedStudentIds((current) => current.includes(student.id) ? current.filter((id) => id !== student.id) : [...current, student.id])} />
                {student.name}
              </label>
            ))}
          </div>
          <button className="primary-soft-button" type="submit" disabled={creating || !groupName.trim()}>{creating ? 'Creating...' : 'Create group'}</button>
        </form>
      )}

      <div className="group-chat-layout">
        <aside className="group-list">
          {groups.length === 0 ? <p className="group-empty">{isTeacher ? 'Create a room for students you have taught.' : 'Your tutor has not created a group yet.'}</p> : groups.map((group) => (
            <button type="button" key={group.id} className={`group-list-item ${group.id === selectedGroupId ? 'active' : ''}`} onClick={() => setSelectedGroupId(group.id)}>
              <span className="group-avatar"><span className="material-symbols-outlined">groups</span></span>
              <span><strong>{group.name}</strong><small>{group.members.length} members</small></span>
            </button>
          ))}
        </aside>

        <div className="group-chat-main">
          {selectedGroup ? (
            <>
              <header className="group-chat-header">
                <div><h3>{selectedGroup.name}</h3><span>{selectedGroup.members.length} members</span></div>
                {!isTeacher && <button type="button" className="text-action" onClick={leaveGroup}>Leave group</button>}
              </header>
              <div className="group-member-row">
                {selectedGroup.members.map((member) => (
                  <span className="group-member-chip" key={member.id}>{member.name}{isTeacher && member.role !== 'OWNER' && <button type="button" aria-label={`Remove ${member.name}`} onClick={() => removeStudent(member.id)}>×</button>}</span>
                ))}
                {isTeacher && eligibleStudents.filter((student) => !memberIds.has(student.id)).map((student) => <button type="button" className="add-member-button" key={student.id} onClick={() => addStudent(student.id)}>+ {student.name}</button>)}
              </div>
              <div className="group-message-list">
                {messages.length === 0 ? <p className="group-empty">Start the conversation.</p> : messages.map((message) => {
                  const sender = selectedGroup.members.find((member) => member.id === message.senderId)
                  const own = message.senderId === userId
                  return <div className={`group-message ${own ? 'own' : ''}`} key={message.id}><strong>{own ? 'You' : sender?.name || 'Member'}</strong><div>{message.message && message.message !== '[File Attachment]' ? message.message : null}{message.filePath && <a href={`/${message.filePath.replace(/^\/+/, '')}`} target="_blank" rel="noreferrer">{message.fileName || 'Attachment'}</a>}</div><small>{formatTime(message.timestamp)}</small></div>
                })}
              </div>
              <form className="group-composer" onSubmit={sendMessage}>
                <input ref={fileInputRef} className="visually-hidden" type="file" onChange={(event) => setPendingAttachment(event.target.files?.[0] || null)} />
                <button type="button" className="composer-tool" aria-label="Attach file" onClick={() => fileInputRef.current?.click()}><span className="material-symbols-outlined">attach_file</span></button>
                <input value={draft} onChange={(event) => setDraft(event.target.value)} placeholder="Message the group..." />
                {pendingAttachment && <small>{pendingAttachment.name}</small>}
                <button className="primary-soft-button" type="submit">Send <span className="material-symbols-outlined">send</span></button>
              </form>
            </>
          ) : <p className="group-empty">Select a group to start chatting.</p>}
        </div>
      </div>
    </section>
  )
}
