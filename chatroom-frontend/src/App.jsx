import { Navigate, Route, Routes } from 'react-router-dom'
import ChatPage from './pages/ChatPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import './App.css'

function App() {
  return (
    <main className="app-shell">
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/rooms" element={<Navigate to="/chat/1" replace />} />
        <Route path="/chat/:roomId" element={<ChatPage />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </main>
  )
}

export default App
