import React, { useEffect, useState } from 'react';
import { Users as UsersIcon, Shield, Search } from 'lucide-react';
import { User } from '../types';
import { usersApi } from '../services/api';

export const UsersPage: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    usersApi
      .getAllUsers()
      .then((data) => setUsers(data))
      .catch(() => setUsers([]))
      .finally(() => setLoading(false));
  }, []);

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

        <div className="navbar-search" style={{ maxWidth: '300px' }}>
          <Search className="search-icon" size={16} />
          <input
            type="text"
            placeholder="Search users..."
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
                  <th>ID</th>
                  <th>Username</th>
                  <th>Email</th>
                  <th>Global Role</th>
                  <th>Status</th>
                  <th>Created At</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={6} className="text-center py-4">
                      <span className="spinner"></span> Loading users...
                    </td>
                  </tr>
                ) : filteredUsers.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="text-center text-muted py-4">
                      No users found.
                    </td>
                  </tr>
                ) : (
                  filteredUsers.map((u) => (
                    <tr key={u.id}>
                      <td>#{u.id}</td>
                      <td><strong>{u.username}</strong></td>
                      <td>{u.email}</td>
                      <td>
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
                      </td>
                      <td>
                        <span className={`badge ${u.active ? 'badge-success' : 'badge-danger'}`}>
                          {u.active ? 'Active' : 'Disabled'}
                        </span>
                      </td>
                      <td>{u.createdAt ? new Date(u.createdAt).toLocaleDateString() : '-'}</td>
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
