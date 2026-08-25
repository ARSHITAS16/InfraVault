export type UserRole = 'SUPER_ADMIN' | 'DC_ADMIN' | 'OPERATOR' | 'VIEWER';
export type PermissionLevel = 'ADMIN' | 'WRITE' | 'READ';

export interface User {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  active: boolean;
  createdAt?: string;
}

export interface Datacenter {
  id: number;
  name: string;
  location?: string;
  description?: string;
}

export interface DatacenterUser {
  id: number;
  datacenter: Datacenter;
  user: User;
  permissionLevel: PermissionLevel;
}

export interface Folder {
  id: number;
  name: string;
  datacenter: Datacenter;
}

export interface Device {
  id: number;
  hostname: string;
  model?: string;
  serialNumber?: string;
  capacity?: string;
  serviceTag?: string;
  supportEndDate?: string;
  consolePort?: string;
  idracConfigured?: boolean;
  folder: Folder;
}

export interface CredentialMasked {
  id: number;
  type: string; // IDRAC, SYSADMIN, SECOFF, CONSOLE, PASSPHRASE, OTHER
  username: string;
  maskedSecret: string;
  updatedAt?: string;
}

export interface AuditLog {
  id: number;
  userId: number;
  username: string;
  action: string;
  entityType: string;
  entityId?: number;
  datacenterId?: number;
  timestamp: string;
  metadata?: string;
}

export interface ImportPreviewRow {
  rowNum: number;
  hostname: string;
  model?: string;
  serialNumber?: string;
  capacity?: string;
  serviceTag?: string;
  supportEod?: string;
  idracPassword?: string;
  sysadminPassword?: string;
  secoffPassword?: string;
  passphrase?: string;
  rawIdracPassword?: string;
  rawSysadminPassword?: string;
  rawSecoffPassword?: string;
  rawPassphrase?: string;
  valid: boolean;
  errors: string[];
}
