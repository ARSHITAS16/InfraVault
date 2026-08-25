import React, { useState } from 'react';
import { FolderPlus, Plus, ShieldAlert } from 'lucide-react';
import { datacentersApi } from '../services/api';

interface CreateFolderModalProps {
  datacenterId: number;
  datacenterName: string;
  onClose: () => void;
  onSuccess: () => void;
}

export const CreateFolderModal: React.FC<CreateFolderModalProps> = ({
  datacenterId,
  datacenterName,
  onClose,
  onSuccess,
}) => {
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    try {
      setLoading(true);
      setError('');
      await datacentersApi.createFolder(datacenterId, name.trim());
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to create folder');
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
              <FolderPlus size={18} />
              <span>Create Infrastructure Folder in {datacenterName}</span>
            </div>
            <button type="button" className="modal-close-btn" onClick={onClose}>
              &times;
            </button>
          </div>

          <div className="modal-body">
            <div className="form-group">
              <label className="form-label">Folder Name (e.g. datadomain, datacenter-cluster):</label>
              <input
                type="text"
                className="form-control"
                placeholder="e.g. datadomain"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                autoFocus
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
              {loading ? <span className="spinner"></span> : <><Plus size={16} /> <span>Create Folder</span></>}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
