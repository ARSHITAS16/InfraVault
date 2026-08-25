import React, { useState, useEffect } from 'react';
import { Users, UserPlus, Trash2, Copy, ShieldAlert } from 'lucide-react';
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
      const [assigned, users] = await Promise.all([
        datacentersApi.getUsers(datacenter.id),
        usersApi.getAllUsers(),
      ]);
      setDcUsers(assigned);
      setAllUsers(users);

      const unassigned = users.filter(
        (u) => !assigned.some((du) => du.user.id === u.id)
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
    try {
      setError('');
      await datacentersApi.removeUser(datacenter.id, userId);
      loadData();
    } catch (err: any) {
      setError(err.message || 'Failed to remove user');
    }
  };

  const unassignedUsers = allUsers.filter(
    (u) => !dcUsers.some((du) => du.user.id === u.id)
  );

  return (
    <div className="modal-backdrop">
      <div className="modal-content modal-lg">
        <div className="modal-header">
          <div className="modal-title">
            <Users size={18} />
            <span>Datacenter Access Management — {datacenter.name}</span>
          </div>
          <button className="modal-close-btn" onClick={onClose}>
            &times;
          </button>
        </div>

        <div className="modal-body">
          <div className="flex-between mb-3">
            <span className="section-subtitle">Assigned Users & Permissions</span>
            {allDatacenters.length > 1 && (
              <button
                className="btn btn-secondary btn-sm"
                onClick={onOpenCopyModal}
              >
                <Copy size={14} />
                <span>Copy Permissions from another DC</span>
              </button>
            )}
          </div>

          {error && (
            <div className="alert alert-danger">
              <ShieldAlert size={16} />
              <span>{error}</span>
            </div>
          )}

          {/* Add User Bar */}
          <div className="add-user-bar">
            <div className="form-group flex-1">
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

            <div className="form-group flex-initial">
              <label className="form-label">Permission Level:</label>
              <select
                className="form-control"
                value={permissionLevel}
                onChange={(e) => setPermissionLevel(e.target.value)}
              >
                <option value="READ">READ (Viewer)</option>
                <option value="WRITE">WRITE (Operator)</option>
                <option value="ADMIN">ADMIN (Full Access)</option>
              </select>
            </div>

            <button
              className="btn btn-primary btn-add-user"
              onClick={handleAddUser}
              disabled={!selectedUserId}
            >
              <UserPlus size={15} />
              <span>Add User</span>
            </button>
          </div>

          {/* Assigned Users Table */}
          <div className="table-responsive mt-3">
            <table className="infravault-table">
              <thead>
                <tr>
                  <th>Username</th>
                  <th>Email</th>
                  <th>Global Role</th>
                  <th>DC Permission</th>
                  <th style={{ width: '80px' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {dcUsers.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="text-center text-muted py-3">
                      No users currently assigned to this Datacenter.
                    </td>
                  </tr>
                ) : (
                  dcUsers.map((du) => (
                    <tr key={du.id}>
                      <td><strong>{du.user.username}</strong></td>
                      <td>{du.user.email}</td>
                      <td>
                        <span className="badge badge-info">{du.user.role || 'OPERATOR'}</span>
                      </td>
                      <td>
                        <span
                          className={`badge ${
                            du.permissionLevel === 'ADMIN'
                              ? 'badge-danger'
                              : du.permissionLevel === 'WRITE'
                              ? 'badge-primary'
                              : 'badge-secondary'
                          }`}
                        >
                          {du.permissionLevel}
                        </span>
                      </td>
                      <td>
                        <button
                          className="btn btn-icon btn-danger-icon"
                          onClick={() => handleRemoveUser(du.user.id)}
                          title="Remove Access"
                        >
                          <Trash2 size={15} />
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
          <button className="btn btn-secondary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
