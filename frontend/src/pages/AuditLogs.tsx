import React, { useEffect, useState } from 'react';
import { FileText, Search, ShieldCheck } from 'lucide-react';
import { AuditLog } from '../types';
import { auditApi } from '../services/api';

export const AuditLogs: React.FC = () => {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    auditApi
      .getAuditLogs()
      .then((data) => setLogs(data))
      .catch(() => setLogs([]))
      .finally(() => setLoading(false));
  }, []);

  const formatUTC = (timestampStr: string) => {
    if (!timestampStr) return '-';
    try {
      let isoStr = timestampStr;
      if (!isoStr.endsWith('Z') && !isoStr.includes('+')) {
        isoStr += 'Z';
      }
      const date = new Date(isoStr);
      return (
        date.toLocaleString('en-US', {
          timeZone: 'UTC',
          year: 'numeric',
          month: 'short',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
          hour12: true,
        }) + ' UTC'
      );
    } catch {
      return timestampStr;
    }
  };

  const filteredLogs = logs.filter(
    (l) =>
      l.username.toLowerCase().includes(search.toLowerCase()) ||
      l.action.toLowerCase().includes(search.toLowerCase()) ||
      l.entityType.toLowerCase().includes(search.toLowerCase()) ||
      (l.metadata && l.metadata.toLowerCase().includes(search.toLowerCase()))
  );

  return (
    <div className="audit-page-container">
      <div className="flex-between mb-4">
        <div>
          <h2>Security & Operations Audit Trail</h2>
          <p className="text-muted">
            Immutable log of all login, secret reveal, device modifications, and datacenter permission copy events (Coordinated Universal Time - UTC).
          </p>
        </div>

        <div className="navbar-search" style={{ maxWidth: '300px' }}>
          <Search className="search-icon" size={16} />
          <input
            type="text"
            placeholder="Filter audit logs..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="search-input"
          />
        </div>
      </div>

      <div className="card">
        <div className="card-body p-0">
          <div className="table-responsive">
            <table className="infravault-table">
              <thead>
                <tr>
                  <th>Timestamp (UTC)</th>
                  <th>User</th>
                  <th>Action</th>
                  <th>Entity Type</th>
                  <th>Entity ID</th>
                  <th>Datacenter ID</th>
                  <th>Details / Metadata</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={7} className="text-center py-4">
                      <span className="spinner"></span> Loading audit logs...
                    </td>
                  </tr>
                ) : filteredLogs.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="text-center text-muted py-4">
                      No audit records matching your search.
                    </td>
                  </tr>
                ) : (
                  filteredLogs.map((log) => (
                    <tr key={log.id}>
                      <td><span className="badge badge-secondary">{formatUTC(log.timestamp)}</span></td>
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
                      <td>{log.datacenterId || '-'}</td>
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
