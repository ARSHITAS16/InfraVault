import React, { useEffect, useState } from 'react';
import { Search, UserPlus, ShieldAlert, Trash2 } from 'lucide-react';
import { User } from '../types';
import { usersApi } from '../services/api';
import { CreateUserModal } from '../components/CreateUserModal';

interface UsersPageProps {
  currentUserRole?: string;
}

export const UsersPage: React.FC<UsersPageProps> = ({ currentUserRole = 'OPERATOR' }) => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [showAddUserModal, setShowAddUserModal] = useState(false);

  const isSuperAdmin = currentUserRole === 'SUPER_ADMIN';
  const currentUsername = localStorage.getItem('username') || '';

  const fetchUsers = () => {
    setLoading(true);
    setError('');
    usersApi
      .getAllUsers()
      .then((data) => setUsers(data))
      .catch((err: any) => {
        setUsers([]);
        setError(err.message || 'Failed to load users');
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleRoleChange = async (userId: number, newRole: string) => {
    try {
      setError('');
      await usersApi.updateUser(userId, { role: newRole });
      fetchUsers();
    } catch (err: any) {
      setError(err.message || 'Failed to update user role');
    }
  };

  const handleToggleActive = async (u: User) => {
    try {
      setError('');
      await usersApi.updateUser(u.id, { active: !u.active });
      fetchUsers();
    } catch (err: any) {
      setError(err.message || 'Failed to update user status');
    }
  };

  const handleDeleteUser = async (u: User) => {
    if (!window.confirm(`Delete user "${u.username}"? This cannot be undone.`)) return;
    try {
      setError('');
      await usersApi.deleteUser(u.id);
      fetchUsers();
    } catch (err: any) {
      setError(err.message || 'Failed to delete user');
    }
  };

  const filteredUsers = users.filter(
    (u) =>
      u.username.toLowerCase().includes(search.toLowerCase()) ||
      u.email.toLowerCase().includes(search.toLowerCase()) ||
      (u.role && u.role.toLowerCase().includes(search.toLowerCase()))
  );

  return (
    <div className="users-page-container">
      <div className="flex-between mb-4">
        <div>
          <h2>System Users & Global Roles</h2>
          <p className="text-muted">
            Manage user accounts, global authorization roles (SUPER_ADMIN, DC_ADMIN, OPERATOR, VIEWER), and active status.
          </p>
        </div>

        <div className="flex-align gap-2">
          <div className="navbar-search" style={{ maxWidth: '280px' }}>
            <Search className="search-icon" size={16} />
            <input
              type="text"
              placeholder="Search users..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="search-input"
            />
          </div>

          {isSuperAdmin && (
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => setShowAddUserModal(true)}
              style={{ whiteSpace: 'nowrap' }}
            >
              <UserPlus size={16} /> <span>+ Add User</span>
            </button>
          )}
        </div>
      </div>

      {error && (
        <div className="alert alert-danger mb-3">
          <ShieldAlert size={16} />
          <span>{error}</span>
        </div>
      )}

      <div className="card">
        <div className="card-body p-0">
          <div className="table-responsive">
            <table className="infravault-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Username</th>
                  <th>Email</th>
                  <th>Global Role</th>
                  <th>Status</th>
                  <th>Created At</th>
                  {isSuperAdmin && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={isSuperAdmin ? 7 : 6} className="text-center py-4">
                      <span className="spinner"></span> Loading users...
                    </td>
                  </tr>
                ) : filteredUsers.length === 0 ? (
                  <tr>
                    <td colSpan={isSuperAdmin ? 7 : 6} className="text-center text-muted py-4">
                      {error ? 'Unable to view system users.' : 'No users found.'}
                    </td>
                  </tr>
                ) : (
                  filteredUsers.map((u) => {
                    const isSelf = u.username === currentUsername;
                    return (
                      <tr key={u.id}>
                        <td>#{u.id}</td>
                        <td><strong>{u.username}</strong></td>
                        <td>{u.email}</td>
                        <td>
                          {isSuperAdmin && !isSelf ? (
                            <select
                              className="form-control form-control-sm"
                              value={u.role || 'OPERATOR'}
                              onChange={(e) => handleRoleChange(u.id, e.target.value)}
                              style={{ minWidth: '140px' }}
                            >
                              <option value="SUPER_ADMIN">SUPER_ADMIN</option>
                              <option value="DC_ADMIN">DC_ADMIN</option>
                              <option value="OPERATOR">OPERATOR</option>
                              <option value="VIEWER">VIEWER</option>
                            </select>
                          ) : (
                            <span
                              className={`badge ${
                                u.role === 'SUPER_ADMIN'
                                  ? 'badge-danger'
                                  : u.role === 'DC_ADMIN'
                                  ? 'badge-warning'
                                  : u.role === 'OPERATOR'
                                  ? 'badge-primary'
                                  : 'badge-secondary'
                              }`}
                            >
                              {u.role || 'OPERATOR'}
                            </span>
                          )}
                        </td>
                        <td>
                          <span className={`badge ${u.active ? 'badge-success' : 'badge-danger'}`}>
                            {u.active ? 'Active' : 'Disabled'}
                          </span>
                        </td>
                        <td>{u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '-'}</td>
                        {isSuperAdmin && (
                          <td>
                            <div className="flex-align gap-2">
                              <button
                                type="button"
                                className="btn btn-secondary btn-sm"
                                disabled={isSelf}
                                onClick={() => handleToggleActive(u)}
                                title={isSelf ? 'Cannot disable your own account' : (u.active ? 'Disable User' : 'Enable User')}
                              >
                                {u.active ? 'Disable' : 'Enable'}
                              </button>
                              <button
                                type="button"
                                className="btn btn-danger btn-sm p-1"
                                disabled={isSelf}
                                onClick={() => handleDeleteUser(u)}
                                title={isSelf ? 'Cannot delete your own account' : 'Delete User'}
                              >
                                <Trash2 size={14} />
                              </button>
                            </div>
                          </td>
                        )}
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {showAddUserModal && (
        <CreateUserModal
          onClose={() => setShowAddUserModal(false)}
          onCreated={fetchUsers}
        />
      )}
    </div>
  );
};
