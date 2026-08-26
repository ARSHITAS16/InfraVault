import React, { useState } from 'react';
import { Server, Plus, ShieldAlert, Lock, Trash2 } from 'lucide-react';
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

  const [customFields, setCustomFields] = useState<{ key: string; value: string }[]>([]);
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

  const handleAddCustomField = () => {
    setCustomFields((prev) => [...prev, { key: '', value: '' }]);
  };

  const handleCustomFieldChange = (index: number, field: 'key' | 'value', val: string) => {
    setCustomFields((prev) => {
      const updated = [...prev];
      updated[index][field] = val;
      return updated;
    });
  };

  const handleRemoveCustomField = (index: number) => {
    setCustomFields((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.hostname.trim()) return;

    try {
      setLoading(true);
      setError('');
      
      const payload: any = { ...form };
      customFields.forEach((cf) => {
        if (cf.key.trim()) {
          payload[cf.key.trim()] = cf.value;
        }
      });

      await foldersApi.createDevice(folderId, payload);
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
      <div className="modal-content modal-lg" style={{ maxHeight: '90vh' }}>
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
          <div className="modal-header">
            <div className="modal-title">
              <Server size={18} />
              <span>Add Host / Device to Folder ({folderName})</span>
            </div>
            <button type="button" className="modal-close-btn" onClick={onClose}>
              &times;
            </button>
          </div>

          <div className="modal-body" style={{ overflowY: 'auto', maxHeight: 'calc(85vh - 120px)', padding: '20px' }}>
            {error && (
              <div className="alert alert-danger mb-3">
                <ShieldAlert size={16} />
                <span>{error}</span>
              </div>
            )}

            <h4 style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '12px' }}>
              Host & Hardware Specifications
            </h4>

            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Hostname:*</label>
                <input
                  type="text"
                  name="hostname"
                  className="form-control"
                  placeholder="e.g. sjc01-c01-dds01"
                  value={form.hostname}
                  onChange={handleChange}
                  required
                  autoFocus
                />
              </div>

              <div className="form-group">
                <label className="form-label">Model:</label>
                <input
                  type="text"
                  name="model"
                  className="form-control"
                  placeholder="e.g. DataDomain DD9400"
                  value={form.model}
                  onChange={handleChange}
                />
              </div>
            </div>

            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Serial Number:</label>
                <input
                  type="text"
                  name="serialNumber"
                  className="form-control"
                  placeholder="e.g. SN-9948271-SJ"
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
                  placeholder="e.g. 256TB"
                  value={form.capacity}
                  onChange={handleChange}
                />
              </div>
            </div>

            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Service Tag:</label>
                <input
                  type="text"
                  name="serviceTag"
                  className="form-control"
                  placeholder="e.g. 7X889K1"
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
            </div>

            <div className="grid-2" style={{ alignItems: 'center' }}>
              <div className="form-group">
                <label className="form-label">Console Port / Connection:</label>
                <input
                  type="text"
                  name="consolePort"
                  className="form-control"
                  placeholder="e.g. COM1/115200 or IPMI/vKVM"
                  value={form.consolePort}
                  onChange={handleChange}
                />
              </div>

              <div className="form-group" style={{ flexDirection: 'row', alignItems: 'center', gap: '8px', paddingTop: '20px' }}>
                <input
                  type="checkbox"
                  id="idracConfigured"
                  name="idracConfigured"
                  checked={form.idracConfigured}
                  onChange={handleChange}
                  style={{ width: '16px', height: '16px', cursor: 'pointer' }}
                />
                <label htmlFor="idracConfigured" className="form-label" style={{ marginBottom: 0, cursor: 'pointer' }}>
                  iDRAC / Out-of-band Configured
                </label>
              </div>
            </div>

            <hr style={{ borderColor: 'var(--border-color)', margin: '20px 0' }} />

            <h4 style={{ fontSize: '13px', color: 'var(--accent)', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Lock size={14} />
              <span>Encrypted Infrastructure Passwords (AES-256-GCM)</span>
            </h4>

            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">iDRAC Password:</label>
                <input
                  type="password"
                  name="idracPassword"
                  className="form-control"
                  placeholder="Enter iDRAC root password"
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
                  placeholder="Enter sysadmin password"
                  value={form.sysadminPassword}
                  onChange={handleChange}
                />
              </div>
            </div>

            <div className="grid-2">
              <div className="form-group">
                <label className="form-label">Secoff Password:</label>
                <input
                  type="password"
                  name="secoffPassword"
                  className="form-control"
                  placeholder="Enter secoff password"
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
                  placeholder="Enter security passphrase"
                  value={form.passphrase}
                  onChange={handleChange}
                />
              </div>
            </div>

            <hr style={{ borderColor: 'var(--border-color)', margin: '20px 0' }} />

            {/* Dynamic Custom Fields Section */}
            <div className="flex-between mb-2">
              <h4 style={{ fontSize: '13px', color: 'var(--text-secondary)', margin: 0 }}>
                Dynamic Custom Fields & Attributes
              </h4>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={handleAddCustomField}
              >
                <Plus size={14} /> <span>Add Custom Field</span>
              </button>
            </div>

            {customFields.length === 0 ? (
              <p style={{ fontSize: '12px', color: 'var(--text-muted)', fontStyle: 'italic', marginBottom: '16px' }}>
                No custom text boxes added yet. Click "+ Add Custom Field" to add dynamic attributes like vcenter service, comment, or vmcleaner.
              </p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '16px' }}>
                {customFields.map((cf, index) => (
                  <div key={index} style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="Field Name (e.g. comment)"
                      style={{ flex: '1' }}
                      value={cf.key}
                      onChange={(e) => handleCustomFieldChange(index, 'key', e.target.value)}
                    />
                    <input
                      type="text"
                      className="form-control"
                      placeholder="Field Value (e.g. vmcleaner service)"
                      style={{ flex: '1.5' }}
                      value={cf.value}
                      onChange={(e) => handleCustomFieldChange(index, 'value', e.target.value)}
                    />
                    <button
                      type="button"
                      className="btn btn-danger btn-sm p-1"
                      onClick={() => handleRemoveCustomField(index)}
                      title="Remove field"
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading || !form.hostname.trim()}>
              {loading ? <span className="spinner"></span> : <><Plus size={16} /> <span>Create Device</span></>}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
