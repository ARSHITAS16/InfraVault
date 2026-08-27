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

export async function apiRequest<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = localStorage.getItem('token');

  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  if (!(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

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
    let errorMessage = 'API request failed';
    if (typeof data === 'object' && data?.message) {
      errorMessage = data.message;
    } else if (typeof data === 'string' && data) {
      errorMessage = data;
    }
    throw new Error(errorMessage);
  }

  return data as T;
}

// Authentication APIs
export const authApi = {
  login: (username?: string, password?: string) =>
    apiRequest<{ token: string; userId: number; id: number; username: string; role: string }>(
      '/api/auth/login',
      { method: 'POST', body: JSON.stringify({ username, password }) }
    ),

  register: (username?: string, email?: string, password?: string, role?: string) =>
    apiRequest<{ token: string; userId: number; id: number; username: string; role: string }>(
      '/api/auth/register',
      { method: 'POST', body: JSON.stringify({ username, email, password, role }) }
    ),
};

// Datacenter APIs
export const datacentersApi = {
  getAll: () => apiRequest<Datacenter[]>('/api/datacenters'),
  getDatacenters: () => apiRequest<Datacenter[]>('/api/datacenters'),

  create: (data: { name: string; description?: string }) =>
    apiRequest<Datacenter>('/api/datacenters', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  createDatacenter: (name: string, description?: string) =>
    apiRequest<Datacenter>('/api/datacenters', {
      method: 'POST',
      body: JSON.stringify({ name, description }),
    }),

  getFolders: (datacenterId: number) =>
    apiRequest<Folder[]>(`/api/datacenters/${datacenterId}/folders`),

  createFolder: (datacenterId: number, name: string) =>
    apiRequest<Folder>(`/api/datacenters/${datacenterId}/folders`, {
      method: 'POST',
      body: JSON.stringify({ name }),
    }),

  getUsers: (datacenterId: number) =>
    apiRequest<DatacenterUser[]>(`/api/datacenters/${datacenterId}/users`),

  addUser: (datacenterId: number, userIdOrData: any, permissionLevel?: string) => {
    let payload = {};
    if (typeof userIdOrData === 'object' && userIdOrData !== null) {
      payload = userIdOrData;
    } else {
      payload = { userId: userIdOrData, permissionLevel };
    }
    return apiRequest<DatacenterUser>(`/api/datacenters/${datacenterId}/users`, {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  removeUser: (datacenterId: number, userId: number) =>
    apiRequest<void>(`/api/datacenters/${datacenterId}/users/${userId}`, {
      method: 'DELETE',
    }),

  copyPermissions: (targetDatacenterId: number, sourceDatacenterId: number) =>
    apiRequest<{ message: string }>(
      `/api/datacenters/${targetDatacenterId}/copy-permissions?sourceId=${sourceDatacenterId}`,
      { method: 'POST' }
    ),

  delete: (datacenterId: number) =>
    apiRequest<void>(`/api/datacenters/${datacenterId}`, {
      method: 'DELETE',
    }),

  deleteDatacenter: (datacenterId: number) =>
    apiRequest<void>(`/api/datacenters/${datacenterId}`, {
      method: 'DELETE',
    }),
};

// Folder APIs
export const foldersApi = {
  getByDatacenter: (datacenterId: number) =>
    apiRequest<Folder[]>(`/api/datacenters/${datacenterId}/folders`),

  getDevices: (folderId: number) =>
    apiRequest<Device[]>(`/api/folders/${folderId}/devices`),

  create: (datacenterId: number, data: { name: string }) =>
    apiRequest<Folder>(`/api/datacenters/${datacenterId}/folders`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  createFolder: (datacenterId: number, name: string) =>
    apiRequest<Folder>(`/api/datacenters/${datacenterId}/folders`, {
      method: 'POST',
      body: JSON.stringify({ name }),
    }),

  createDevice: (folderId: number, data: any) =>
    apiRequest<Device>(`/api/folders/${folderId}/devices`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  delete: (folderId: number, force = false) =>
    apiRequest<void>(`/api/folders/${folderId}?force=${force}`, {
      method: 'DELETE',
    }),

  deleteFolder: (folderId: number, force = false) =>
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

  createDevice: (folderId: number, data: any) =>
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

  deleteDevice: (deviceId: number) =>
    apiRequest<void>(`/api/devices/${deviceId}`, {
      method: 'DELETE',
    }),
};

// Credential Reveal & Create API
export const credentialsApi = {
  reveal: (credentialId: number, datacenterId: number) =>
    apiRequest<{ id: number; type: string; secret: string }>(
      `/api/credentials/${credentialId}/reveal?datacenterId=${datacenterId}`,
      { method: 'POST' }
    ),

  create: (deviceId: number, type: string, username: string, password?: string) =>
    apiRequest<{ message: string }>('/api/credentials/create', {
      method: 'POST',
      body: JSON.stringify({ deviceId, type, username, password }),
    }),
};

// Audit Log API
export const auditApi = {
  getLogs: () => apiRequest<AuditLog[]>('/api/audit'),
  getAuditLogs: () => apiRequest<AuditLog[]>('/api/audit'),
};

// Users API
export const usersApi = {
  getAll: () => apiRequest<User[]>('/api/users'),
  getAllUsers: () => apiRequest<User[]>('/api/users'),
};

// Excel Import API
export const importApi = {
  preview: (file: File, folderId?: number) => {
    const formData = new FormData();
    formData.append('file', file);
    const targetFolderId = folderId || 1;
    return apiRequest<ImportPreviewRow[] | { filename: string; previewRows: ImportPreviewRow[] }>(
      `/api/import/preview?folderId=${targetFolderId}`,
      { method: 'POST', body: formData }
    ).then((res: any) => {
      if (Array.isArray(res)) return res;
      if (res && Array.isArray(res.previewRows)) return res.previewRows;
      return [];
    });
  },

  commit: (folderIdOrData: any, rowsOrData?: any) => {
    let folderId = 1;
    let rows = [];

    if (typeof folderIdOrData === 'number') {
      folderId = folderIdOrData;
      rows = rowsOrData || [];
    } else if (typeof folderIdOrData === 'object' && folderIdOrData !== null) {
      folderId = folderIdOrData.folderId || 1;
      rows = folderIdOrData.previewRows || folderIdOrData.rows || [];
    }

    return apiRequest<{ importedCount: number; message: string }>(
      `/api/import/commit?folderId=${folderId}`,
      {
        method: 'POST',
        body: JSON.stringify(rows),
      }
    );
  },
};
