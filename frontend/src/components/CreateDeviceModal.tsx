import React, { useState } from 'react';
import { Server, Plus, ShieldAlert, Lock } from 'lucide-react';
import { foldersApi } from '../services/api';

interface CreateDeviceModalProps {
  folderId: number;
  folderName: string;
  onClose: () => void;
  onSuccess: () => void;
}

export const CreateDeviceModal: React.FC<CreateDeviceModalProps> = ({
  folderId,
  folderName,
  onClose,
  onSuccess,
}) => {
  const [form, setForm] = useState({
    hostname: '',
    model: '',
    serialNumber: '',
    capacity: '',
    serviceTag: '',
    supportEndDate: '',
    consolePort: '',
    idracConfigured: false,

    idracPassword: '',
    sysadminPassword: '',
    secoffPassword: '',
    passphrase: '',
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    if (type === 'checkbox') {
      const checked = (e.target as HTMLInputElement).checked;
      setForm((prev) => ({ ...prev, [name]: checked }));
    } else {
      setForm((prev) => ({ ...prev, [name]: value }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.hostname.trim()) return;

    try {
      setLoading(true);
      setError('');
      await foldersApi.createDevice(folderId, form);
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to create device');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-content modal-lg">
        <form onSubmit={handleSubmit}>
          <div className="modal-header">
            <div className="modal-title">
              <Server size={18} />
              <span>Add Host / Device to Folder ({folderName})</span>
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

            <div className="section-title text-primary mb-2">Host & Hardware Specifications</div>
            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Hostname:*</label>
                <input
                  type="text"
                  name="hostname"
                  className="form-control"
                  placeholder="e.g. blr01-c01-dds01"
                  value={form.hostname}
                  onChange={handleChange}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Model:</label>
                <input
                  type="text"
                  name="model"
                  className="form-control"
                  placeholder="e.g. DataDomain DD9400 / PowerEdge R750"
                  value={form.model}
                  onChange={handleChange}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Serial Number:</label>
                <input
                  type="text"
                  name="serialNumber"
                  className="form-control"
                  placeholder="e.g. SN-88492041"
                  value={form.serialNumber}
                  onChange={handleChange}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Capacity / Storage:</label>
                <input
                  type="text"
                  name="capacity"
                  className="form-control"
                  placeholder="e.g. 128TB"
                  value={form.capacity}
                  onChange={handleChange}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Service Tag:</label>
                <input
                  type="text"
                  name="serviceTag"
                  className="form-control"
                  placeholder="e.g. 7X992K2"
                  value={form.serviceTag}
                  onChange={handleChange}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Support End Date (EOD):</label>
                <input
                  type="date"
                  name="supportEndDate"
                  className="form-control"
                  value={form.supportEndDate}
                  onChange={handleChange}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Console Port:</label>
                <input
                  type="text"
                  name="consolePort"
                  className="form-control"
                  placeholder="e.g. TTY1 / Port 22"
                  value={form.consolePort}
                  onChange={handleChange}
                />
              </div>

              <div className="form-group flex-center-y pt-4">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    name="idracConfigured"
                    checked={form.idracConfigured}
                    onChange={handleChange}
                  />
                  <span>iDRAC / Out-of-band Configured</span>
                </label>
              </div>
            </div>

            <div className="section-title text-primary mt-4 mb-2 flex-align">
              <Lock size={15} />
              <span>Encrypted Infrastructure Passwords (AES-256-GCM)</span>
            </div>

            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">iDRAC Password:</label>
                <input
                  type="password"
                  name="idracPassword"
                  className="form-control"
                  placeholder="Enter iDRAC password"
                  value={form.idracPassword}
                  onChange={handleChange}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Sysadmin Password:</label>
                <input
                  type="password"
                  name="sysadminPassword"
                  className="form-control"
                  placeholder="Enter Sysadmin password"
                  value={form.sysadminPassword}
                  onChange={handleChange}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Secoff Password:</label>
                <input
                  type="password"
                  name="secoffPassword"
                  className="form-control"
                  placeholder="Enter Secoff password"
                  value={form.secoffPassword}
                  onChange={handleChange}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Passphrase:</label>
                <input
                  type="password"
                  name="passphrase"
                  className="form-control"
                  placeholder="Enter Passphrase"
                  value={form.passphrase}
                  onChange={handleChange}
                />
              </div>
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading || !form.hostname.trim()}>
              {loading ? <span className="spinner"></span> : <><Plus size={16} /> <span>Save Host</span></>}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
