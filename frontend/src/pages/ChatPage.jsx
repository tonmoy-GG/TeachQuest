import { useEffect, useRef, useState } from 'react'
import { NavLink, Navigate, useNavigate } from 'react-router-dom'
import { getChatMessages, getStoredUser, getUniqueRegisteredUsers, navItems, saveChatMessages, syncRegisteredUsers } from '../utils/appData'

const accentPalette = ['purple', 'cyan', 'green', 'amber', 'rose', 'slate']

export default function ChatPage() {
  const navigate = useNavigate()
  const [user] = useState(() => getStoredUser())
  const [selectedChat, setSelectedChat] = useState(0)
  const [draft, setDraft] = useState('')
  const [pendingAttachment, setPendingAttachment] = useState(null)
  const attachmentInputRef = useRef(null)
  const imageInputRef = useRef(null)
  const [messagesByChat, setMessagesByChat] = useState(() => getChatMessages())
  const [registeredUsers, setRegisteredUsers] = useState(() => getUniqueRegisteredUsers())

  useEffect(() => {
    syncRegisteredUsers().then(setRegisteredUsers)
  }, [])

  useEffect(() => {
    const currentUser = getStoredUser()
    if (!currentUser) {
      navigate('/')
      return
    }
  }, [navigate])

  useEffect(() => {
    const handleStorage = (event) => {
      if (event.key === 'teachquest_chat_messages') {
        setMessagesByChat(getChatMessages())
      }
    }

    window.addEventListener('storage', handleStorage)
    return () => window.removeEventListener('storage', handleStorage)
  }, [])

  if (!user) {
    return <Navigate to="/" replace />
  }

  const displayName = user.username || user.email?.split('@')[0] || 'Alex'
  const currentUserEmail = (user.email || '').trim().toLowerCase()
  const conversations = registeredUsers
    .filter((member) => {
      const memberEmail = (member.email || '').trim().toLowerCase()

      return Boolean(memberEmail) && memberEmail !== currentUserEmail
    })
    .slice(0, 8)
    .map((member, index) => ({
      id: member.email || `contact-${index}`,
      name: member.username || 'Unknown User',
      role: member.userType === 'teacher' ? 'Teacher' : 'Student',
      time: index % 4 === 0 ? '2m ago' : index % 4 === 1 ? '18m ago' : index % 4 === 2 ? '1h ago' : 'Yesterday',
      unread: index % 3 === 0 ? 2 : 0,
      online: index % 2 === 0,
      accent: accentPalette[index % accentPalette.length],
      email: member.email || '',
    }))

  const buildRoomKey = (emailA, emailB) => [String(emailA || '').trim().toLowerCase(), String(emailB || '').trim().toLowerCase()].sort().join('|')
  const activeChat = conversations[Math.min(selectedChat, Math.max(conversations.length - 1, 0))] || null
  const activeRoomKey = activeChat ? buildRoomKey(currentUserEmail, activeChat.email) : ''

  const threadMessages = activeChat
    ? (messagesByChat[activeRoomKey] || [
        { side: 'incoming', text: `Hi ${displayName.split(' ')[0]}! I’m available for your ${activeChat.role === 'Teacher' ? 'tutoring' : 'study'} session this week.`, time: '9:42 AM' },
        { side: 'outgoing', text: `Perfect! I’ll review the materials and send you the topic list before our session.`, time: '9:44 AM' },
        { side: 'incoming', text: `Great. I can also share notes and the expected schedule if you want.`, time: '9:46 AM' },
      ])
    : []

  const handleSendMessage = () => {
    if (!activeChat || (!draft.trim() && !pendingAttachment)) return

    const nextMessage = {
      senderEmail: currentUserEmail,
      text: draft.trim(),
      time: new Date().toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' }),
      attachment: pendingAttachment,
    }

    const nextMap = {
      ...messagesByChat,
      [activeRoomKey]: [...(messagesByChat[activeRoomKey] || []), nextMessage],
    }

    saveChatMessages(nextMap)
    setMessagesByChat(nextMap)
    setDraft('')
    setPendingAttachment(null)
  }

  const handleFileSelected = (event) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    const reader = new FileReader()
    reader.onload = () => {
      setPendingAttachment({ name: file.name, type: file.type || 'application/octet-stream', size: file.size, dataUrl: reader.result })
    }
    reader.readAsDataURL(file)
  }

  const sharedMedia = [
    { label: 'Course notes', type: 'PDF' },
    { label: 'Session card', type: 'DOC' },
    { label: 'Reference', type: 'PDF' },
    { label: 'Quiz recap', type: 'IMG' },
  ]

  return (
    <div className="student-dashboard-shell">
      <header className="dashboard-topbar glass-panel">
        <div className="brand-block">
          <div className="brand-mark">TQ</div>
          <h1>TeachQuest</h1>
        </div>

        <nav className="main-nav" aria-label="Main navigation">
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="dashboard-search-box">
          <span className="material-symbols-outlined search-icon">search</span>
          <input type="text" placeholder="Search tutors, courses, or resources..." />
        </div>

        <div className="dashboard-top-actions">
          <button type="button" className="icon-button" aria-label="Notifications">
            <span className="material-symbols-outlined">notifications</span>
          </button>
          <button type="button" className="icon-button" aria-label="Settings">
            <span className="material-symbols-outlined">settings</span>
          </button>

          <div className="topbar-divider" />

          <button type="button" className="user-profile-button">
            <img alt="Student avatar" src="https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=200&q=80" />
            <div className="user-meta">
              <span>{displayName}</span>
              <small>Computer Science</small>
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

      <main className="dashboard-canvas chat-shell">
        <div className="chat-workspace glass-panel">
          <aside className="chat-sidebar">
            <div className="chat-sidebar-header">
              <div>
                <span className="mini-label">Messages</span>
                <h2>Chats</h2>
              </div>
              <button type="button" className="chat-new-btn">New chat</button>
            </div>

            <div className="chat-search-box">
              <span className="material-symbols-outlined">search</span>
              <input type="text" placeholder="Search conversations" />
            </div>

            <div className="conversation-list">
              {conversations.length === 0 ? (
                <div className="empty-chat-state">No other registered users yet.</div>
              ) : (
                conversations.map((chat, index) => (
                  <button type="button" key={chat.id} className={`conversation-item ${index === selectedChat ? 'active' : ''}`} onClick={() => setSelectedChat(index)}>
                    <div className={`avatar avatar-${chat.accent}`}>
                      {chat.name.split(' ').map((part) => part[0]).slice(0, 2).join('') || 'U'}
                      {chat.online && <span className="presence-dot" />}
                    </div>

                    <div className="conversation-copy">
                      <div className="conversation-topline">
                        <strong>{chat.name}</strong>
                        <span>{chat.time}</span>
                      </div>
                      <p>{chat.role}</p>
                    </div>

                    {chat.unread > 0 && <span className="unread-badge">{chat.unread}</span>}
                  </button>
                ))
              )}
            </div>
          </aside>

          <section className="chat-main-panel">
            {activeChat ? (
              <>
                <div className="chat-header">
                  <div className="chat-contact">
                    <div className={`avatar avatar-${activeChat.accent} large`}>
                      {activeChat.name.split(' ').map((part) => part[0]).slice(0, 2).join('') || 'U'}
                    </div>
                    <div>
                      <h3>{activeChat.name}</h3>
                      <span>{activeChat.online ? 'Online now' : 'Offline'}</span>
                    </div>
                  </div>

                  <div className="chat-actions">
                    <button type="button" className="chat-icon-btn" aria-label="Voice call">
                      <span className="material-symbols-outlined">call</span>
                    </button>
                    <button type="button" className="chat-icon-btn" aria-label="Video call">
                      <span className="material-symbols-outlined">videocam</span>
                    </button>
                    <button type="button" className="chat-icon-btn" aria-label="More options">
                      <span className="material-symbols-outlined">more_vert</span>
                    </button>
                  </div>
                </div>

                <div className="chat-thread">
                  {threadMessages.map((message, index) => {
                    const messageEmail = (message.senderEmail || '').trim().toLowerCase()
                    const side = messageEmail ? (messageEmail === currentUserEmail ? 'outgoing' : 'incoming') : message.side

                    return (
                    <div key={`${message.senderEmail || message.side}-${index}`} className={`message-row ${side}`}>
                      <div className="message-bubble">
                        {message.attachment && (
                          message.attachment.type.startsWith('image/') ? (
                            <img className="message-image" src={message.attachment.dataUrl} alt={message.attachment.name} />
                          ) : (
                            <a className="media-preview" href={message.attachment.dataUrl} download={message.attachment.name}>
                              <span className="material-symbols-outlined">description</span>
                              <span><strong>{message.attachment.name}</strong><small>{Math.ceil(message.attachment.size / 1024)} KB</small></span>
                            </a>
                          )
                        )}
                        {message.text && <div>{message.text}</div>}
                      </div>
                      <span className="message-time">{message.time}</span>
                    </div>
                    )
                  })}
                </div>

                <div className="media-strip">
                  <div className="media-strip-header">
                    <h4>Shared media</h4>
                    <button type="button">View all</button>
                  </div>
                  <div className="media-grid">
                    {sharedMedia.map((item) => (
                      <div key={item.label} className="media-item">
                        <div className="media-thumb">
                          <span className="material-symbols-outlined">image</span>
                        </div>
                        <div className="media-meta">
                          <strong>{item.label}</strong>
                          <small>{item.type}</small>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="composer">
                  <div className="composer-tools">
                    <input ref={attachmentInputRef} className="visually-hidden" type="file" onChange={handleFileSelected} />
                    <input ref={imageInputRef} className="visually-hidden" type="file" accept="image/*" onChange={handleFileSelected} />
                    <button type="button" className="composer-tool" aria-label="Attach file" onClick={() => attachmentInputRef.current?.click()}>
                      <span className="material-symbols-outlined">attach_file</span>
                    </button>
                    <button type="button" className="composer-tool" aria-label="Add image" onClick={() => imageInputRef.current?.click()}>
                      <span className="material-symbols-outlined">image</span>
                    </button>
                    <button type="button" className="composer-tool" aria-label="Emoji">
                      <span className="material-symbols-outlined">mood</span>
                    </button>
                  </div>

                  <div className="composer-input-wrap">
                    {pendingAttachment && <span className="attachment-chip">{pendingAttachment.name}</span>}
                    <input
                      type="text"
                      placeholder={`Message ${activeChat.name}...`}
                      value={draft}
                      onChange={(event) => setDraft(event.target.value)}
                      onKeyDown={(event) => {
                        if (event.key === 'Enter') {
                          event.preventDefault()
                          handleSendMessage()
                        }
                      }}
                    />
                  </div>

                  <button type="button" className="primary-soft-button send-button" onClick={handleSendMessage}>
                    <span>Send</span>
                    <span className="material-symbols-outlined">send</span>
                  </button>
                </div>
              </>
            ) : (
              <div className="empty-chat-panel">
                <h3>No users available</h3>
                <p>Register another account to start messaging.</p>
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  )
}
