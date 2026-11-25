import React, { useEffect, useState, useCallback, useMemo } from 'react';
import './App.css';
import { Datacenter, Folder, Device } from './types';
import { datacentersApi, foldersApi, devicesApi } from './services/api';
import { Navbar } from './components/Navbar';
import { TreeDataNode } from './components/SidebarTree';
import { Login } from './pages/Login';
import { Dashboard } from './pages/Dashboard';
import { InventoryView } from './pages/InventoryView';
import { AuditLogs } from './pages/AuditLogs';
import { UsersPage } from './pages/Users';

import { CreateDatacenterModal } from './components/CreateDatacenterModal';
import { CreateFolderModal } from './components/CreateFolderModal';
import { CreateDeviceModal } from './components/CreateDeviceModal';
import { DatacenterUsersModal } from './components/DatacenterUsersModal';
import { CopyPermissionsModal } from './components/CopyPermissionsModal';
import { ExcelImportModal } from './components/ExcelImportModal';

export default function App() {
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'));
  const [username, setUsername] = useState<string>(localStorage.getItem('username') || '');
  const [role, setRole] = useState<string>(localStorage.getItem('role') || 'OPERATOR');
  const [sessionNotice, setSessionNotice] = useState<string>('');

  const [activeView, setActiveView] = useState<'dashboard' | 'inventory' | 'audit' | 'users'>('inventory');
  const [searchQuery, setSearchQuery] = useState('');

  const [datacenters, setDatacenters] = useState<Datacenter[]>([]);
  const [folders, setFolders] = useState<Folder[]>([]);
  const [treeData, setTreeData] = useState<TreeDataNode[]>([]);
  const [selectedNode, setSelectedNode] = useState<TreeDataNode | null>(null);
  const [loadingInventory, setLoadingInventory] = useState(false);
  const [inventoryError, setInventoryError] = useState('');

  // Modals
  const [modalState, setModalState] = useState<{
    type: 'createDc' | 'createFolder' | 'createDevice' | 'dcUsers' | 'copyPerms' | 'excelImport' | null;
    dcId?: number;
    dcName?: string;
    folderId?: number;
    folderName?: string;
    targetDc?: Datacenter;
  }>({ type: null });

  const isAdmin = role === 'SUPER_ADMIN' || role === 'DC_ADMIN';
  const canWrite = role !== 'VIEWER';

  const findNodeById = (nodes: TreeDataNode[], id: string): TreeDataNode | null => {
    for (const n of nodes) {
      if (n.id === id) return n;
      if (n.children) {
        const res = findNodeById(n.children, id);
        if (res) return res;
      }
    }
    return null;
  };

  const handleLogout = useCallback((notice?: string) => {
    localStorage.clear();
    setToken(null);
    setUsername('');
    setRole('OPERATOR');
    setDatacenters([]);
    setFolders([]);
    setTreeData([]);
    setSelectedNode(null);
    setSearchQuery('');
    setInventoryError('');
    if (notice) {
      setSessionNotice(notice);
    }
  }, []);

  useEffect(() => {
    const onUnauthorized = () => {
      handleLogout('Your session expired or is invalid. Please sign in again.');
    };
    window.addEventListener('infravault:unauthorized', onUnauthorized);
    return () => window.removeEventListener('infravault:unauthorized', onUnauthorized);
  }, [handleLogout]);

  const loadFullInventory = useCallback(async () => {
    if (!token) return;

    try {
      setLoadingInventory(true);
      setInventoryError('');
      const dcs = await datacentersApi.getDatacenters();
      setDatacenters(dcs);

      const allFetchedFolders: Folder[] = [];

      const treeNodes: TreeDataNode[] = await Promise.all(
        dcs.map(async (dc) => {
          let dcFolders: Folder[] = [];
          try {
            dcFolders = await datacentersApi.getFolders(dc.id);
          } catch {
            dcFolders = [];
          }
          allFetchedFolders.push(...dcFolders);

          const folderNodes: TreeDataNode[] = await Promise.all(
            dcFolders.map(async (f) => {
              let devices: Device[] = [];
              try {
                devices = await foldersApi.getDevices(f.id);
              } catch {
                devices = [];
              }

              const deviceNodes: TreeDataNode[] = devices.map((d) => ({
                type: 'device',
                id: `device-${d.id}`,
                numericId: d.id,
                name: d.hostname,
                datacenterId: dc.id,
                rawObject: d,
              }));

              return {
                type: 'folder',
                id: `folder-${f.id}`,
                numericId: f.id,
                name: f.name,
                datacenterId: dc.id,
                children: deviceNodes,
                rawObject: f,
              };
            })
          );

          return {
            type: 'datacenter',
            id: `dc-${dc.id}`,
            numericId: dc.id,
            name: dc.name,
            datacenterId: dc.id,
            description: dc.description,
            children: folderNodes,
            rawObject: dc,
          };
        })
      );

      setFolders(allFetchedFolders);
      setTreeData(treeNodes);

      if (treeNodes.length > 0) {
        setSelectedNode((prev) => {
          if (!prev) return treeNodes[0];
          const found = findNodeById(treeNodes, prev.id);
          return found || treeNodes[0];
        });
      } else {
        setSelectedNode(null);
      }
    } catch (err: any) {
      console.error('Failed to load inventory:', err);
      setInventoryError(err.message || 'Failed to load infrastructure inventory');
    } finally {
      setLoadingInventory(false);
    }
  }, [token]);

  useEffect(() => {
    if (token) {
      loadFullInventory();
    }
  }, [token, loadFullInventory]);

  const filteredTreeData = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return treeData;

    const filterNodes = (nodes: TreeDataNode[]): TreeDataNode[] => {
      const results: TreeDataNode[] = [];
      for (const node of nodes) {
        const dev = node.type === 'device' ? (node.rawObject as Device | undefined) : undefined;
        const selfMatches =
          node.name.toLowerCase().includes(q) ||
          (node.description && node.description.toLowerCase().includes(q)) ||
          (dev?.model && dev.model.toLowerCase().includes(q)) ||
          (dev?.serialNumber && dev.serialNumber.toLowerCase().includes(q)) ||
          (dev?.serviceTag && dev.serviceTag.toLowerCase().includes(q)) ||
          (dev?.consolePort && dev.consolePort.toLowerCase().includes(q));

        if (selfMatches) {
          results.push(node);
        } else if (node.children && node.children.length > 0) {
          const matchingChildren = filterNodes(node.children);
          if (matchingChildren.length > 0) {
            results.push({ ...node, children: matchingChildren });
          }
        }
      }
      return results;
    };

    return filterNodes(treeData);
  }, [treeData, searchQuery]);

  const handleLoginSuccess = (newToken: string, newUsername: string, newUserId: number, newRole: string) => {
    localStorage.setItem('token', newToken);
    localStorage.setItem('username', newUsername);
    localStorage.setItem('userId', newUserId.toString());
    localStorage.setItem('role', newRole);

    setSessionNotice('');
    setToken(newToken);
    setUsername(newUsername);
    setRole(newRole);
    setActiveView('inventory');
  };

  const handleDeleteNode = async (node: TreeDataNode) => {
    if (!canWrite) {
      alert('Permission denied: VIEWER accounts have read-only access.');
      return;
    }

    if (node.type === 'datacenter' && node.children && node.children.length > 0) {
      alert(`Cannot delete Datacenter "${node.name}" because it contains ${node.children.length} folder(s). Please delete all child folders first.`);
      return;
    }

    if (node.type === 'folder' && node.children && node.children.length > 0) {
      alert(`Cannot delete Folder "${node.name}" because it contains ${node.children.length} host(s). Please delete all child hosts first.`);
      return;
    }

    if (!window.confirm(`Are you sure you want to delete ${node.type} "${node.name}"?`)) {
      return;
    }

    try {
      if (node.type === 'datacenter') {
        await datacentersApi.deleteDatacenter(node.numericId);
      } else if (node.type === 'folder') {
        await foldersApi.deleteFolder(node.numericId, false);
      } else if (node.type === 'device') {
        await devicesApi.deleteDevice(node.numericId);
      }
      loadFullInventory();
      setSelectedNode(null);
    } catch (err: any) {
      alert(err.message || 'Failed to delete node');
    }
  };

  if (!token) {
    return <Login onLoginSuccess={handleLoginSuccess} sessionNotice={sessionNotice} />;
  }

  if (loadingInventory && treeData.length === 0) {
    return (
      <div className="infravault-app-layout flex-align justify-content-center text-center" style={{ height: '100vh', backgroundColor: '#0f172a' }}>
        <div>
          <div className="spinner mb-3" style={{ width: '32px', height: '32px', margin: '0 auto' }}></div>
          <h4 style={{ color: 'var(--text-primary)', fontWeight: 600 }}>Loading Infrastructure Vault...</h4>
        </div>
      </div>
    );
  }

  let totalDevices = 0;
  treeData.forEach((dc) => {
    dc.children?.forEach((f) => {
      totalDevices += f.children?.length || 0;
    });
  });

  return (
    <div className="infravault-app-layout">
      <Navbar
        username={username}
        role={role}
        searchQuery={searchQuery}
        onSearchChange={(q) => {
          setSearchQuery(q);
          if (q.trim() && activeView !== 'inventory') {
            setActiveView('inventory');
          }
        }}
        onOpenCreateDatacenter={() => setModalState({ type: 'createDc' })}
        onOpenImport={() => setModalState({ type: 'excelImport' })}
        onOpenAuditLogs={() => setActiveView('audit')}
        onOpenUsers={() => setActiveView('users')}
        onLogout={() => handleLogout()}
      />

      <div className="infravault-subnav">
        <button
          className={`subnav-btn ${activeView === 'inventory' ? 'active' : ''}`}
          onClick={() => setActiveView('inventory')}
        >
          Infrastructure Inventory
        </button>

        <button
          className={`subnav-btn ${activeView === 'dashboard' ? 'active' : ''}`}
          onClick={() => setActiveView('dashboard')}
        >
          Dashboard Summary
        </button>

        {isAdmin && (
          <button
            className={`subnav-btn ${activeView === 'audit' ? 'active' : ''}`}
            onClick={() => setActiveView('audit')}
          >
            Audit Logs
          </button>
        )}

        {isAdmin && (
          <button
            className={`subnav-btn ${activeView === 'users' ? 'active' : ''}`}
            onClick={() => setActiveView('users')}
          >
            Users & Access Control
          </button>
        )}
      </div>

      {inventoryError && (
        <div className="alert alert-danger m-3 flex-between">
          <span>{inventoryError}</span>
          <button type="button" className="btn btn-secondary btn-sm" onClick={loadFullInventory}>
            Retry
          </button>
        </div>
      )}

      <main className="infravault-body">
        {activeView === 'dashboard' && (
          <Dashboard
            datacenters={datacenters}
            totalFolders={folders.length}
            totalDevices={totalDevices}
            isAdmin={isAdmin}
            onNavigateToDatacenter={(dcId) => {
              const node = treeData.find((n) => n.numericId === dcId);
              if (node) setSelectedNode(node);
              setActiveView('inventory');
            }}
          />
        )}

        {activeView === 'inventory' && (
          <InventoryView
            treeData={filteredTreeData}
            selectedNode={selectedNode}
            allDatacenters={datacenters}
            allFolders={folders}
            userRole={role}
            onSelectNode={setSelectedNode}
            onAddFolder={(dcId) => {
              const dc = datacenters.find((d) => d.id === dcId);
              setModalState({ type: 'createFolder', dcId, dcName: dc?.name });
            }}
            onAddDevice={(folderId) => {
              const f = folders.find((fol) => fol.id === folderId);
              setModalState({ type: 'createDevice', folderId, folderName: f?.name });
            }}
            onManageUsers={(dcId) => {
              const dc = datacenters.find((d) => d.id === dcId);
              if (dc) setModalState({ type: 'dcUsers', targetDc: dc });
            }}
            onCopyPermissions={(dcId) => {
              const dc = datacenters.find((d) => d.id === dcId);
              if (dc) setModalState({ type: 'copyPerms', targetDc: dc });
            }}
            onOpenImport={(folderId) => setModalState({ type: 'excelImport', folderId })}
            onDeleteNode={handleDeleteNode}
          />
        )}

        {activeView === 'audit' && <AuditLogs />}
        {activeView === 'users' && <UsersPage currentUserRole={role} />}
      </main>

      {/* Render Active Modals */}
      {modalState.type === 'createDc' && (
        <CreateDatacenterModal
          onClose={() => setModalState({ type: null })}
          onSuccess={loadFullInventory}
        />
      )}

      {modalState.type === 'createFolder' && modalState.dcId && (
        <CreateFolderModal
          datacenterId={modalState.dcId}
          datacenterName={modalState.dcName || 'Datacenter'}
          onClose={() => setModalState({ type: null })}
          onSuccess={loadFullInventory}
        />
      )}

      {modalState.type === 'createDevice' && modalState.folderId && (
        <CreateDeviceModal
          folderId={modalState.folderId}
          folderName={modalState.folderName || 'Folder'}
          onClose={() => setModalState({ type: null })}
          onSuccess={loadFullInventory}
        />
      )}

      {modalState.type === 'dcUsers' && modalState.targetDc && (
        <DatacenterUsersModal
          datacenter={modalState.targetDc}
          allDatacenters={datacenters}
          onClose={() => setModalState({ type: null })}
          onOpenCopyModal={() =>
            setModalState({ type: 'copyPerms', targetDc: modalState.targetDc })
          }
        />
      )}

      {modalState.type === 'copyPerms' && modalState.targetDc && (
        <CopyPermissionsModal
          targetDatacenter={modalState.targetDc}
          allDatacenters={datacenters}
          onClose={() => setModalState({ type: null })}
          onSuccess={loadFullInventory}
        />
      )}

      {modalState.type === 'excelImport' && (
        <ExcelImportModal
          folders={folders}
          defaultFolderId={modalState.folderId}
          onClose={() => setModalState({ type: null })}
          onSuccess={loadFullInventory}
        />
      )}
    </div>
  );
}
