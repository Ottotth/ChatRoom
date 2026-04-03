import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getMe, login } from '../lib/api'
import ouroborosImage from '../assets/ouroboros.png'

function LoginPage() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setMessage('')

    try {
      await login(username.trim(), password)
      await getMe()
      navigate('/chat/1')
    } catch (error) {
      setMessage(`Login failed: ${error.message}`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="page-card login-page-card">
      <img src={ouroborosImage} alt="Login artwork" className="login-art" />
      <form onSubmit={handleSubmit} className="form-col login-form">
        <input
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          placeholder="Username"
          required
        />
        <input
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          placeholder="Password"
          required
        />
        <button disabled={loading}>Login</button>
      </form>
      {message && <p className="error-text">{message}</p>}
      <p className="sub"><Link to="/register">Register</Link></p>
    </section>
  )
}

export default LoginPage
