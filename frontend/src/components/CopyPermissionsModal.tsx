import React, { useState } from 'react';
import { Copy, ShieldAlert, Check } from 'lucide-react';
import { Datacenter } from '../types';
import { datacentersApi } from '../services/api';

interface CopyPermissionsModalProps {
  targetDatacenter: Datacenter;
  allDatacenters: Datacenter[];
  onClose: () => void;
  onSuccess: () => void;
}

export const CopyPermissionsModal: React.FC<CopyPermissionsModalProps> = ({
  targetDatacenter,
  allDatacenters,
  onClose,
  onSuccess,
}) => {
  const availableSources = allDatacenters.filter(
    (dc) => dc.id !== targetDatacenter.id
  );

  const [selectedSourceId, setSelectedSourceId] = useState<number | ''>(
    availableSources.length > 0 ? availableSources[0].id : ''
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleCopy = async () => {
    if (!selectedSourceId) return;

    try {
      setLoading(true);
      setError('');
      await datacentersApi.copyPermissions(
        targetDatacenter.id,
        Number(selectedSourceId)
      );
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to copy permissions');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-content modal-md">
        <div className="modal-header">
          <div className="modal-title">
            <Copy size={18} />
            <span>Copy User Permissions to Datacenter</span>
          </div>
          <button className="modal-close-btn" onClick={onClose}>
            &times;
          </button>
        </div>

        <div className="modal-body">
          <p className="modal-desc">
            Copy all user access mappings and permission levels from an existing Datacenter to <strong>{targetDatacenter.name}</strong>.
          </p>

          {availableSources.length === 0 ? (
            <div className="alert alert-warning">
              No other Datacenters available to copy permissions from.
            </div>
          ) : (
            <div className="form-group">
              <label className="form-label">Select Source Datacenter:</label>
              <select
                className="form-control"
                value={selectedSourceId}
                onChange={(e) => setSelectedSourceId(Number(e.target.value))}
              >
                {availableSources.map((dc) => (
                  <option key={dc.id} value={dc.id}>
                    {dc.name} ({dc.description || 'Datacenter'})
                  </option>
                ))}
              </select>
            </div>
          )}

          {error && (
            <div className="alert alert-danger">
              <ShieldAlert size={16} />
              <span>{error}</span>
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button
            className="btn btn-primary"
            onClick={handleCopy}
            disabled={loading || !selectedSourceId}
          >
            {loading ? <span className="spinner"></span> : <><Check size={16} /> <span>Copy Permissions</span></>}
          </button>
        </div>
      </div>
    </div>
  );
};
