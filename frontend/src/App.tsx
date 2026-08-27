import React, { useEffect, useState, useCallback } from 'react';
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
  const [userId, setUserId] = useState<number>(Number(localStorage.getItem('userId')) || 0);
  const [role, setRole] = useState<string>(localStorage.getItem('role') || 'OPERATOR');

  const [activeView, setActiveView] = useState<'dashboard' | 'inventory' | 'audit' | 'users'>('inventory');
  const [searchQuery, setSearchQuery] = useState('');

  const [datacenters, setDatacenters] = useState<Datacenter[]>([]);
  const [folders, setFolders] = useState<Folder[]>([]);
  const [treeData, setTreeData] = useState<TreeDataNode[]>([]);
  const [selectedNode, setSelectedNode] = useState<TreeDataNode | null>(null);
  const [loadingInventory, setLoadingInventory] = useState(false);

  // Modals
  const [modalState, setModalState] = useState<{
    type: 'createDc' | 'createFolder' | 'createDevice' | 'dcUsers' | 'copyPerms' | 'excelImport' | null;
    dcId?: number;
    dcName?: string;
    folderId?: number;
    folderName?: string;
    targetDc?: Datacenter;
  }>({ type: null });

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

  // Load cached tree on initial mount for instant load (0ms wait)
  useEffect(() => {
    try {
      const cached = localStorage.getItem('cached_tree_data');
      if (cached) {
        const parsed = JSON.parse(cached);
        if (Array.isArray(parsed) && parsed.length > 0) {
          setTreeData(parsed);
          setSelectedNode(parsed[0]);
          setLoadingInventory(false);
        }
      }
    } catch (e) {
      // Ignore cache parse errors
    }
  }, []);

  const loadFullInventory = useCallback(async () => {
    if (!token) return;

    try {
      // Try single-batch tree API for maximum speed (1 roundtrip instead of N+1 calls)
      const rawTree = await datacentersApi.getTree();

      const allFetchedDcs: Datacenter[] = [];
      const allFetchedFolders: Folder[] = [];

      const treeNodes: TreeDataNode[] = rawTree.map((dc: any) => {
        const dcObj: Datacenter = { id: dc.id, name: dc.name, description: dc.description };
        allFetchedDcs.push(dcObj);

        const folderNodes: TreeDataNode[] = (dc.folders || []).map((f: any) => {
          const folderObj: Folder = { id: f.id, name: f.name, datacenter: dcObj };
          allFetchedFolders.push(folderObj);

          const deviceNodes: TreeDataNode[] = (f.devices || []).map((d: any) => ({
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
            rawObject: folderObj,
          };
        });

        return {
          type: 'datacenter',
          id: `dc-${dc.id}`,
          numericId: dc.id,
          name: dc.name,
          datacenterId: dc.id,
          description: dc.description,
          children: folderNodes,
          rawObject: dcObj,
        };
      });

      setDatacenters(allFetchedDcs);
      setFolders(allFetchedFolders);
      setTreeData(treeNodes);

      // Save to localStorage cache for instant zero-wait page reloads
      localStorage.setItem('cached_tree_data', JSON.stringify(treeNodes));

      if (treeNodes.length > 0) {
        setSelectedNode((prev) => {
          if (!prev) return treeNodes[0];
          const found = findNodeById(treeNodes, prev.id);
          return found || treeNodes[0];
        });
      }
    } catch (err: any) {
      console.warn('Single-batch tree fetch failed:', err);
    } finally {
      setLoadingInventory(false);
    }
  }, [token]);

  useEffect(() => {
    if (token) {
      loadFullInventory();
    }
  }, [token, loadFullInventory]);

  const handleLoginSuccess = (newToken: string, newUsername: string, newUserId: number, newRole: string) => {
    localStorage.setItem('token', newToken);
    localStorage.setItem('username', newUsername);
    localStorage.setItem('userId', newUserId.toString());
    localStorage.setItem('role', newRole);

    setToken(newToken);
    setUsername(newUsername);
    setUserId(newUserId);
    setRole(newRole);
    setActiveView('inventory');
  };

  const handleLogout = () => {
    localStorage.clear();
    setToken(null);
    setUsername('');
    setUserId(0);
    setRole('OPERATOR');
  };

  const handleDeleteNode = async (node: TreeDataNode) => {
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
    return <Login onLoginSuccess={handleLoginSuccess} />;
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
        onSearchChange={setSearchQuery}
        onOpenCreateDatacenter={() => setModalState({ type: 'createDc' })}
        onOpenImport={() => setModalState({ type: 'excelImport' })}
        onOpenAuditLogs={() => setActiveView('audit')}
        onOpenUsers={() => setActiveView('users')}
        onLogout={handleLogout}
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

        <button
          className={`subnav-btn ${activeView === 'audit' ? 'active' : ''}`}
          onClick={() => setActiveView('audit')}
        >
          Audit Logs
        </button>

        <button
          className={`subnav-btn ${activeView === 'users' ? 'active' : ''}`}
          onClick={() => setActiveView('users')}
        >
          Users & Access Control
        </button>
      </div>

      <main className="infravault-body">
        {activeView === 'dashboard' && (
          <Dashboard
            datacenters={datacenters}
            totalFolders={folders.length}
            totalDevices={totalDevices}
            onNavigateToDatacenter={(dcId) => {
              const node = treeData.find((n) => n.numericId === dcId);
              if (node) setSelectedNode(node);
              setActiveView('inventory');
            }}
          />
        )}

        {activeView === 'inventory' && (
          <InventoryView
            treeData={treeData}
            selectedNode={selectedNode}
            allDatacenters={datacenters}
            allFolders={folders}
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
        {activeView === 'users' && <UsersPage />}
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
