import React from 'react';
import {
  Server,
  Shield,
  Search,
  Plus,
  LogOut,
  User as UserIcon,
  Upload,
  Layers,
  FileText,
} from 'lucide-react';
import ringCentralLogo from '../assets/ringcentral-logo.png';

interface NavbarProps {
  username: string;
  role: string;
  searchQuery: string;
  onSearchChange: (q: string) => void;
  onOpenCreateDatacenter: () => void;
  onOpenImport: () => void;
  onOpenAuditLogs: () => void;
  onOpenUsers: () => void;
  onLogout: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  username,
  role,
  searchQuery,
  onSearchChange,
  onOpenCreateDatacenter,
  onOpenImport,
  onOpenAuditLogs,
  onOpenUsers,
  onLogout,
}) => {
  return (
    <header className="infravault-navbar">
      <div className="navbar-brand">
        <img
          src={ringCentralLogo}
          alt="RingCentral Logo"
          className="brand-logo"
        />
        <div className="brand-divider"></div>
        <div className="brand-title">
          <span className="app-title">InfraVault</span>
          <span className="app-subtitle">Password & Infrastructure Inventory</span>
        </div>
      </div>

      <div className="navbar-search">
        <Search className="search-icon" size={16} />
        <input
          type="text"
          placeholder="Search datacenters, folders, hosts, serial tags..."
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          className="search-input"
        />
      </div>

      <div className="navbar-actions">
        <button
          className="btn btn-secondary btn-sm"
          onClick={onOpenCreateDatacenter}
          title="Create New Datacenter"
        >
          <Plus size={15} />
          <span>New Datacenter</span>
        </button>

        <button
          className="btn btn-secondary btn-sm"
          onClick={onOpenImport}
          title="Import Devices from Excel"
        >
          <Upload size={15} />
          <span>Excel Import</span>
        </button>

        <button
          className="btn btn-secondary btn-sm"
          onClick={onOpenAuditLogs}
          title="View Audit Logs"
        >
          <FileText size={15} />
          <span>Audit Logs</span>
        </button>

        <button
          className="btn btn-secondary btn-sm"
          onClick={onOpenUsers}
          title="Manage Users & Access"
        >
          <UserIcon size={15} />
          <span>Users</span>
        </button>

        <div className="user-profile">
          <div className="user-avatar">
            {username.substring(0, 2).toUpperCase()}
          </div>
          <div className="user-info">
            <span className="user-name">{username}</span>
            <span className="user-role-badge">{role || 'OPERATOR'}</span>
          </div>
        </div>

        <button
          className="btn btn-icon btn-logout"
          onClick={onLogout}
          title="Logout"
        >
          <LogOut size={18} />
        </button>
      </div>
    </header>
  );
};
