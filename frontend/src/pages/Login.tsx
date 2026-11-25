import React, { useState } from 'react';
import { Lock, User, Mail, ShieldAlert, Info } from 'lucide-react';
import ringCentralLogo from '../assets/ringcentral-logo.png';
import { authApi } from '../services/api';

interface LoginProps {
  onLoginSuccess: (token: string, username: string, userId: number, role: string) => void;
  sessionNotice?: string;
}

export const Login: React.FC<LoginProps> = ({ onLoginSuccess, sessionNotice }) => {
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      setLoading(true);
      if (isRegister) {
        const res = await authApi.register(username, email, password);
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
          {sessionNotice && !error && (
            <div className="alert alert-warning mb-3">
              <Info size={16} />
              <span>{sessionNotice}</span>
            </div>
          )}

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
                placeholder={isRegister ? 'Minimum 8 characters' : 'Enter password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                minLength={isRegister ? 8 : undefined}
                required
              />
            </div>
          </div>

          {isRegister && (
            <div className="alert alert-info mb-3" style={{ fontSize: '12px' }}>
              <Info size={15} />
              <span>
                Self-registered accounts are created with <strong>VIEWER</strong> privileges until an administrator assigns Datacenter access.
              </span>
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
        </div>
      </div>
    </div>
  );
};
