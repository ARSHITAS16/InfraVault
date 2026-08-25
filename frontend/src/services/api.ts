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

const API_BASE_URL = 'http://localhost:8080';

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

  // Don't set Content-Type if FormData is used
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
    let errorMessage = 'Request failed';
    if (typeof data === 'object' && data?.message) {
      errorMessage = data.message;
    } else if (typeof data === 'string' && data) {
      errorMessage = data;
    } else if (response.status === 403) {
      errorMessage = 'Permission denied for this action or datacenter (HTTP 403)';
    } else if (response.status === 401) {
      errorMessage = 'Authentication required (HTTP 401)';
    }
    throw new Error(errorMessage);
  }

  return data as T;
}

// Auth API
export const authApi = {
  login: (username: string, password: string) =>
    apiRequest<{ id: number; username: string; email: string; role: string; token: string }>(
      '/api/auth/login',
      {
        method: 'POST',
        body: JSON.stringify({ username, password }),
      }
    ),
  register: (username: string, email: string, password: string, role: string = 'OPERATOR') =>
    apiRequest<{ id: number; username: string; email: string; role: string; token: string }>(
      '/api/auth/register',
      {
        method: 'POST',
        body: JSON.stringify({ username, email, password, role }),
      }
    ),
};

// Users API
export const usersApi = {
  getAllUsers: () => apiRequest<User[]>('/api/users'),
};

// Datacenters API
export const datacentersApi = {
  getDatacenters: () => apiRequest<Datacenter[]>('/api/datacenters'),
  getDatacenter: (id: number) => apiRequest<Datacenter>(`/api/datacenters/${id}`),
  createDatacenter: (name: string, description: string) =>
    apiRequest<Datacenter>('/api/datacenters', {
      method: 'POST',
      body: JSON.stringify({ name, description }),
    }),
  getUsers: (datacenterId: number) =>
    apiRequest<DatacenterUser[]>(`/api/datacenters/${datacenterId}/users`),
  addUser: (datacenterId: number, userId: number, permissionLevel: string) =>
    apiRequest<DatacenterUser>(`/api/datacenters/${datacenterId}/users`, {
      method: 'POST',
      body: JSON.stringify({ userId, permissionLevel }),
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
  getFolders: (datacenterId: number) =>
    apiRequest<Folder[]>(`/api/datacenters/${datacenterId}/folders`),
  createFolder: (datacenterId: number, name: string) =>
    apiRequest<Folder>(`/api/datacenters/${datacenterId}/folders`, {
      method: 'POST',
      body: JSON.stringify({ name }),
    }),
};

// Folders API
export const foldersApi = {
  deleteFolder: (folderId: number, force: boolean = false) =>
    apiRequest<void>(`/api/folders/${folderId}?force=${force}`, {
      method: 'DELETE',
    }),
  getDevices: (folderId: number) => apiRequest<Device[]>(`/api/folders/${folderId}/devices`),
  createDevice: (folderId: number, deviceData: any) =>
    apiRequest<Device>(`/api/folders/${folderId}/devices`, {
      method: 'POST',
      body: JSON.stringify(deviceData),
    }),
};

// Devices API
export const devicesApi = {
  getDevice: (id: number) => apiRequest<Device>(`/api/devices/${id}`),
  deleteDevice: (id: number) =>
    apiRequest<void>(`/api/devices/${id}`, { method: 'DELETE' }),
  getCredentials: (deviceId: number, datacenterId: number) =>
    apiRequest<CredentialMasked[]>(`/api/devices/${deviceId}/credentials?datacenterId=${datacenterId}`),
};

// Credentials API
export const credentialsApi = {
  reveal: (credentialId: number, datacenterId: number) =>
    apiRequest<{ secret: string }>(`/api/credentials/${credentialId}/reveal?datacenterId=${datacenterId}`, {
      method: 'POST',
    }),
};

// Excel Import API
export const importApi = {
  preview: (file: File, folderId: number) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('folderId', folderId.toString());
    return apiRequest<ImportPreviewRow[]>('/api/import/preview', {
      method: 'POST',
      body: formData,
    });
  },
  commit: (folderId: number, rows: ImportPreviewRow[]) =>
    apiRequest<{ message: string; count: number }>('/api/import/commit?folderId=' + folderId, {
      method: 'POST',
      body: JSON.stringify(rows),
    }),
};

// Audit API
export const auditApi = {
  getAuditLogs: (datacenterId?: number) =>
    apiRequest<AuditLog[]>(`/api/audit${datacenterId ? `?datacenterId=${datacenterId}` : ''}`),
};
