import React, { useState } from 'react';
import { Upload, FileSpreadsheet, Check, ShieldAlert, AlertTriangle } from 'lucide-react';
import { Folder, ImportPreviewRow } from '../types';
import { importApi } from '../services/api';

interface ExcelImportModalProps {
  folders: Folder[];
  defaultFolderId?: number;
  onClose: () => void;
  onSuccess: () => void;
}

export const ExcelImportModal: React.FC<ExcelImportModalProps> = ({
  folders,
  defaultFolderId,
  onClose,
  onSuccess,
}) => {
  const [selectedFolderId, setSelectedFolderId] = useState<number | ''>(
    defaultFolderId || (folders.length > 0 ? folders[0].id : '')
  );
  const [file, setFile] = useState<File | null>(null);
  const [previewRows, setPreviewRows] = useState<ImportPreviewRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [committing, setCommitting] = useState(false);
  const [error, setError] = useState('');
  const [step, setStep] = useState<'upload' | 'preview'>('upload');

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setFile(e.target.files[0]);
    }
  };

  const handlePreview = async () => {
    if (!file || !selectedFolderId) return;

    try {
      setLoading(true);
      setError('');
      const preview = await importApi.preview(file, Number(selectedFolderId));
      setPreviewRows(preview);
      setStep('preview');
    } catch (err: any) {
      setError(err.message || 'Failed to parse Excel file');
    } finally {
      setLoading(false);
    }
  };

  const handleCommit = async () => {
    if (!selectedFolderId || previewRows.length === 0) return;

    const validRows = previewRows.filter((r) => r.valid);
    if (validRows.length === 0) {
      setError('No valid rows to import.');
      return;
    }

    try {
      setCommitting(true);
      setError('');
      await importApi.commit(Number(selectedFolderId), validRows);
      onSuccess();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to commit import');
    } finally {
      setCommitting(false);
    }
  };

  const validCount = previewRows.filter((r) => r.valid).length;
  const invalidCount = previewRows.filter((r) => !r.valid).length;

  return (
    <div className="modal-backdrop">
      <div className="modal-content modal-xl">
        <div className="modal-header">
          <div className="modal-title">
            <FileSpreadsheet size={18} />
            <span>Bulk Add Infrastructure Devices via Excel (.xlsx)</span>
          </div>
          <button className="modal-close-btn" onClick={onClose}>
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

          {step === 'upload' ? (
            <div className="upload-step-container">
              <div className="form-group mb-3">
                <label className="form-label">Target Destination Folder:*</label>
                <select
                  className="form-control"
                  value={selectedFolderId}
                  onChange={(e) => setSelectedFolderId(Number(e.target.value))}
                >
                  {folders.length === 0 ? (
                    <option value="">No folders available in current DC</option>
                  ) : (
                    folders.map((f) => (
                      <option key={f.id} value={f.id}>
                        {f.name} ({f.datacenter?.name || 'Folder'})
                      </option>
                    ))
                  )}
                </select>
              </div>

              <div className="file-drop-zone">
                <Upload size={36} className="text-primary mb-2" />
                <p className="drop-title">Upload Excel File (.xlsx)</p>
                <p className="drop-subtext">
                  Supported columns: <code>hostname</code>, <code>model</code>, <code>serial_number</code>, <code>capacity</code>, <code>service_tag</code>, <code>support_eod</code>, <code>idrac_password</code>, <code>sysadmin_password</code>, <code>secoff_password</code>, <code>passphrase</code>
                </p>
                <input
                  type="file"
                  accept=".xlsx, .xls"
                  onChange={handleFileChange}
                  className="file-input-hidden"
                  id="excelFileInput"
                />
                <label htmlFor="excelFileInput" className="btn btn-secondary mt-2">
                  Select File
                </label>
                {file && <div className="selected-filename mt-2 font-weight-bold">{file.name}</div>}
              </div>
            </div>
          ) : (
            <div className="preview-step-container">
              <div className="preview-summary-bar">
                <span className="badge badge-success">{validCount} Valid Rows</span>
                {invalidCount > 0 && (
                  <span className="badge badge-danger">{invalidCount} Rows with Errors</span>
                )}
                <span className="text-muted ml-auto">Total Rows: {previewRows.length}</span>
              </div>

              <div className="table-responsive preview-table-wrap">
                <table className="infravault-table">
                  <thead>
                    <tr>
                      <th>Row</th>
                      <th>Hostname</th>
                      <th>Model</th>
                      <th>Serial</th>
                      <th>Support EOD</th>
                      <th>iDRAC Pass</th>
                      <th>Sysadmin Pass</th>
                      <th>Secoff Pass</th>
                      <th>Passphrase</th>
                      <th>Status / Errors</th>
                    </tr>
                  </thead>
                  <tbody>
                    {previewRows.map((r, i) => (
                      <tr key={i} className={r.valid ? 'row-valid' : 'row-invalid'}>
                        <td>{r.rowNum}</td>
                        <td><strong>{r.hostname || '-'}</strong></td>
                        <td>{r.model || '-'}</td>
                        <td>{r.serialNumber || '-'}</td>
                        <td>{r.supportEod || '-'}</td>
                        <td>{r.idracPassword || '-'}</td>
                        <td>{r.sysadminPassword || '-'}</td>
                        <td>{r.secoffPassword || '-'}</td>
                        <td>{r.passphrase || '-'}</td>
                        <td>
                          {r.valid ? (
                            <span className="text-success flex-align">
                              <Check size={14} /> Ready
                            </span>
                          ) : (
                            <span className="text-danger flex-align">
                              <AlertTriangle size={14} /> {r.errors.join(', ')}
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>

        <div className="modal-footer">
          {step === 'preview' && (
            <button className="btn btn-secondary" onClick={() => setStep('upload')}>
              Back to Upload
            </button>
          )}

          <button className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>

          {step === 'upload' ? (
            <button
              className="btn btn-primary"
              onClick={handlePreview}
              disabled={loading || !file || !selectedFolderId}
            >
              {loading ? <span className="spinner"></span> : <span>Preview Spreadsheet</span>}
            </button>
          ) : (
            <button
              className="btn btn-success"
              onClick={handleCommit}
              disabled={committing || validCount === 0}
            >
              {committing ? <span className="spinner"></span> : <><Check size={16} /> <span>Commit {validCount} Hosts</span></>}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
