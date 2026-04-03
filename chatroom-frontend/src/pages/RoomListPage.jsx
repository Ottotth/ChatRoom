import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createRoom, getMe, listRooms, logout } from '../lib/api'

function RoomListPage() {
  const navigate = useNavigate()
  const [me, setMe] = useState(null)
  const [createName, setCreateName] = useState('')
  const [status, setStatus] = useState('')
  const [rooms, setRooms] = useState([])

  const loadRooms = async () => {
    try {
      const data = await listRooms()
      setRooms(Array.isArray(data) ? data : [])
    } catch (error) {
      setStatus(`Failed to load rooms: ${error.message}`)
    }
  }

  useEffect(() => {
    const init = async () => {
      try {
        const user = await getMe()
        setMe(user)
        await loadRooms()
      } catch {
        navigate('/login')
      }
    }
    init()
  }, [navigate])

  const handleCreateRoom = async (event) => {
    event.preventDefault()
    if (!createName.trim()) return

    try {
      const result = await createRoom(createName.trim())
      setStatus(result?.message || 'Room created successfully')
      setCreateName('')
      await loadRooms()
    } catch (error) {
      setStatus(`Create failed: ${error.message}`)
    }
  }

  const enterChat = (roomId) => {
    navigate(`/chat/${roomId}`)
  }

  const handleLogout = async () => {
    try {
      await logout()
    } finally {
      navigate('/login')
    }
  }

  return (
    <section className="page-card wide">
      <header className="header-row">
        <div>
          <h1>Chat Room List</h1>
          <p className="sub">Hi, {me ? (me.displayName || me.username) : '...'} . Select a room to enter chat.</p>
        </div>
        <button className="ghost" onClick={handleLogout}>Logout</button>
      </header>

      <form className="form-col" onSubmit={handleCreateRoom}>
        <h2>Create Room</h2>
        <label>
          Room Name
          <input value={createName} onChange={(event) => setCreateName(event.target.value)} placeholder="General" />
        </label>
        <button>Create</button>
      </form>

      {status && <p className="sub status">{status}</p>}

      <div className="room-list">
        <div className="actions">
          <button className="ghost" onClick={loadRooms}>Refresh Room List</button>
        </div>
        {rooms.length === 0 && <p className="sub">No rooms yet. Create one first.</p>}
        {rooms.map((room) => (
          <article key={room.id} className="room-item">
            <div>
              <strong>{room.name}</strong>
              <p className="sub">ID: {room.id}</p>
              <p className="sub">Members: {(room.members || []).length}</p>
            </div>
            <div className="actions">
              <button onClick={() => enterChat(room.id)}>Enter Chat Room</button>
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}

export default RoomListPage
