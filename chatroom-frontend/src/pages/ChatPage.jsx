import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getMe, getMessageHistory, getRoomInfo, joinRoom, leaveRoom, notifyLeaveOnClose, uploadAvatar, updateDisplayName, logout } from '../lib/api'
import ouroboros from '../assets/ouroboros.png'
import noicon from '../assets/noicon.png'
import messageSound from '../assets/message_sound.mp3'

function ChatPage() {
  const { roomId } = useParams()
  const navigate = useNavigate()
  const clientRef = useRef(null)
  const hasLeftRef = useRef(false)
  const messagesEndRef = useRef(null)
  const messageSoundRef = useRef(null)
  const currentUsernameRef = useRef('')
  const isMutedRef = useRef(false)
  const avatarInputRef = useRef(null)

  const [me, setMe] = useState(null)
  const [roomTitle, setRoomTitle] = useState('-')
  const [members, setMembers] = useState([])
  const [messages, setMessages] = useState([])
  const [messageInput, setMessageInput] = useState('')
  const [connected, setConnected] = useState(false)
  const [logs, setLogs] = useState([])
  const [showAvatarUpload, setShowAvatarUpload] = useState(false)
  const [displayNameInput, setDisplayNameInput] = useState('')
  const [isUpdatingDisplayName, setIsUpdatingDisplayName] = useState(false)
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false)
  const [isMuted, setIsMuted] = useState(false)
  const [isNearBottom, setIsNearBottom] = useState(true)
  const userScrollTimeoutRef = useRef(null)

  const numericRoomId = Number(roomId)

  const pushLog = (text) => {
    const time = new Date().toLocaleTimeString('zh-TW', { hour12: false })
    setLogs((prev) => [`${time} ${text}`, ...prev].slice(0, 120))
  }

  const playIncomingMessageSound = () => {
    if (!messageSoundRef.current) return
    if (isMutedRef.current) return
    messageSoundRef.current.currentTime = 0
    messageSoundRef.current.play().catch((error) => {
      pushLog(`Message sound blocked: ${error?.message || 'play failed'}`)
    })
  }

  const toggleMute = () => {
    setIsMuted((prev) => !prev)
  }

  useEffect(() => {
    messageSoundRef.current = new Audio(messageSound)
    messageSoundRef.current.preload = 'auto'
    messageSoundRef.current.volume = 1

    const unlockAudio = () => {
      if (!messageSoundRef.current) return
      messageSoundRef.current.muted = true
      messageSoundRef.current.play()
        .then(() => {
          if (!messageSoundRef.current) return
          messageSoundRef.current.pause()
          messageSoundRef.current.currentTime = 0
          messageSoundRef.current.muted = false
        })
        .catch(() => {
          // Ignore unlock failures until next user interaction.
        })
    }

    window.addEventListener('pointerdown', unlockAudio, { once: true })
    window.addEventListener('keydown', unlockAudio, { once: true })

    return () => {
      window.removeEventListener('pointerdown', unlockAudio)
      window.removeEventListener('keydown', unlockAudio)
      if (!messageSoundRef.current) return
      messageSoundRef.current.pause()
      messageSoundRef.current = null
    }
  }, [])

  useEffect(() => {
    isMutedRef.current = isMuted
    if (messageSoundRef.current) {
      messageSoundRef.current.muted = isMuted
    }
  }, [isMuted])

  const displayName = useMemo(() => {
    if (!me) return 'Guest'
    return me.displayName || me.username
  }, [me])

  // Handle scroll detection
  const handleMessagesScroll = (e) => {
    const element = e.target
    const isNear = element.scrollHeight - element.scrollTop - element.clientHeight < 100
    setIsNearBottom(isNear)

    // Clear existing timeout
    if (userScrollTimeoutRef.current) {
      clearTimeout(userScrollTimeoutRef.current)
    }

    // Disable auto-scroll for 2 seconds when user manually scrolls
    if (!isNear) {
      userScrollTimeoutRef.current = setTimeout(() => {
        // Re-enable auto-scroll after 2 seconds
        userScrollTimeoutRef.current = null
      }, 20)
    }
  }

  // Auto-scroll to latest message only if user is near bottom or they sent the message
  useEffect(() => {
    const lastMessage = messages[messages.length - 1]
    const isCurrentUserMessage = lastMessage && me && lastMessage.sender === me.username

    // Only auto-scroll if not in manual scroll cooldown
    if (userScrollTimeoutRef.current === null && (isNearBottom || isCurrentUserMessage)) {
      messagesEndRef.current?.scrollIntoView()
    }
  }, [messages, isNearBottom, me])

  const disconnect = async () => {
    if (!clientRef.current) {
      setConnected(false)
      return
    }
    await clientRef.current.deactivate()
    clientRef.current = null
    setConnected(false)
  }

  const connectWebSocket = (targetRoomId) => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 0,
      onConnect: () => {
        client.subscribe(`/topic/rooms/${targetRoomId}`, (frame) => {
          try {
            const payload = JSON.parse(frame.body)
            setMessages((prev) => [...prev, payload])

            const isIncomingChat = payload?.type !== 'SYSTEM'
              && payload?.sender
              && payload.sender !== currentUsernameRef.current
            if (isIncomingChat) {
              playIncomingMessageSound()
            }
          } catch {
            pushLog(`Failed to parse message: ${frame.body}`)
          }
        })

        client.subscribe(`/topic/rooms/${targetRoomId}/members`, async (frame) => {
          try {
            const memberEvent = JSON.parse(frame.body)
            if (memberEvent?.event === 'JOIN' && memberEvent?.username) {
              setMessages((prev) => [...prev, {
                type: 'SYSTEM',
                content: `---- ${memberEvent.username} in ----`,
                createdAt: new Date().toISOString(),
              }])
            }
            if (memberEvent?.event === 'LEAVE' && memberEvent?.username) {
              setMessages((prev) => [...prev, {
                type: 'SYSTEM',
                content: `---- ${memberEvent.username} out ----`,
                createdAt: new Date().toISOString(),
              }])
            }
          } catch {
            pushLog(`Failed to parse member event: ${frame.body}`)
          }

          try {
            const latest = await getRoomInfo(targetRoomId)
            setMembers(latest?.members || [])
          } catch (error) {
            pushLog(`Failed to update members: ${error.message}`)
          }
        })

        setConnected(true)
        pushLog(`Connected to room ${targetRoomId}`)
      },
      onStompError: (frame) => {
        pushLog(`STOMP error: ${frame.headers.message || 'unknown error'}`)
      },
      onWebSocketClose: () => setConnected(false),
      onWebSocketError: () => {
        setConnected(false)
        pushLog('WebSocket connection failed')
      },
    })

    clientRef.current = client
    client.activate()
  }

  const handleReconnect = async () => {
    if (connected || !Number.isInteger(numericRoomId) || numericRoomId <= 0) return
    try {
      await disconnect()
      await joinRoom(numericRoomId)
      const latest = await getRoomInfo(numericRoomId)
      setMembers(latest?.members || [])
      connectWebSocket(numericRoomId)
      pushLog('Reconnecting...')
    } catch (error) {
      pushLog(`Reconnect failed: ${error.message}`)
    }
  }

  useEffect(() => {
    if (!Number.isInteger(numericRoomId) || numericRoomId <= 0) {
      navigate('/rooms')
      return
    }

    let cancelled = false

    const init = async () => {
      try {
        const user = await getMe()
        console.log('getMe result:', user)
        if (cancelled) return
        setMe(user)
        currentUsernameRef.current = user?.username || ''
      } catch {
        navigate('/login')
        return
      }

      try {
        await joinRoom(numericRoomId)
        if (cancelled) return

        const [room, history] = await Promise.all([
          getRoomInfo(numericRoomId),
          getMessageHistory(numericRoomId),
        ])

        if (cancelled) return
        setRoomTitle(room?.name || `Room ${numericRoomId}`)
        setMembers(room?.members || [])
        setMessages(Array.isArray(history) ? history : [])

        connectWebSocket(numericRoomId)
      } catch (error) {
        pushLog(`Failed to join room: ${error.message}`)
      }
    }

    init()

    const handlePageExit = () => {
      if (hasLeftRef.current) return
      hasLeftRef.current = true
      notifyLeaveOnClose(numericRoomId)
    }

    window.addEventListener('pagehide', handlePageExit)
    window.addEventListener('beforeunload', handlePageExit)

    return () => {
      cancelled = true
      window.removeEventListener('pagehide', handlePageExit)
      window.removeEventListener('beforeunload', handlePageExit)
      disconnect()
      // Clean up scroll timeout
      if (userScrollTimeoutRef.current) {
        clearTimeout(userScrollTimeoutRef.current)
      }
    }
  }, [navigate, numericRoomId])

  const handleLeave = async () => {
    hasLeftRef.current = true
    try {
      await leaveRoom(numericRoomId)
    } catch {
      // Ignore leave errors and still navigate away.
    }
    await disconnect()
    navigate('/rooms')
  }

  const handleLogout = async () => {
    hasLeftRef.current = true
    try {
      await leaveRoom(numericRoomId)
    } catch {
      // Ignore leave errors and continue logout.
    }
    try {
      await logout()
    } catch {
      // Ignore logout errors and still navigate away.
    }
    await disconnect()
    navigate('/login')
  }

  const handleSendMessage = () => {
    if (!clientRef.current || !connected || !messageInput.trim()) return

    const payload = {
      roomId: numericRoomId,
      content: messageInput.trim(),
      type: 'CHAT',
      createdAt: new Date().toISOString(),
    }

    clientRef.current.publish({
      destination: '/app/chat.send',
      body: JSON.stringify(payload),
    })
    setMessageInput('')
  }

  const handleUpdateDisplayName = async () => {
    const trimmed = displayNameInput.trim()
    if (!trimmed) return
    setIsUpdatingDisplayName(true)
    try {
      await updateDisplayName(trimmed)
      setMe((prev) => ({ ...prev, displayName: trimmed }))
      setDisplayNameInput('')
    } catch (error) {
      alert(`Failed to update display name: ${error.message}`)
    } finally {
      setIsUpdatingDisplayName(false)
    }
  }

  const handleAvatarFileChange = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    const maxAvatarBytes = 10 * 1024 * 1024
    if (file.size > maxAvatarBytes) {
      alert('Image is too large. Please choose a file smaller than 10MB.')
      e.target.value = ''
      return
    }
    e.target.value = ''
    setIsUploadingAvatar(true)
    try {
      const result = await uploadAvatar(file)
      setMe((prev) => ({ ...prev, avatarUrl: result }))
    } catch (error) {
      const rawMessage = error?.message || ''
      const isPayloadTooLarge = rawMessage.includes('413')
      alert(isPayloadTooLarge
        ? 'Image is too large for upload. Please choose a smaller file (under 10MB).'
        : `Upload failed: ${rawMessage}`)
    } finally {
      setIsUploadingAvatar(false)
    }
  }

  return (
    <div className="chat-page">
      <header className="chat-header">
        <div className="chat-header-logo">
          <img src={ouroboros} alt="ouroboros logo" className="header-logo-img" />
        </div>
        <div className="actions">
          <p className="header-user-name">{displayName}</p>
          <button
            className="avatar-btn"
            onClick={() => setShowAvatarUpload(true)}
            title="Upload avatar"
          >
            <img src={me?.avatarUrl || noicon} alt="user avatar" />
          </button>
          <div className="conn-status">
            <button
              type="button"
              className={`conn-dot ${connected ? 'ok' : 'off'}`}
              title={connected ? 'Connected' : 'Disconnected - click to reconnect'}
              onClick={handleReconnect}
              aria-label={connected ? 'Connected' : 'Disconnected, click to reconnect'}
            />
            <button
              className="mute-btn"
              onClick={toggleMute}
              title={isMuted ? 'Unmute notifications' : 'Mute notifications'}
              aria-label={isMuted ? 'Unmute notifications' : 'Mute notifications'}
            >
              <svg className="mute-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M4 10V14H8L13 18V6L8 10H4Z" />
                {isMuted ? (
                  <line x1="4" y1="4" x2="20" y2="20" />
                ) : (
                  <>
                    <path d="M16 9C17.5 10.5 17.5 13.5 16 15" />
                    <path d="M19 7C21.5 9.5 21.5 14.5 19 17" />
                  </>
                )}
              </svg>
            </button>
            <button className="logout-btn" onClick={handleLogout} title="Logout">Logout</button>
          </div>
        </div>
      </header>

      <div className="chat-body">
        <section className="chat-main">
          <div className="messages" onScroll={handleMessagesScroll}>
            {messages.map((message, index) => {
              const isSystem = message?.type === 'SYSTEM'
              if (isSystem) {
                return (
                  <div className="message-system-row" key={`${message.createdAt || index}-${index}`}>
                    <p className="message-system-text">{message.content}</p>
                  </div>
                )
              }

              const mine = me && message.sender === me.username
              const avatarUrl = message.senderAvatarUrl || (mine ? me?.avatarUrl : '') || noicon
              const senderLabel = mine ? 'Me' : (message.senderDisplayName || message.sender || 'unknown')
              const isLongSenderLabel = senderLabel.length > 6
              const messageTime = new Date(message.createdAt || Date.now()).toLocaleTimeString('zh-TW', { hour12: false })

              return (
                <div className={`message-row ${mine ? 'mine' : ''}`} key={`${message.createdAt || index}-${index}`}>
                  <div className={`message-bubble-stack ${mine ? 'mine' : ''}`}>
                    <div className={`bubble ${mine ? 'mine' : ''}`}>
                      <p>{message.content}</p>
                    </div>
                    <p className={`message-time ${mine ? 'mine' : ''}`}>{messageTime}</p>
                  </div>

                  <div className={`message-side ${mine ? 'mine' : ''}`}>
                    <p className={`message-name ${isLongSenderLabel ? 'long' : ''}`}>{senderLabel}</p>
                    <img src={avatarUrl} alt="avatar" className="bubble-avatar" />
                  </div>
                </div>
              )
            })}
            <div ref={messagesEndRef} />
          </div>
          <div className="chat-input-row">
            <textarea
              value={messageInput}
              onChange={(event) => setMessageInput(event.target.value)}
              placeholder="Type a message..."
              rows={2}
              onKeyDown={(event) => {
                if (event.key === 'Enter' && !event.shiftKey) {
                  event.preventDefault()
                  handleSendMessage()
                }
              }}
            />
            <button onClick={handleSendMessage}>SEND</button>
          </div>
        </section>

        <aside className="chat-sidebar">
          <div className="chat-info-panel">

            <p className="sub">
              Under the Rose.<br />
              -- <br />
              Sei in dieser Nacht aus Übermaß<br />
              Zauberkraft am Kreuzweg deiner Sinne,<br />
              ihrer seltsamen Begegnung Sinn.<br />
              <span className="info-stanza-gap" />
              Und wenn dich das Irdische vergaß,<br />
              zu der stillen Erde sag: Ich rinne.<br />
              Zu dem raschen Wasser sprich: Ich bin.<br />

            </p>

          </div>

          <div className="chat-members-panel">
            <h2>Members ({members.length})</h2>
            <ul className="members">
              {members.map((member) => (
                <li key={`${member.username}-${member.joinedAt || ''}`}>
                  <img src={member.avatarUrl || noicon} alt={member.displayName} className="member-avatar" />
                  <div className="member-info">
                    <strong>{member.username === me?.username ? 'Me' : member.displayName}</strong>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </aside>
      </div>

      {showAvatarUpload && (
        <div className="modal-overlay" onClick={() => setShowAvatarUpload(false)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
            <button className="modal-close-btn" onClick={() => { setShowAvatarUpload(false); setDisplayNameInput('') }}>✕</button>
            <h2>Profile Settings</h2>

            <div className="profile-avatar-preview">
              <img src={me?.avatarUrl || noicon} alt="current avatar" />
              <p className="profile-current-name">{me?.displayName || me?.username}</p>
              <p className="profile-current-username">@{me?.username || 'guest'}</p>
            </div>

            <div className="profile-section">
              <h3>Change Avatar</h3>
              <input
                ref={avatarInputRef}
                type="file"
                accept="image/*"
                style={{ display: 'none' }}
                onChange={handleAvatarFileChange}
              />
              <button
                className="profile-save-btn"
                onClick={() => avatarInputRef.current?.click()}
                disabled={isUploadingAvatar}
              >
                {isUploadingAvatar ? 'Uploading...' : 'Choose & Upload Image'}
              </button>
            </div>

            <div className="profile-section">
              <h3>Change Display Name</h3>
              <input
                type="text"
                className="profile-text-input"
                placeholder={me?.displayName || me?.username}
                value={displayNameInput}
                onChange={(e) => setDisplayNameInput(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') handleUpdateDisplayName() }}
                maxLength={32}
              />
              <button
                className="profile-save-btn"
                onClick={handleUpdateDisplayName}
                disabled={isUpdatingDisplayName || !displayNameInput.trim()}
              >
                {isUpdatingDisplayName ? 'Saving...' : 'Save'}
              </button>
            </div>


          </div>
        </div>
      )}
    </div>
  )
}

export default ChatPage
