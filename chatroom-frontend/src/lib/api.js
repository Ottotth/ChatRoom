export async function request(path, options = {}) {
  const response = await fetch(path, {
    credentials: 'include',
    ...options,
    headers: {
      ...(options.headers || {}),
    },
  })

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `Request failed: ${response.status}`)
  }

  const contentType = response.headers.get('content-type') || ''
  if (!contentType.includes('application/json')) {
    return null
  }
  return response.json()
}

export function login(username, password) {
  return request('/user/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
}

export function registerUser(username, displayName, password) {
  return request('/user/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, displayName, password }),
  })
}

export function logout() {
  return request('/user/logout', { method: 'POST' })
}

export function getMe() {
  return request('/user/me')
}

export function createRoom(name) {
  return request('/rooms', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
}

export function listRooms() {
  return request('/rooms')
}

export function getRoomInfo(roomId) {
  return request(`/rooms/${roomId}`)
}

export function joinRoom(roomId) {
  return request(`/rooms/${roomId}/members/join`, { method: 'POST' })
}

export function leaveRoom(roomId) {
  return request(`/rooms/${roomId}/members/leave`, { method: 'DELETE' })
}

export function notifyLeaveOnClose(roomId) {
  const url = `/rooms/${roomId}/members/disconnect`

  if (typeof navigator !== 'undefined' && typeof navigator.sendBeacon === 'function') {
    navigator.sendBeacon(url, '')
    return
  }

  fetch(url, {
    method: 'POST',
    credentials: 'include',
    keepalive: true,
  }).catch(() => {
    // Best-effort request during page close; ignore network errors.
  })
}

export function getMessageHistory(roomId) {
  return request(`/messages/rooms/${roomId}`)
}

export async function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  
  const response = await fetch('/user/avatar', {
    method: 'POST',
    credentials: 'include',
    body: formData,
  })

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `Upload failed: ${response.status}`)
  }

  const data = await response.json()
  return data.avatarUrl
}
