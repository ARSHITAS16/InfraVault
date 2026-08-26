import React, { useEffect, useState } from 'react';
import { Users, UserPlus, Trash2, Copy, ShieldCheck, ShieldAlert } from 'lucide-react';
import { Datacenter, DatacenterUser, User } from '../types';
import { datacentersApi, usersApi } from '../services/api';

interface DatacenterUsersModalProps {
  datacenter: Datacenter;
  allDatacenters: Datacenter[];
  onClose: () => void;
  onOpenCopyModal: () => void;
}

export const DatacenterUsersModal: React.FC<DatacenterUsersModalProps> = ({
  datacenter,
  allDatacenters,
  onClose,
  onOpenCopyModal,
}) => {
  const [dcUsers, setDcUsers] = useState<DatacenterUser[]>([]);
  const [allUsers, setAllUsers] = useState<User[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | ''>('');
  const [permissionLevel, setPermissionLevel] = useState<string>('WRITE');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const loadData = async () => {
    try {
      setLoading(true);
      setError('');
      let assigned: DatacenterUser[] = [];
      let users: User[] = [];

      try {
        assigned = await datacentersApi.getUsers(datacenter.id);
      } catch {
        assigned = [];
      }

      try {
        users = await usersApi.getAllUsers();
      } catch {
        users = [];
      }

      setDcUsers(assigned);
      setAllUsers(users);

      const unassigned = users.filter(
        (u) => !assigned.some((du) => du.user?.id === u.id)
      );
      if (unassigned.length > 0) {
        setSelectedUserId(unassigned[0].id);
      } else {
        setSelectedUserId('');
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load user permissions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [datacenter.id]);

  const handleAddUser = async () => {
    if (!selectedUserId) return;

    try {
      setError('');
      await datacentersApi.addUser(
        datacenter.id,
        Number(selectedUserId),
        permissionLevel
      );
      loadData();
    } catch (err: any) {
      setError(err.message || 'Failed to assign user');
    }
  };

  const handleRemoveUser = async (userId: number) => {
    if (!window.confirm('Remove user access from this datacenter?')) return;

    try {
      setError('');
      await datacentersApi.removeUser(datacenter.id, userId);
      loadData();
    } catch (err: any) {
      setError(err.message || 'Failed to remove user permission');
    }
  };

  const unassignedUsers = allUsers.filter(
    (u) => !dcUsers.some((du) => du.user?.id === u.id)
  );

  return (
    <div className="modal-backdrop">
      <div className="modal-content modal-lg">
        <div className="modal-header">
          <div className="modal-title">
            <Users size={18} />
            <span>Datacenter Access Management — {datacenter.name}</span>
          </div>
          <button type="button" className="modal-close-btn" onClick={onClose}>
            &times;
          </button>
        </div>

        <div className="modal-body">
          <div className="flex-between mb-3">
            <h4 style={{ margin: 0, fontSize: '14px' }}>Assigned Users & Permissions</h4>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={onOpenCopyModal}
            >
              <Copy size={14} /> <span>Copy Permissions from another DC</span>
            </button>
          </div>

          {error && (
            <div className="alert alert-danger mb-3">
              <ShieldAlert size={16} />
              <span>{error}</span>
            </div>
          )}

          {/* Add User Bar */}
          <div className="card mb-4" style={{ backgroundColor: '#0f172a' }}>
            <div className="card-body">
              <div className="grid-3" style={{ alignItems: 'flex-end' }}>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label">Select User:</label>
                  <select
                    className="form-control"
                    value={selectedUserId}
                    onChange={(e) => setSelectedUserId(Number(e.target.value))}
                    disabled={unassignedUsers.length === 0}
                  >
                    {unassignedUsers.length === 0 ? (
                      <option value="">All users already assigned</option>
                    ) : (
                      unassignedUsers.map((u) => (
                        <option key={u.id} value={u.id}>
                          {u.username} ({u.email})
                        </option>
                      ))
                    )}
                  </select>
                </div>

                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label">Permission Level:</label>
                  <select
                    className="form-control"
                    value={permissionLevel}
                    onChange={(e) => setPermissionLevel(e.target.value)}
                  >
                    <option value="READ">READ (View Devices & Reveal Secrets)</option>
                    <option value="WRITE">WRITE (Operator - Create & Delete)</option>
                    <option value="ADMIN">ADMIN (Datacenter Administrator)</option>
                  </select>
                </div>

                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleAddUser}
                  disabled={!selectedUserId || unassignedUsers.length === 0}
                >
                  <UserPlus size={16} /> <span>Add User</span>
                </button>
              </div>
            </div>
          </div>

          {/* Users Table */}
          <div className="table-responsive">
            <table className="infravault-table">
              <thead>
                <tr>
                  <th>Username</th>
                  <th>Email</th>
                  <th>Global Role</th>
                  <th>DC Permission</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={5} className="text-center py-3">
                      <span className="spinner"></span> Loading assigned users...
                    </td>
                  </tr>
                ) : dcUsers.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="text-center text-muted py-3">
                      No users currently assigned to this Datacenter.
                    </td>
                  </tr>
                ) : (
                  dcUsers.map((du) => (
                    <tr key={du.id || du.user?.id}>
                      <td><strong>{du.user?.username || 'User'}</strong></td>
                      <td>{du.user?.email || '-'}</td>
                      <td>
                        <span className="badge badge-secondary">
                          {du.user?.role || 'OPERATOR'}
                        </span>
                      </td>
                      <td>
                        <span
                          className={`badge ${
                            du.permissionLevel === 'ADMIN'
                              ? 'badge-warning'
                              : du.permissionLevel === 'WRITE'
                              ? 'badge-primary'
                              : 'badge-info'
                          }`}
                        >
                          {du.permissionLevel}
                        </span>
                      </td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-danger btn-sm p-1"
                          onClick={() => du.user && handleRemoveUser(du.user.id)}
                          title="Revoke Datacenter Access"
                        >
                          <Trash2 size={14} />
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div className="modal-footer">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
