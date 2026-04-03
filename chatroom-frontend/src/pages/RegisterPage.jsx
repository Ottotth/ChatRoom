import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { registerUser } from '../lib/api'

function RegisterPage() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setMessage('')

    try {
      const result = await registerUser(username.trim(), displayName.trim(), password)
      if (result === false) {
        setMessage('Registration failed: username already exists')
        return
      }
      navigate('/login')
    } catch (error) {
      setMessage(`Registration failed: ${error.message}`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="page-card">
      <h1>Register</h1>
      <p className="sub">Create an account to sign in.</p>
      <form onSubmit={handleSubmit} className="form-col">
        <label>
          Username
          <input value={username} onChange={(event) => setUsername(event.target.value)} required />
        </label>
        <label>
          Display Name
          <input value={displayName} onChange={(event) => setDisplayName(event.target.value)} required />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required />
        </label>
        <button disabled={loading}>{loading ? 'Registering...' : 'Register'}</button>
      </form>
      {message && <p className="error-text">{message}</p>}
      <p className="sub">Already have an account? <Link to="/login">Back to Login</Link></p>
    </section>
  )
}

export default RegisterPage
