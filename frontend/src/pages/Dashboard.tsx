import React, { useEffect, useState } from 'react';
import { Database, Folder, Server, ShieldCheck, AlertTriangle, FileText } from 'lucide-react';
import { Datacenter, AuditLog } from '../types';
import { auditApi } from '../services/api';

interface DashboardProps {
  datacenters: Datacenter[];
  totalFolders: number;
  totalDevices: number;
  onNavigateToDatacenter: (dcId: number) => void;
}

export const Dashboard: React.FC<DashboardProps> = ({
  datacenters,
  totalFolders,
  totalDevices,
  onNavigateToDatacenter,
}) => {
  const [logs, setLogs] = useState<AuditLog[]>([]);

  useEffect(() => {
    auditApi
      .getAuditLogs()
      .then((data) => setLogs(data.slice(0, 10)))
      .catch(() => setLogs([]));
  }, []);

  return (
    <div className="dashboard-container">
      <div className="dashboard-header mb-4">
        <h2>Infrastructure Summary</h2>
        <p className="text-muted">
          Centralized overview of managed datacenters, device groups, hardware inventory, and security audit logs.
        </p>
      </div>

      {/* Metric Cards */}
      <div className="grid-4 mb-4">
        <div className="metric-card bg-primary-light">
          <div className="metric-icon">
            <Database size={24} />
          </div>
          <div className="metric-info">
            <span className="metric-value">{datacenters.length}</span>
            <span className="metric-label">Active Datacenters</span>
          </div>
        </div>

        <div className="metric-card bg-info-light">
          <div className="metric-icon">
            <Folder size={24} />
          </div>
          <div className="metric-info">
            <span className="metric-value">{totalFolders}</span>
            <span className="metric-label">Device Folders</span>
          </div>
        </div>

        <div className="metric-card bg-success-light">
          <div className="metric-icon">
            <Server size={24} />
          </div>
          <div className="metric-info">
            <span className="metric-value">{totalDevices}</span>
            <span className="metric-label">Managed Hosts</span>
          </div>
        </div>

        <div className="metric-card bg-warning-light">
          <div className="metric-icon">
            <ShieldCheck size={24} />
          </div>
          <div className="metric-info">
            <span className="metric-value">AES-256</span>
            <span className="metric-label">GCM Encrypted</span>
          </div>
        </div>
      </div>

      {/* Datacenters Grid */}
      <div className="section-title text-primary mb-3">Accessible Datacenters</div>
      <div className="grid-3 mb-4">
        {datacenters.map((dc) => (
          <div
            key={dc.id}
            className="dc-summary-card"
            onClick={() => onNavigateToDatacenter(dc.id)}
          >
            <div className="dc-card-header">
              <Database size={20} className="text-primary" />
              <span className="dc-name">{dc.name}</span>
            </div>
            <p className="dc-desc">{dc.description || 'Datacenter'}</p>
            <div className="dc-card-footer">
              <span className="btn-link">View Inventory &rarr;</span>
            </div>
          </div>
        ))}
      </div>

      {/* Recent Audit Logs */}
      <div className="card mt-4">
        <div className="card-header flex-between">
          <div className="flex-align">
            <FileText size={18} className="mr-2" />
            <span className="font-weight-bold">Recent Security & Operations Audit Logs</span>
          </div>
        </div>
        <div className="card-body p-0">
          <div className="table-responsive">
            <table className="infravault-table">
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>User</th>
                  <th>Action</th>
                  <th>Entity Type</th>
                  <th>Entity ID</th>
                  <th>Metadata</th>
                </tr>
              </thead>
              <tbody>
                {logs.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="text-center text-muted py-3">
                      No audit activity recorded yet.
                    </td>
                  </tr>
                ) : (
                  logs.map((log) => (
                    <tr key={log.id}>
                      <td>{new Date(log.timestamp).toLocaleString()}</td>
                      <td><strong>{log.username}</strong></td>
                      <td>
                        <span
                          className={`badge ${
                            log.action === 'REVEAL'
                              ? 'badge-warning'
                              : log.action.startsWith('CREATE')
                              ? 'badge-success'
                              : log.action.startsWith('DELETE')
                              ? 'badge-danger'
                              : 'badge-info'
                          }`}
                        >
                          {log.action}
                        </span>
                      </td>
                      <td>{log.entityType}</td>
                      <td>{log.entityId || '-'}</td>
                      <td className="text-muted">{log.metadata || '-'}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};
