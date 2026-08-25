import React, { useState, useEffect } from 'react';
import { Eye, EyeOff, Copy, Check, Lock, ShieldAlert } from 'lucide-react';
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

  return (
    <div className="credential-row-card">
      <div className="cred-type-badge">
        <Lock size={14} />
        <span>{type}</span>
      </div>

      <div className="cred-details">
        <div className="cred-username">
          <span className="label">Username:</span>
          <span className="value">{username || 'N/A'}</span>
        </div>

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
      </div>

      <div className="cred-actions">
        {revealed && (
          <>
            <span className="timer-badge">{timeLeft}s remaining</span>
            <button
              className="btn btn-icon btn-copy"
              onClick={handleCopy}
              title="Copy Password"
            >
              {copied ? <Check size={16} className="text-success" /> : <Copy size={16} />}
            </button>
          </>
        )}

        <button
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
        <div className="cred-error">
          <ShieldAlert size={14} />
          <span>{error}</span>
        </div>
      )}
    </div>
  );
};
