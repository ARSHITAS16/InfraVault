import React, { useState } from 'react';
import { Lock, User, Mail, ShieldCheck, ShieldAlert } from 'lucide-react';
import ringCentralLogo from '../assets/ringcentral-logo.png';
import { authApi } from '../services/api';

interface LoginProps {
  onLoginSuccess: (token: string, username: string, userId: number, role: string) => void;
}

export const Login: React.FC<LoginProps> = ({ onLoginSuccess }) => {
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('OPERATOR');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      setLoading(true);
      if (isRegister) {
        const res = await authApi.register(username, email, password, role);
        onLoginSuccess(res.token, res.username, res.id, res.role);
      } else {
        const res = await authApi.login(username, password);
        onLoginSuccess(res.token, res.username, res.id, res.role);
      }
    } catch (err: any) {
      setError(err.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-screen-bg">
      <div className="login-card-container">
        <div className="login-card-header text-center">
          <img
            src={ringCentralLogo}
            alt="RingCentral Logo"
            className="login-ringcentral-logo"
          />
          <h2 className="login-title">InfraVault</h2>
          <p className="login-subtitle">
            Internal Password Management & Infrastructure Inventory
          </p>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          {error && (
            <div className="alert alert-danger mb-3">
              <ShieldAlert size={16} />
              <span>{error}</span>
            </div>
          )}

          <div className="form-group">
            <label className="form-label">Username:</label>
            <div className="input-group">
              <User className="input-icon" size={16} />
              <input
                type="text"
                className="form-control"
                placeholder="Enter username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                autoFocus
              />
            </div>
          </div>

          {isRegister && (
            <div className="form-group">
              <label className="form-label">Email Address:</label>
              <div className="input-group">
                <Mail className="input-icon" size={16} />
                <input
                  type="email"
                  className="form-control"
                  placeholder="Enter email address"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>
            </div>
          )}

          <div className="form-group">
            <label className="form-label">Password:</label>
            <div className="input-group">
              <Lock className="input-icon" size={16} />
              <input
                type="password"
                className="form-control"
                placeholder="Enter password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
          </div>

          {isRegister && (
            <div className="form-group">
              <label className="form-label">Initial Role:</label>
              <select
                className="form-control"
                value={role}
                onChange={(e) => setRole(e.target.value)}
              >
                <option value="SUPER_ADMIN">SUPER_ADMIN (System Administrator)</option>
                <option value="DC_ADMIN">DC_ADMIN (Datacenter Administrator)</option>
                <option value="OPERATOR">OPERATOR (Read & Write Devices)</option>
                <option value="VIEWER">VIEWER (Read Only)</option>
              </select>
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary btn-block btn-lg mt-4"
            disabled={loading}
          >
            {loading ? (
              <span className="spinner"></span>
            ) : isRegister ? (
              <span>Create Account & Sign In</span>
            ) : (
              <span>Sign In to InfraVault</span>
            )}
          </button>
        </form>

        <div className="login-card-footer text-center mt-4">
          <button
            type="button"
            className="btn-link"
            onClick={() => {
              setIsRegister(!isRegister);
              setError('');
            }}
          >
            {isRegister
              ? 'Already have an account? Sign In'
              : "Don't have an account? Register new user"}
          </button>

          <div style={{ marginTop: '16px', borderTop: '1px solid rgba(255,255,255,0.1)', paddingTop: '16px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-block"
              style={{ backgroundColor: '#2d3748', borderColor: '#4a5568', color: '#e2e8f0' }}
              onClick={() => {
                const demoUser = {
                  userId: 1,
                  username: 'admin (Demo Mode)',
                  role: 'SUPER_ADMIN',
                  token: 'demo-token-123'
                };
                onLoginSuccess(demoUser);
              }}
            >
              🚀 Explore Live UI Demo Mode
            </button>
            <small style={{ color: '#a0aec0', fontSize: '11px', display: 'block', marginTop: '6px' }}>
              Instantly explore InfraVault UI & features on GitHub Pages
            </small>
          </div>
        </div>
      </div>
    </div>
  );
};
