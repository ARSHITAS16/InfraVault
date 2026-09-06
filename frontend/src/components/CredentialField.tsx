import React, { useState, useEffect } from 'react';
import { Eye, EyeOff, Copy, Check, Lock, ShieldAlert, Edit3, KeyRound } from 'lucide-react';
import { credentialsApi } from '../services/api';

interface CredentialFieldProps {
  credentialId: number;
  type: string;
  username: string;
  datacenterId: number;
}

export const CredentialField: React.FC<CredentialFieldProps> = ({
  credentialId,
  type,
  username,
  datacenterId,
}) => {
  const [revealed, setRevealed] = useState(false);
  const [secret, setSecret] = useState('');
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState('');
  const [timeLeft, setTimeLeft] = useState(15);

  // Edit Mode state
  const [isEditing, setIsEditing] = useState(false);
  const [newPassword, setNewPassword] = useState('');
  const [savingEdit, setSavingEdit] = useState(false);
  const [editSuccess, setEditSuccess] = useState(false);

  useEffect(() => {
    let timer: any = null;
    if (revealed && timeLeft > 0) {
      timer = setInterval(() => {
        setTimeLeft((prev) => prev - 1);
      }, 1000);
    } else if (timeLeft === 0) {
      setRevealed(false);
      setSecret('');
      setTimeLeft(15);
    }
    return () => {
      if (timer) clearInterval(timer);
    };
  }, [revealed, timeLeft]);

  const handleToggleReveal = async () => {
    if (revealed) {
      setRevealed(false);
      setSecret('');
      setTimeLeft(15);
      return;
    }

    try {
      setLoading(true);
      setError('');
      const res = await credentialsApi.reveal(credentialId, datacenterId);
      setSecret(res.secret);
      setRevealed(true);
      setTimeLeft(15);
    } catch (err: any) {
      setError(err.message || 'Failed to reveal password');
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = () => {
    if (secret) {
      navigator.clipboard.writeText(secret);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPassword) return;

    try {
      setSavingEdit(true);
      setError('');
      await credentialsApi.update(credentialId, newPassword, datacenterId);

      setEditSuccess(true);
      setIsEditing(false);
      setNewPassword('');

      if (revealed) {
        setSecret(newPassword);
      }

      setTimeout(() => setEditSuccess(false), 3000);
    } catch (err: any) {
      setError(err.message || 'Failed to update password');
    } finally {
      setSavingEdit(false);
    }
  };

  return (
    <div className="credential-row-card">
      <div className="cred-type-badge flex-between">
        <div className="flex-align gap-1">
          <Lock size={14} />
          <span>{type}</span>
        </div>
        {editSuccess && (
          <span className="badge badge-success" style={{ fontSize: '10px' }}>
            <Check size={12} /> Updated!
          </span>
        )}
      </div>

      <div className="cred-details">
        <div className="cred-username">
          <span className="label">Username:</span>
          <span className="value">{username || 'N/A'}</span>
        </div>

        {isEditing ? (
          <form onSubmit={handleSaveEdit} className="mt-2 p-2" style={{ backgroundColor: '#0f172a', borderRadius: '6px', border: '1px solid var(--primary)' }}>
            <label className="form-label mb-1" style={{ fontSize: '11px', color: 'var(--primary)' }}>
              New Encrypted Password:
            </label>
            <div className="flex-align gap-2">
              <input
                type="password"
                className="form-control form-control-sm"
                placeholder="Enter new secret"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                autoFocus
              />
              <button
                type="submit"
                className="btn btn-primary btn-sm"
                disabled={savingEdit || !newPassword}
                style={{ whiteSpace: 'nowrap' }}
              >
                {savingEdit ? <span className="spinner"></span> : 'Save'}
              </button>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => {
                  setIsEditing(false);
                  setNewPassword('');
                }}
              >
                Cancel
              </button>
            </div>
          </form>
        ) : (
          <div className="cred-secret-box">
            <span className="label">Password:</span>
            <div className="secret-display">
              {revealed ? (
                <span className="secret-text plaintext">{secret}</span>
              ) : (
                <span className="secret-text masked">••••••••••••</span>
              )}
            </div>
          </div>
        )}
      </div>

      <div className="cred-actions" style={{ gap: '6px' }}>
        {revealed && (
          <>
            <span className="timer-badge">{timeLeft}s remaining</span>
            <button
              type="button"
              className="btn btn-icon btn-copy"
              onClick={handleCopy}
              title="Copy Password"
            >
              {copied ? <Check size={16} className="text-success" /> : <Copy size={16} />}
            </button>
          </>
        )}

        <button
          type="button"
          className="btn btn-secondary btn-sm flex-align gap-1"
          onClick={() => {
            setIsEditing(!isEditing);
            setNewPassword('');
          }}
          title="Edit Password"
        >
          <Edit3 size={14} />
          <span>{isEditing ? 'Cancel Edit' : 'Edit'}</span>
        </button>

        <button
          type="button"
          className={`btn btn-reveal ${revealed ? 'btn-danger' : 'btn-primary'}`}
          onClick={handleToggleReveal}
          disabled={loading}
        >
          {loading ? (
            <span className="spinner"></span>
          ) : revealed ? (
            <>
              <EyeOff size={15} />
              <span>Hide</span>
            </>
          ) : (
            <>
              <Eye size={15} />
              <span>Reveal Secret</span>
            </>
          )}
        </button>
      </div>

      {error && (
        <div className="cred-error mt-2">
          <ShieldAlert size={14} />
          <span>{error}</span>
        </div>
      )}
    </div>
  );
};
