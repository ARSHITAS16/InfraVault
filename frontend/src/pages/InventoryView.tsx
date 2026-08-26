import React, { useState, useEffect } from 'react';
import {
  Database,
  Folder,
  Server,
  Lock,
  Plus,
  Trash2,
  Users,
  Copy,
  Upload,
  Calendar,
  Tag,
  HardDrive,
  Cpu,
  ShieldCheck,
  FileSpreadsheet,
  AlertTriangle,
  Key,
} from 'lucide-react';
import { Datacenter, Folder as FolderType, Device, CredentialMasked } from '../types';
import { SidebarTree, TreeDataNode } from '../components/SidebarTree';
import { CredentialField } from '../components/CredentialField';
import { devicesApi, credentialsApi } from '../services/api';

interface InventoryViewProps {
  treeData: TreeDataNode[];
  selectedNode: TreeDataNode | null;
  allDatacenters: Datacenter[];
  allFolders: FolderType[];
  onSelectNode: (node: TreeDataNode) => void;
  onAddFolder: (dcId: number) => void;
  onAddDevice: (folderId: number) => void;
  onManageUsers: (dcId: number) => void;
  onCopyPermissions: (dcId: number) => void;
  onOpenImport: (folderId?: number) => void;
  onDeleteNode: (node: TreeDataNode) => void;
}

export const InventoryView: React.FC<InventoryViewProps> = ({
  treeData,
  selectedNode,
  allDatacenters,
  allFolders,
  onSelectNode,
  onAddFolder,
  onAddDevice,
  onManageUsers,
  onCopyPermissions,
  onOpenImport,
  onDeleteNode,
}) => {
  const [activeTab, setActiveTab] = useState<'summary' | 'credentials' | 'permissions'>('summary');
  const [credentials, setCredentials] = useState<CredentialMasked[]>([]);
  const [loadingCreds, setLoadingCreds] = useState(false);

  // Dynamic inline secret creation
  const [showAddSecret, setShowAddSecret] = useState(false);
  const [newSecretType, setNewSecretType] = useState('');
  const [newSecretUser, setNewSecretUser] = useState('root');
  const [newSecretPass, setNewSecretPass] = useState('');
  const [savingSecret, setSavingSecret] = useState(false);

  const loadCredentials = () => {
    if (selectedNode && selectedNode.type === 'device') {
      setLoadingCreds(true);
      devicesApi
        .getCredentials(selectedNode.numericId, selectedNode.datacenterId)
        .then((data) => setCredentials(data))
        .catch(() => setCredentials([]))
        .finally(() => setLoadingCreds(false));
    }
  };

  useEffect(() => {
    loadCredentials();
    setShowAddSecret(false);
  }, [selectedNode]);

  const handleAddSecretSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newSecretType.trim() || !selectedNode || selectedNode.type !== 'device') return;

    try {
      setSavingSecret(true);
      await credentialsApi.create(
        selectedNode.numericId,
        newSecretType.trim(),
        newSecretUser.trim() || 'root',
        newSecretPass
      );
      setNewSecretType('');
      setNewSecretUser('root');
      setNewSecretPass('');
      setShowAddSecret(false);
      loadCredentials();
    } catch (err: any) {
      alert(err.message || 'Failed to add encrypted secret');
    } finally {
      setSavingSecret(false);
    }
  };

  if (!selectedNode) {
    return (
      <div className="infravault-split-layout">
        <SidebarTree
          treeData={treeData}
          selectedNodeId={null}
          onSelectNode={onSelectNode}
          onAddFolder={onAddFolder}
          onAddDevice={onAddDevice}
          onManageUsers={onManageUsers}
          onCopyPermissions={onCopyPermissions}
          onDeleteNode={onDeleteNode}
        />
        <div className="infravault-main-panel empty-selection-panel">
          <div className="empty-state">
            <Database size={48} className="text-muted mb-3" />
            <h3>No Inventory Item Selected</h3>
            <p className="text-muted">
              Select a Datacenter, Folder, or Host from the left inventory tree to view details and secrets.
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="infravault-inventory-split">
      <div className="inventory-sidebar">
        <div className="sidebar-header">
          <span>Infrastructure Tree</span>
        </div>
        <div className="sidebar-content">
          <SidebarTree
            treeData={treeData}
            selectedNodeId={selectedNode.id}
            onSelectNode={onSelectNode}
            onAddFolder={onAddFolder}
            onAddDevice={onAddDevice}
            onManageUsers={onManageUsers}
            onCopyPermissions={onCopyPermissions}
            onDeleteNode={onDeleteNode}
          />
        </div>
      </div>

      <div className="inventory-details-pane">
        <div className="details-pane-header flex-between p-3 border-bottom">
          <div className="flex-align">
            {selectedNode.type === 'datacenter' && <Database size={20} className="text-primary mr-2" />}
            {selectedNode.type === 'folder' && <Folder size={20} className="text-warning mr-2" />}
            {selectedNode.type === 'device' && <Server size={20} className="text-success mr-2" />}
            <div>
              <h3 className="m-0 font-weight-bold">{selectedNode.name}</h3>
              <span className="text-muted text-capitalize" style={{ fontSize: '11px' }}>
                {selectedNode.type} Node #{selectedNode.numericId}
              </span>
            </div>
          </div>

          <div className="flex-align gap-2">
            {selectedNode.type === 'datacenter' && (
              <>
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => onManageUsers(selectedNode.numericId)}
                >
                  <Users size={14} /> Access Management
                </button>
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => onCopyPermissions(selectedNode.numericId)}
                >
                  <Copy size={14} /> Copy Permissions
                </button>
                <button
                  className="btn btn-primary btn-sm"
                  onClick={() => onAddFolder(selectedNode.numericId)}
                >
                  <Plus size={14} /> Add Folder
                </button>
              </>
            )}

            {selectedNode.type === 'folder' && (
              <>
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => onOpenImport(selectedNode.numericId)}
                >
                  <FileSpreadsheet size={14} /> Import Excel
                </button>
                <button
                  className="btn btn-primary btn-sm"
                  onClick={() => onAddDevice(selectedNode.numericId)}
                >
                  <Plus size={14} /> Add Host
                </button>
                <button
                  className="btn btn-danger btn-sm p-1"
                  onClick={() => onDeleteNode(selectedNode)}
                  title="Delete Folder"
                >
                  <Trash2 size={14} />
                </button>
              </>
            )}

            {selectedNode.type === 'device' && (
              <button
                className="btn btn-danger btn-sm p-1"
                onClick={() => onDeleteNode(selectedNode)}
                title="Delete Host"
              >
                <Trash2 size={14} />
              </button>
            )}
          </div>
        </div>

        {selectedNode.type === 'device' && (
          <div className="details-pane-tabs px-3 border-bottom flex-align gap-3">
            <button
              className={`subnav-btn ${activeTab === 'summary' ? 'active' : ''}`}
              onClick={() => setActiveTab('summary')}
            >
              Summary & Specifications
            </button>
            <button
              className={`subnav-btn ${activeTab === 'credentials' ? 'active' : ''}`}
              onClick={() => setActiveTab('credentials')}
            >
              Encrypted Passwords & Secrets ({credentials.length})
            </button>
          </div>
        )}

        <div className="details-pane-body p-4">
          {activeTab === 'summary' && (
            <div className="summary-tab-content">
              {selectedNode.type === 'datacenter' && (
                <div className="dc-summary-card">
                  <h4>Datacenter Overview</h4>
                  <p className="text-muted mb-4">{selectedNode.description || 'Primary Datacenter Location'}</p>

                  <div className="grid-3 mb-4">
                    <div className="spec-item">
                      <span className="spec-label">Datacenter ID</span>
                      <span className="spec-value">#{selectedNode.numericId}</span>
                    </div>

                    <div className="spec-item">
                      <span className="spec-label">Child Folders</span>
                      <span className="spec-value">{selectedNode.children?.length || 0} Folders</span>
                    </div>

                    <div className="spec-item">
                      <span className="spec-label">Security Protocol</span>
                      <span className="spec-value text-success font-weight-bold">AES-256-GCM</span>
                    </div>
                  </div>

                  <div className="card-body bg-dark border rounded p-3 mb-3">
                    <div className="flex-between mb-2">
                      <span className="font-weight-bold">Child Folders in {selectedNode.name}</span>
                      <button className="btn btn-primary btn-sm" onClick={() => onAddFolder(selectedNode.numericId)}>
                        <Plus size={14} /> Add Folder
                      </button>
                    </div>

                    <div className="table-responsive">
                      <table className="infravault-table">
                        <thead>
                          <tr>
                            <th>Folder Name</th>
                            <th>Host Count</th>
                            <th>Action</th>
                          </tr>
                        </thead>
                        <tbody>
                          {!selectedNode.children || selectedNode.children.length === 0 ? (
                            <tr>
                              <td colSpan={3} className="text-center text-muted py-3">
                                No folders created in this datacenter yet.
                              </td>
                            </tr>
                          ) : (
                            selectedNode.children.map((fNode) => (
                              <tr key={fNode.id}>
                                <td>
                                  <span className="font-weight-bold flex-align" onClick={() => onSelectNode(fNode)} style={{ cursor: 'pointer' }}>
                                    <Folder size={16} className="text-warning mr-2" /> {fNode.name}
                                  </span>
                                </td>
                                <td>{fNode.children?.length || 0} Hosts</td>
                                <td>
                                  <button className="btn btn-secondary btn-sm" onClick={() => onSelectNode(fNode)}>
                                    View Folder
                                  </button>
                                </td>
                              </tr>
                            ))
                          )}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              )}

              {selectedNode.type === 'folder' && (
                <div className="folder-detail-card">
                  <div className="flex-between mb-3">
                    <div>
                      <h4>Folder Infrastructure Hosts</h4>
                      <p className="text-muted">Managed hosts inside folder <strong>{selectedNode.name}</strong></p>
                    </div>
                    <div className="flex-align gap-2">
                      <button className="btn btn-primary btn-sm" onClick={() => onAddDevice(selectedNode.numericId)}>
                        <Plus size={14} /> Add Host
                      </button>
                      <button className="btn btn-secondary btn-sm" onClick={() => onOpenImport(selectedNode.numericId)}>
                        <FileSpreadsheet size={14} /> Import Excel
                      </button>
                    </div>
                  </div>

                  <div className="table-responsive">
                    <table className="infravault-table">
                      <thead>
                        <tr>
                          <th>Hostname</th>
                          <th>Model</th>
                          <th>Serial Number</th>
                          <th>Capacity</th>
                          <th>Service Tag</th>
                          <th>Support EOD</th>
                          <th>Console Port</th>
                          <th>Action</th>
                        </tr>
                      </thead>
                      <tbody>
                        {!selectedNode.children || selectedNode.children.length === 0 ? (
                          <tr>
                            <td colSpan={8} className="text-center text-muted py-4">
                              No hosts found in this folder. Click "Add Host" or "Import Excel".
                            </td>
                          </tr>
                        ) : (
                          selectedNode.children.map((dNode) => {
                            const dev = dNode.rawObject as Device;
                            return (
                              <tr key={dNode.id}>
                                <td>
                                  <span className="font-weight-bold text-primary flex-align" onClick={() => onSelectNode(dNode)} style={{ cursor: 'pointer' }}>
                                    <Server size={15} className="mr-2" /> {dNode.name}
                                  </span>
                                </td>
                                <td>{dev?.model || '-'}</td>
                                <td>{dev?.serialNumber || '-'}</td>
                                <td>{dev?.capacity || '-'}</td>
                                <td>{dev?.serviceTag || '-'}</td>
                                <td>
                                  {dev?.supportEndDate ? (
                                    <span className="badge badge-info flex-align">
                                      <Calendar size={12} className="mr-1" /> {dev.supportEndDate}
                                    </span>
                                  ) : (
                                    '-'
                                  )}
                                </td>
                                <td>{dev?.consolePort || '-'}</td>
                                <td>
                                  <button className="btn btn-secondary btn-sm" onClick={() => onSelectNode(dNode)}>
                                    View Details & Secrets
                                  </button>
                                </td>
                              </tr>
                            );
                          })
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {selectedNode.type === 'device' && (
                <div className="device-spec-card">
                  <div className="section-title text-primary mb-3">Hardware Specifications</div>
                  {(() => {
                    const dev = selectedNode.rawObject as Device;
                    return (
                      <div className="grid-3 mb-4">
                        <div className="spec-item">
                          <span className="spec-label"><Server size={14} /> Hostname</span>
                          <span className="spec-value">{dev?.hostname || selectedNode.name}</span>
                        </div>

                        <div className="spec-item">
                          <span className="spec-label"><Cpu size={14} /> Model</span>
                          <span className="spec-value">{dev?.model || 'N/A'}</span>
                        </div>

                        <div className="spec-item">
                          <span className="spec-label"><Tag size={14} /> Serial Number</span>
                          <span className="spec-value">{dev?.serialNumber || 'N/A'}</span>
                        </div>

                        <div className="spec-item">
                          <span className="spec-label"><HardDrive size={14} /> Capacity / Storage</span>
                          <span className="spec-value">{dev?.capacity || 'N/A'}</span>
                        </div>

                        <div className="spec-item">
                          <span className="spec-label"><Tag size={14} /> Service Tag</span>
                          <span className="spec-value">{dev?.serviceTag || 'N/A'}</span>
                        </div>

                        <div className="spec-item">
                          <span className="spec-label"><Calendar size={14} /> Support End Date (EOD)</span>
                          <span className="spec-value">{dev?.supportEndDate || 'N/A'}</span>
                        </div>
                      </div>
                    );
                  })()}

                  <div className="flex-between mt-4 mb-3">
                    <div className="section-title text-primary m-0 flex-align">
                      <Lock size={16} className="mr-2" />
                      <span>Encrypted Infrastructure Passwords</span>
                    </div>
                    <button
                      className="btn btn-primary btn-sm"
                      onClick={() => setShowAddSecret(!showAddSecret)}
                    >
                      <Plus size={14} /> Add Secret Field
                    </button>
                  </div>

                  {showAddSecret && (
                    <form onSubmit={handleAddSecretSubmit} className="card p-3 mb-4" style={{ backgroundColor: '#0b1120', border: '1px solid var(--primary)' }}>
                      <h4 style={{ fontSize: '13px', color: 'var(--primary)', marginBottom: '10px' }}>
                        Add New Encrypted Secret Field
                      </h4>
                      <div className="grid-3 mb-3">
                        <div className="form-group" style={{ marginBottom: 0 }}>
                          <label className="form-label">Secret Type / Label:*</label>
                          <input
                            type="text"
                            className="form-control"
                            placeholder="e.g. vCenter Service, VM Cleaner, Custom API"
                            value={newSecretType}
                            onChange={(e) => setNewSecretType(e.target.value)}
                            required
                          />
                        </div>

                        <div className="form-group" style={{ marginBottom: 0 }}>
                          <label className="form-label">Username:</label>
                          <input
                            type="text"
                            className="form-control"
                            placeholder="e.g. root or sysadmin"
                            value={newSecretUser}
                            onChange={(e) => setNewSecretUser(e.target.value)}
                          />
                        </div>

                        <div className="form-group" style={{ marginBottom: 0 }}>
                          <label className="form-label">Password / Secret:*</label>
                          <input
                            type="password"
                            className="form-control"
                            placeholder="Enter plaintext secret"
                            value={newSecretPass}
                            onChange={(e) => setNewSecretPass(e.target.value)}
                            required
                          />
                        </div>
                      </div>

                      <div className="flex-align gap-2 justify-content-end">
                        <button type="button" className="btn btn-secondary btn-sm" onClick={() => setShowAddSecret(false)}>
                          Cancel
                        </button>
                        <button type="submit" className="btn btn-primary btn-sm" disabled={savingSecret || !newSecretType.trim()}>
                          {savingSecret ? <span className="spinner"></span> : <><Key size={14} /> Save Secret</>}
                        </button>
                      </div>
                    </form>
                  )}

                  {loadingCreds ? (
                    <div className="text-center py-4"><span className="spinner"></span> Loading secrets...</div>
                  ) : credentials.length === 0 ? (
                    <div className="alert alert-info">No credentials configured for this host.</div>
                  ) : (
                    <div className="credentials-grid">
                      {credentials.map((cred) => (
                        <CredentialField
                          key={cred.id}
                          credentialId={cred.id}
                          type={cred.type}
                          username={cred.username}
                          datacenterId={selectedNode.datacenterId}
                        />
                      ))}
                      <div
                        className="credential-row-card"
                        style={{
                          border: '2px dashed var(--primary)',
                          backgroundColor: 'rgba(2, 132, 199, 0.08)',
                          display: 'flex',
                          flexDirection: 'column',
                          alignItems: 'center',
                          justifyContent: 'center',
                          minHeight: '140px',
                          cursor: 'pointer',
                          borderRadius: 'var(--radius-md)',
                        }}
                        onClick={() => setShowAddSecret(true)}
                      >
                        <Plus size={24} color="var(--primary)" />
                        <span style={{ fontWeight: 700, color: 'var(--primary)', marginTop: '8px', fontSize: '13px' }}>
                          + Add Credential Field
                        </span>
                        <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                          vCenter, VM Cleaner, or Custom Secret
                        </span>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {activeTab === 'credentials' && selectedNode.type === 'device' && (
            <div className="credentials-tab-content">
              <div className="flex-between mb-3">
                <div className="alert alert-warning m-0 flex-align flex-1 mr-3">
                  <ShieldCheck size={18} className="mr-2" />
                  <span>
                    All credentials below are encrypted with <strong>AES-256-GCM</strong>. Plaintext passwords are revealed temporarily and logged to the security audit trail.
                  </span>
                </div>
                <button
                  className="btn btn-primary btn-sm"
                  onClick={() => setShowAddSecret(!showAddSecret)}
                  style={{ whiteSpace: 'nowrap' }}
                >
                  <Plus size={14} /> Add Secret Field
                </button>
              </div>

              {showAddSecret && (
                <form onSubmit={handleAddSecretSubmit} className="card p-3 mb-4" style={{ backgroundColor: '#0b1120', border: '1px solid var(--primary)' }}>
                  <h4 style={{ fontSize: '13px', color: 'var(--primary)', marginBottom: '10px' }}>
                    Add New Encrypted Secret Field
                  </h4>
                  <div className="grid-3 mb-3">
                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <label className="form-label">Secret Type / Label:*</label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="e.g. vCenter Service, VM Cleaner, Custom API"
                        value={newSecretType}
                        onChange={(e) => setNewSecretType(e.target.value)}
                        required
                      />
                    </div>

                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <label className="form-label">Username:</label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="e.g. root or sysadmin"
                        value={newSecretUser}
                        onChange={(e) => setNewSecretUser(e.target.value)}
                      />
                    </div>

                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <label className="form-label">Password / Secret:*</label>
                      <input
                        type="password"
                        className="form-control"
                        placeholder="Enter plaintext secret"
                        value={newSecretPass}
                        onChange={(e) => setNewSecretPass(e.target.value)}
                        required
                      />
                    </div>
                  </div>

                  <div className="flex-align gap-2 justify-content-end">
                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => setShowAddSecret(false)}>
                      Cancel
                    </button>
                    <button type="submit" className="btn btn-primary btn-sm" disabled={savingSecret || !newSecretType.trim()}>
                      {savingSecret ? <span className="spinner"></span> : <><Key size={14} /> Save Secret</>}
                    </button>
                  </div>
                </form>
              )}

              {loadingCreds ? (
                <div className="text-center py-4"><span className="spinner"></span> Loading secrets...</div>
              ) : credentials.length === 0 ? (
                <div className="alert alert-info">No credentials recorded for this host.</div>
              ) : (
                <div className="credentials-grid">
                  {credentials.map((cred) => (
                    <CredentialField
                      key={cred.id}
                      credentialId={cred.id}
                      type={cred.type}
                      username={cred.username}
                      datacenterId={selectedNode.datacenterId}
                    />
                  ))}
                  
                  {/* Built-in Add Credential Field Card */}
                  <div
                    className="credential-row-card"
                    style={{
                      border: '2px dashed var(--primary)',
                      backgroundColor: 'rgba(2, 132, 199, 0.08)',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      justifyContent: 'center',
                      minHeight: '140px',
                      cursor: 'pointer',
                      borderRadius: 'var(--radius-md)',
                    }}
                    onClick={() => setShowAddSecret(true)}
                  >
                    <Plus size={24} color="var(--primary)" />
                    <span style={{ fontWeight: 700, color: 'var(--primary)', marginTop: '8px', fontSize: '13px' }}>
                      + Add Credential Field
                    </span>
                    <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                      vCenter, VM Cleaner, or Custom Secret
                    </span>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
