import { useState, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { registerUser, login, uploadAvatar } from '../lib/api'
import noicon from '../assets/noicon.png'

function RegisterPage() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [errors, setErrors] = useState({})
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const [avatarFile, setAvatarFile] = useState(null)
  const [avatarPreview, setAvatarPreview] = useState(null)
  const avatarInputRef = useRef(null)

  const validate = () => {
    const e = {}
    if (!/^[a-zA-Z0-9]{4,30}$/.test(username.trim()))
      e.username = 'Username must be 4–30 English letters or numbers'
    if (displayName.trim().length < 1 || displayName.trim().length > 30)
      e.displayName = 'Display name must be 1–30 characters'
    if (!/^[a-zA-Z0-9!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?`~]{6,30}$/.test(password))
      e.password = 'Password must be 6–30 characters (letters, numbers or special characters)'
    if (password !== confirmPassword)
      e.confirmPassword = 'Passwords do not match'
    return e
  }

  const handleAvatarChange = (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    if (file.size > 10 * 1024 * 1024) {
      setMessage('Image must be under 10MB')
      return
    }
    setAvatarFile(file)
    setAvatarPreview(URL.createObjectURL(file))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    const validationErrors = validate()
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors)
      return
    }
    setErrors({})
    setLoading(true)
    setMessage('')

    try {
      const result = await registerUser(username.trim(), displayName.trim(), password)
      if (result === false) {
        setMessage('Registration failed: username already exists')
        return
      }
      if (avatarFile) {
        await login(username.trim(), password)
        await uploadAvatar(avatarFile)
      }
      navigate('/login')
    } catch (error) {
      setMessage(`Registration failed: ${error.message}`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="page-card login-page-card">
      <h2>Register</h2>
      <div
        className="register-avatar-picker"
        onClick={() => avatarInputRef.current?.click()}
        title="Click to set avatar"
      >
        <img src={avatarPreview || noicon} alt="avatar preview" />
        <span className="register-avatar-hint">Upload Icon</span>
      </div>
      <input
        ref={avatarInputRef}
        type="file"
        accept="image/*"
        style={{ display: 'none' }}
        onChange={handleAvatarChange}
      />
      <form onSubmit={handleSubmit} className="form-col login-form">
        <input
          placeholder="Username"
          value={username}
          onChange={(event) => { setUsername(event.target.value); setErrors(prev => ({ ...prev, username: undefined })) }}
        />
        {errors.username && <p className="field-error">{errors.username}</p>}
        <input
          placeholder="Display Name"
          value={displayName}
          onChange={(event) => { setDisplayName(event.target.value); setErrors(prev => ({ ...prev, displayName: undefined })) }}
        />
        {errors.displayName && <p className="field-error">{errors.displayName}</p>}
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(event) => { setPassword(event.target.value); setErrors(prev => ({ ...prev, password: undefined })) }}
        />
        {errors.password && <p className="field-error">{errors.password}</p>}
          <input
            type="password"
            placeholder="Confirm Password"
            value={confirmPassword}
            onChange={(event) => { setConfirmPassword(event.target.value); setErrors(prev => ({ ...prev, confirmPassword: undefined })) }}
          />
          {errors.confirmPassword && <p className="field-error">{errors.confirmPassword}</p>}
        <button disabled={loading}>{loading ? 'Registering...' : 'Register'}</button>
      </form>
      {message && <p className="error-text">{message}</p>}
      <p className="sub">Already have an account? <Link to="/login">Back to Login</Link></p>
    </section>
  )
}

export default RegisterPage
