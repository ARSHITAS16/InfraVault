import {
  User,
  Datacenter,
  DatacenterUser,
  Folder,
  Device,
  CredentialMasked,
  AuditLog,
  ImportPreviewRow,
} from '../types';

const API_BASE_URL = (import.meta.env.VITE_API_URL as string) || 'https://infravault-backend-znmg.onrender.com';

// Mock Data for Demo Mode on GitHub Pages
const MOCK_DATACENTERS: Datacenter[] = [
  { id: 1, name: 'sjc51-prod', description: 'San Jose Primary Datacenter (USA)' },
  { id: 2, name: 'iad51-prod', description: 'Ashburn Secondary Datacenter (USA)' },
  { id: 3, name: 'fra01-prod', description: 'Frankfurt European Datacenter (Germany)' }
];

const MOCK_FOLDERS: Folder[] = [
  { id: 1, name: 'datadomain-storage', datacenter: MOCK_DATACENTERS[0] },
  { id: 2, name: 'vcenter-compute-cluster', datacenter: MOCK_DATACENTERS[0] },
  { id: 3, name: 'backup-vaults', datacenter: MOCK_DATACENTERS[1] }
];

const MOCK_DEVICES: Device[] = [
  {
    id: 1,
    hostname: 'sjc01-c01-dds01',
    model: 'DataDomain DD9400',
    serialNumber: 'SN-9948271-SJ',
    capacity: '256TB',
    serviceTag: '7X889K1',
    supportEndDate: '2028-12-31',
    consolePort: 'COM1/115200',
    idracConfigured: true,
    folder: MOCK_FOLDERS[0]
  },
  {
    id: 2,
    hostname: 'sjc01-c01-esx01',
    model: 'Dell PowerEdge R750',
    serialNumber: 'SN-3394812-SJ',
    capacity: '512GB RAM / 64 Cores',
    serviceTag: '9B4412K',
    supportEndDate: '2027-06-30',
    consolePort: 'IPMI/vKVM',
    idracConfigured: true,
    folder: MOCK_FOLDERS[1]
  }
];

const MOCK_CREDENTIALS: CredentialMasked[] = [
  { id: 1, type: 'IDRAC', username: 'root', maskedSecret: '••••••••••••', createdBy: 'admin', updatedAt: '2026-08-26' },
  { id: 2, type: 'SYSADMIN', username: 'sysadmin', maskedSecret: '••••••••••••', createdBy: 'admin', updatedAt: '2026-08-26' },
  { id: 3, type: 'SECOFF', username: 'secoff', maskedSecret: '••••••••••••', createdBy: 'admin', updatedAt: '2026-08-26' },
  { id: 4, type: 'PASSPHRASE', username: 'admin', maskedSecret: '••••••••••••', createdBy: 'admin', updatedAt: '2026-08-26' }
];

const MOCK_AUDIT_LOGS: AuditLog[] = [
  { id: 1, userId: 1, username: 'admin', action: 'REVEAL', entityType: 'CREDENTIAL', entityId: 1, datacenterId: 1, timestamp: '2026-08-26T03:00:00Z', metadata: 'Revealed secret for IDRAC root on sjc01-c01-dds01' },
  { id: 2, userId: 1, username: 'admin', action: 'COPY_PERMISSIONS', entityType: 'DATACENTER', entityId: 2, datacenterId: 2, timestamp: '2026-08-26T02:45:00Z', metadata: 'Copied permissions from sjc51-prod to iad51-prod' },
  { id: 3, userId: 1, username: 'admin', action: 'CREATE_DEVICE', entityType: 'DEVICE', entityId: 1, datacenterId: 1, timestamp: '2026-08-26T02:30:00Z', metadata: 'Created host sjc01-c01-dds01 in datadomain-storage' }
];

export async function apiRequest<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = localStorage.getItem('token');

  // Check if Demo Mode active
  if (token === 'demo-token-123') {
    return handleDemoMode<T>(endpoint, options);
  }

  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  if (!(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  try {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    const text = await response.text();
    let data: any = {};
    try {
      data = text ? JSON.parse(text) : {};
    } catch {
      data = text;
    }

    if (!response.ok) {
      let errorMessage = 'Request failed';
      if (typeof data === 'object' && data?.message) {
        errorMessage = data.message;
      } else if (typeof data === 'string' && data) {
        errorMessage = data;
      }
      throw new Error(errorMessage);
    }

    return data as T;
  } catch (err: any) {
    if (err.message === 'Failed to fetch' || err.name === 'TypeError') {
      throw new Error(`Cannot connect to backend server at ${API_BASE_URL}. Ensure Spring Boot server is running or click 'Explore Live UI Demo Mode' on login.`);
    }
    throw err;
  }
}

function handleDemoMode<T>(endpoint: string, options: RequestInit): Promise<T> {
  return new Promise((resolve) => {
    setTimeout(() => {
      if (endpoint.includes('/api/datacenters') && !endpoint.includes('/folders') && !endpoint.includes('/users') && !endpoint.includes('/copy-permissions')) {
        if (options.method === 'POST') {
          const body = JSON.parse(options.body as string || '{}');
          const newDc: Datacenter = { id: Date.now(), name: body.name, description: body.description };
          MOCK_DATACENTERS.push(newDc);
          return resolve(newDc as any);
        }
        return resolve(MOCK_DATACENTERS as any);
      }
      if (endpoint.includes('/folders') && !endpoint.includes('/devices')) {
        return resolve(MOCK_FOLDERS as any);
      }
      if (endpoint.includes('/devices')) {
        if (endpoint.includes('/credentials')) {
          return resolve(MOCK_CREDENTIALS as any);
        }
        return resolve(MOCK_DEVICES as any);
      }
      if (endpoint.includes('/credentials') && endpoint.includes('/reveal')) {
        return resolve({ id: 1, type: 'IDRAC', secret: 'DemoRevealedSecret123!' } as any);
      }
      if (endpoint.includes('/audit')) {
        return resolve(MOCK_AUDIT_LOGS as any);
      }
      if (endpoint.includes('/users')) {
        return resolve([{ id: 1, username: 'admin', email: 'admin@ringcentral.com', role: 'SUPER_ADMIN', active: true, createdAt: '2026-08-26' }] as any);
      }
      if (endpoint.includes('/copy-permissions')) {
        return resolve({ message: 'Permissions copied successfully (Demo Mode)' } as any);
      }
      resolve([] as any);
    }, 300);
  });
}

// Authentication APIs
export const authApi = {
  login: (data: { username?: string; password?: string }) =>
    apiRequest<{ token: string; userId: number; username: string; role: string }>(
      '/api/auth/login',
      { method: 'POST', body: JSON.stringify(data) }
    ),

  register: (data: { username?: string; email?: string; password?: string; role?: string }) =>
    apiRequest<{ token: string; userId: number; username: string; role: string }>(
      '/api/auth/register',
      { method: 'POST', body: JSON.stringify(data) }
    ),
};

// Datacenter APIs
export const datacentersApi = {
  getAll: () => apiRequest<Datacenter[]>('/api/datacenters'),

  create: (data: { name: string; description?: string }) =>
    apiRequest<Datacenter>('/api/datacenters', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  getUsers: (datacenterId: number) =>
    apiRequest<DatacenterUser[]>(`/api/datacenters/${datacenterId}/users`),

  addUser: (datacenterId: number, data: { userId: number; permissionLevel: string }) =>
    apiRequest<DatacenterUser>(`/api/datacenters/${datacenterId}/users`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  removeUser: (datacenterId: number, userId: number) =>
    apiRequest<void>(`/api/datacenters/${datacenterId}/users/${userId}`, {
      method: 'DELETE',
    }),

  copyPermissions: (targetDatacenterId: number, sourceDatacenterId: number) =>
    apiRequest<{ message: string }>(
      `/api/datacenters/${targetDatacenterId}/copy-permissions?sourceId=${sourceDatacenterId}`,
      { method: 'POST' }
    ),
};

// Folder APIs
export const foldersApi = {
  getByDatacenter: (datacenterId: number) =>
    apiRequest<Folder[]>(`/api/datacenters/${datacenterId}/folders`),

  create: (datacenterId: number, data: { name: string }) =>
    apiRequest<Folder>(`/api/datacenters/${datacenterId}/folders`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  delete: (folderId: number, force = false) =>
    apiRequest<void>(`/api/folders/${folderId}?force=${force}`, {
      method: 'DELETE',
    }),
};

// Device APIs
export const devicesApi = {
  getByFolder: (folderId: number) =>
    apiRequest<Device[]>(`/api/folders/${folderId}/devices`),

  getById: (deviceId: number) =>
    apiRequest<Device>(`/api/devices/${deviceId}`),

  create: (folderId: number, data: any) =>
    apiRequest<Device>(`/api/folders/${folderId}/devices`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  getCredentials: (deviceId: number, datacenterId: number) =>
    apiRequest<CredentialMasked[]>(
      `/api/devices/${deviceId}/credentials?datacenterId=${datacenterId}`
    ),

  delete: (deviceId: number) =>
    apiRequest<void>(`/api/devices/${deviceId}`, {
      method: 'DELETE',
    }),
};

// Credential Reveal API
export const credentialsApi = {
  reveal: (credentialId: number, datacenterId: number) =>
    apiRequest<{ id: number; type: string; secret: string }>(
      `/api/credentials/${credentialId}/reveal?datacenterId=${datacenterId}`,
      { method: 'POST' }
    ),
};

// Audit Log API
export const auditApi = {
  getLogs: () => apiRequest<AuditLog[]>('/api/audit'),
};

// Users API
export const usersApi = {
  getAll: () => apiRequest<User[]>('/api/users'),
};

// Excel Import API
export const importApi = {
  preview: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiRequest<{ filename: string; previewRows: ImportPreviewRow[] }>(
      '/api/import/preview',
      { method: 'POST', body: formData }
    );
  },

  commit: (datacenterId: number, folderName: string, previewRows: ImportPreviewRow[]) =>
    apiRequest<{ importedCount: number; message: string }>('/api/import/commit', {
      method: 'POST',
      body: JSON.stringify({ datacenterId, folderName, previewRows }),
    }),
};
