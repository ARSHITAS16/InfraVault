import React, { useState } from 'react';
import {
  ChevronDown,
  ChevronRight,
  Database,
  Folder as FolderIcon,
  Server,
  Plus,
  Trash2,
  Users,
  Copy,
  FolderPlus,
  Monitor,
} from 'lucide-react';
import { Datacenter, Folder, Device } from '../types';

export interface TreeDataNode {
  type: 'datacenter' | 'folder' | 'device';
  id: string; // e.g. "dc-1", "folder-5", "device-10"
  numericId: number;
  name: string;
  datacenterId: number; // parent DC
  description?: string;
  children?: TreeDataNode[];
  rawObject?: Datacenter | Folder | Device;
}

interface SidebarTreeProps {
  treeData: TreeDataNode[];
  selectedNodeId: string | null;
  onSelectNode: (node: TreeDataNode) => void;
  onAddFolder: (dcId: number) => void;
  onAddDevice: (folderId: number) => void;
  onManageUsers: (dcId: number) => void;
  onCopyPermissions: (dcId: number) => void;
  onDeleteNode: (node: TreeDataNode) => void;
}

export const SidebarTree: React.FC<SidebarTreeProps> = ({
  treeData,
  selectedNodeId,
  onSelectNode,
  onAddFolder,
  onAddDevice,
  onManageUsers,
  onCopyPermissions,
  onDeleteNode,
}) => {
  const [expandedNodes, setExpandedNodes] = useState<Record<string, boolean>>({});

  const toggleExpand = (nodeId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setExpandedNodes((prev) => ({
      ...prev,
      [nodeId]: !prev[nodeId],
    }));
  };

  const renderNode = (node: TreeDataNode, level: number = 0) => {
    const isExpanded = expandedNodes[node.id] !== false; // Default expanded
    const isSelected = selectedNodeId === node.id;

    const getNodeIcon = () => {
      switch (node.type) {
        case 'datacenter':
          return <Database className="node-icon icon-dc" size={16} />;
        case 'folder':
          return <FolderIcon className="node-icon icon-folder" size={16} />;
        case 'device':
          return <Server className="node-icon icon-device" size={16} />;
      }
    };

    return (
      <div key={node.id} className="tree-node-wrapper">
        <div
          className={`tree-node-row level-${level} ${isSelected ? 'selected' : ''}`}
          onClick={() => onSelectNode(node)}
          style={{ paddingLeft: `${level * 16 + 8}px` }}
        >
          {node.children && node.children.length > 0 ? (
            <span
              className="expand-arrow"
              onClick={(e) => toggleExpand(node.id, e)}
            >
              {isExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
            </span>
          ) : (
            <span className="expand-arrow-spacer"></span>
          )}

          {getNodeIcon()}
          <span className="node-name" title={node.name}>
            {node.name}
          </span>

          <div className="node-action-hover">
            {node.type === 'datacenter' && (
              <>
                <button
                  className="icon-action-btn"
                  title="Add Folder"
                  onClick={(e) => {
                    e.stopPropagation();
                    onAddFolder(node.numericId);
                  }}
                >
                  <FolderPlus size={13} />
                </button>
                <button
                  className="icon-action-btn"
                  title="Datacenter Users & Copy Permissions"
                  onClick={(e) => {
                    e.stopPropagation();
                    onManageUsers(node.numericId);
                  }}
                >
                  <Users size={13} />
                </button>
              </>
            )}

            {node.type === 'folder' && (
              <button
                className="icon-action-btn"
                title="Add Device/Host"
                onClick={(e) => {
                  e.stopPropagation();
                  onAddDevice(node.numericId);
                }}
              >
                <Plus size={13} />
              </button>
            )}

            <button
              className="icon-action-btn btn-danger-icon"
              title={`Delete ${node.type}`}
              onClick={(e) => {
                e.stopPropagation();
                onDeleteNode(node);
              }}
            >
              <Trash2 size={13} />
            </button>
          </div>
        </div>

        {node.children && node.children.length > 0 && isExpanded && (
          <div className="tree-children">
            {node.children.map((child) => renderNode(child, level + 1))}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="infravault-sidebar-tree">
      <div className="sidebar-header">
        <span className="sidebar-title">Infrastructure Inventory</span>
      </div>

      <div className="tree-container">
        {treeData.length === 0 ? (
          <div className="tree-empty-state">
            <Database size={24} className="text-muted" />
            <p>No Datacenters found.</p>
            <p className="subtext">Click "+ New Datacenter" to start.</p>
          </div>
        ) : (
          treeData.map((node) => renderNode(node, 0))
        )}
      </div>
    </div>
  );
};
