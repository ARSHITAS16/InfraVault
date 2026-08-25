import React, { useState } from 'react';
import { Database, Plus, ShieldAlert } from 'lucide-react';
import { datacentersApi } from '../services/api';

interface CreateDatacenterModalProps {
  onClose: () => void;
  onSuccess: () => void;
}

export const CreateDatacenterModal: React.FC<CreateDatacenterModalProps> = ({
  onClose,
  onSuccess,
}) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    try {
      setLoading(true);
      setError('');
      await datacentersApi.createDatacenter(name.trim(), description.trim());
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to create datacenter');
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
              <Database size={18} />
              <span>Create New Datacenter</span>
            </div>
            <button type="button" className="modal-close-btn" onClick={onClose}>
              &times;
            </button>
          </div>

          <div className="modal-body">
            <div className="form-group">
              <label className="form-label">Datacenter Name (e.g. sjc51, iad51):</label>
              <input
                type="text"
                className="form-control"
                placeholder="Enter datacenter name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                autoFocus
              />
            </div>

            <div className="form-group">
              <label className="form-label">Description / Location:</label>
              <input
                type="text"
                className="form-control"
                placeholder="e.g. San Jose Primary DC"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>

            {error && (
              <div className="alert alert-danger">
                <ShieldAlert size={16} />
                <span>{error}</span>
              </div>
            )}
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading || !name.trim()}>
              {loading ? <span className="spinner"></span> : <><Plus size={16} /> <span>Create Datacenter</span></>}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
