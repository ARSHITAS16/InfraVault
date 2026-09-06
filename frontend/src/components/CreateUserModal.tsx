import React, { useState } from 'react';
import { UserPlus, ShieldAlert, Check } from 'lucide-react';
import { usersApi } from '../services/api';

interface CreateUserModalProps {
  onClose: () => void;
  onCreated: () => void;
}

export const CreateUserModal: React.FC<CreateUserModalProps> = ({ onClose, onCreated }) => {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('OPERATOR');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !email.trim() || !password.trim()) return;

    try {
      setLoading(true);
      setError('');
      await usersApi.createUser(username.trim(), email.trim(), password, role);
      onCreated();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to create user account');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-content modal-md">
        <form onSubmit={handleSubmit}>
          <div className="modal-header">
            <div className="modal-title">
              <UserPlus size={18} />
              <span>Create New System User</span>
            </div>
            <button type="button" className="modal-close-btn" onClick={onClose}>
              &times;
            </button>
          </div>

          <div className="modal-body">
            {error && (
              <div className="alert alert-danger mb-3">
                <ShieldAlert size={16} />
                <span>{error}</span>
              </div>
            )}

            <div className="form-group mb-3">
              <label className="form-label">Username:*</label>
              <input
                type="text"
                className="form-control"
                placeholder="e.g. john_doe"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                autoFocus
              />
            </div>

            <div className="form-group mb-3">
              <label className="form-label">Email Address:*</label>
              <input
                type="email"
                className="form-control"
                placeholder="e.g. john.doe@company.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="form-group mb-3">
              <label className="form-label">Password:*</label>
              <input
                type="password"
                className="form-control"
                placeholder="Enter initial user password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>

            <div className="form-group mb-3">
              <label className="form-label">Global Authorization Role:</label>
              <select className="form-control" value={role} onChange={(e) => setRole(e.target.value)}>
                <option value="OPERATOR">OPERATOR (Standard Read/Write User)</option>
                <option value="DC_ADMIN">DC_ADMIN (Datacenter Administrator)</option>
                <option value="SUPER_ADMIN">SUPER_ADMIN (Full System Administrator)</option>
                <option value="VIEWER">VIEWER (Read-Only Access)</option>
              </select>
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading || !username.trim() || !email.trim() || !password.trim()}>
              {loading ? <span className="spinner"></span> : <><UserPlus size={16} /> <span>Create Account</span></>}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
