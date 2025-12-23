import axios from 'axios';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9499/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add a request interceptor to add the auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add a response interceptor to handle errors
api.interceptors.response.use(
  (response) => {
    // Backend trả về dạng: { success: true, message: "...", data: {...} }
    return response;
  },
  (error) => {
    // Log error để debug (chỉ trong development)
    if (process.env.NODE_ENV === 'development') {
      console.error('API Error:', {
        url: error.config?.url,
        method: error.config?.method,
        status: error.response?.status,
        data: error.response?.data,
      });
    }

    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      // Chỉ redirect nếu không phải đang ở trang login
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authApi = {
  login: (email: string, password: string) =>
    api.post('/auth/login', { email, password }),
  getCurrentUser: () => api.get('/auth/me'),
};

// Role API
export const roleApi = {
  getAll: () => api.get('/admin/roles'),
  getTree: () => api.get('/admin/roles/tree'),
  getById: (id: number) => api.get(`/admin/roles/${id}`),
  create: (data: { name: string; description?: string; parentId?: number }) =>
    api.post('/admin/roles', data),
  update: (id: number, data: { name: string; description?: string; parentId?: number }) =>
    api.put(`/admin/roles/${id}`, data),
  delete: (id: number) => api.delete(`/admin/roles/${id}`),
};

// User API
export const userApi = {
  getAll: () => api.get('/admin/users'),
  getById: (id: number) => api.get(`/admin/users/${id}`),
  create: (data: {
    email: string;
    password: string;
    fullName: string;
    phone?: string;
    roleId?: number;
    organizationIds?: number[];
    departmentId?: number;
    positionId?: number;
    loginMethod?: string;
    isActive?: boolean;
  }) => api.post('/admin/users', data),
  update: (id: number, data: {
    email?: string;
    password?: string;
    fullName?: string;
    phone?: string;
    roleId?: number;
    organizationIds?: number[];
    departmentId?: number;
    positionId?: number;
    loginMethod?: string;
    isActive?: boolean;
  }) => api.put(`/admin/users/${id}`, data),
  delete: (id: number) => api.delete(`/admin/users/${id}`),
  loadZaloUserId: (id: number) => api.post(`/admin/users/${id}/load-zalo-user-id`),
};

// Organization API
export const organizationApi = {
  getAll: () => api.get('/admin/organizations'),
  getActive: () => api.get('/admin/organizations/active'),
  getById: (id: number) => api.get(`/admin/organizations/${id}`),
  create: (data: {
    name: string;
    code?: string;
    address?: string;
    phone?: string;
    email?: string;
    isActive?: boolean;
  }) => api.post('/admin/organizations', data),
  update: (id: number, data: {
    name: string;
    code?: string;
    address?: string;
    phone?: string;
    email?: string;
    isActive?: boolean;
  }) => api.put(`/admin/organizations/${id}`, data),
  delete: (id: number) => api.delete(`/admin/organizations/${id}`),
};

// Department API
export const departmentApi = {
  getAll: () => api.get('/admin/departments'),
  getByOrganization: (organizationId: number) =>
    api.get(`/admin/departments/by-organization/${organizationId}`),
  getById: (id: number) => api.get(`/admin/departments/${id}`),
  create: (data: {
    name: string;
    code?: string;
    organizationId: number;
    description?: string;
    isActive?: boolean;
  }) => api.post('/admin/departments', data),
  update: (id: number, data: {
    name: string;
    code?: string;
    organizationId: number;
    description?: string;
    isActive?: boolean;
  }) => api.put(`/admin/departments/${id}`, data),
  delete: (id: number) => api.delete(`/admin/departments/${id}`),
};

// Report Request API
export const reportRequestApi = {
  getAll: () => api.get('/report-requests'),
  getMyRequests: () => api.get('/report-requests/my-requests'),
  getReceivedRequests: () => api.get('/report-requests/received'),
  getById: (id: number) => api.get(`/report-requests/${id}`),
  create: (data: {
    title: string;
    description?: string;
    organizationIds?: number[];
    departmentIds?: number[];
    userIds?: number[];
    deadline: string;
  }) => api.post('/report-requests', data),
  update: (id: number, data: {
    title: string;
    description?: string;
    organizationIds?: number[];
    departmentIds?: number[];
    userIds?: number[];
    deadline: string;
  }) => api.put(`/report-requests/${id}`, data),
  delete: (id: number) => api.delete(`/report-requests/${id}`),
  updateStatus: (id: number, status: string) => 
    api.patch(`/report-requests/${id}/status?status=${status}`),
  getHistory: (id: number) => api.get(`/report-requests/${id}/history`),
  forward: (id: number, data: {
    title: string;
    description?: string;
    forwardNote?: string;
    organizationIds?: number[];
    departmentIds?: number[];
    userIds?: number[];
    deadline: string;
  }) => api.post(`/report-requests/${id}/forward`, data),
  exportWord: (id: number) => api.get(`/report-requests/${id}/export-word`, {
    responseType: 'blob',
  }),
};

// Report Response API
export const reportResponseApi = {
  getByRequestId: (requestId: number) => api.get(`/report-responses/by-request/${requestId}`),
  getMyResponses: () => api.get('/report-responses/my-responses'),
  getById: (id: number) => api.get(`/report-responses/${id}`),
  getMyResponseForRequest: (requestId: number) => api.get(`/report-responses/by-request/${requestId}/my`),
  getMyStatistics: () => api.get('/report-responses/my-statistics'),
  create: (data: {
    reportRequestId: number;
    note?: string;
    items: { 
      title?: string;
      content?: string;
      progress?: number;
      difficulties?: string;
      displayOrder?: number;
    }[];
  }) => api.post('/report-responses', data),
  update: (id: number, data: {
    reportRequestId: number;
    note?: string;
    items: { 
      title?: string;
      content?: string;
      progress?: number;
      difficulties?: string;
      displayOrder?: number;
    }[];
  }) => api.put(`/report-responses/${id}`, data),
  delete: (id: number) => api.delete(`/report-responses/${id}`),
  addItem: (responseId: number, formData: FormData) => 
    api.post(`/report-responses/${responseId}/items`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }),
  deleteItem: (itemId: number) => api.delete(`/report-responses/items/${itemId}`),
  uploadItemFile: (itemId: number, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post(`/report-responses/items/${itemId}/file`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  getHistory: (responseId: number) => api.get(`/report-responses/${responseId}/history`),
  getCommentHistory: (responseId: number) => api.get(`/report-responses/${responseId}/comments`),
  evaluate: (responseId: number, score: number, comment?: string) => {
    const params = new URLSearchParams()
    params.append('score', score.toString())
    if (comment) {
      params.append('comment', comment)
    }
    return api.post(`/report-responses/${responseId}/evaluate?${params.toString()}`)
  },
  sendBack: (responseId: number, comment: string) => 
    api.post(`/report-responses/${responseId}/send-back?comment=${encodeURIComponent(comment)}`),
  selfEvaluate: (responseId: number, score: number) => 
    api.post(`/report-responses/${responseId}/self-evaluate?score=${score}`),
  getFileUrl: (filePath: string) => `${API_BASE_URL}/report-responses/files/${filePath}`,
  downloadFile: (filePath: string) =>
    api.get(`/report-responses/files/${filePath}`, { responseType: 'blob' }),
};

// Position API
export const positionApi = {
  getAll: () => api.get('/admin/positions'),
  getActive: () => api.get('/admin/positions/active'),
  getById: (id: number) => api.get(`/admin/positions/${id}`),
  create: (data: {
    name: string;
    code?: string;
    description?: string;
    displayOrder?: number;
    isActive?: boolean;
  }) => api.post('/admin/positions', data),
  update: (id: number, data: {
    name: string;
    code?: string;
    description?: string;
    displayOrder?: number;
    isActive?: boolean;
  }) => api.put(`/admin/positions/${id}`, data),
  delete: (id: number) => api.delete(`/admin/positions/${id}`),
};

// Common Data API (for non-admin users)
export const commonApi = {
  getOrganizations: () => api.get('/common/organizations'),
  getDepartments: () => api.get('/common/departments'),
  getDepartmentsByOrganization: (organizationId: number) =>
    api.get(`/common/departments/by-organization/${organizationId}`),
  getUsers: () => api.get('/common/users'),
  getUsersByDepartment: (departmentId: number) =>
    api.get(`/common/users/by-department/${departmentId}`),
  getUsersByOrganization: (organizationId: number) =>
    api.get(`/common/users/by-organization/${organizationId}`),
};

// Admin Report API
export const adminReportApi = {
  getAll: (params?: {
    search?: string;
    status?: string;
    createdById?: number;
    submittedById?: number;
    organizationId?: number;
    departmentId?: number;
  }) => {
    const queryParams = new URLSearchParams();
    if (params?.search) queryParams.append('search', params.search);
    if (params?.status) queryParams.append('status', params.status);
    if (params?.createdById) queryParams.append('createdById', params.createdById.toString());
    if (params?.submittedById) queryParams.append('submittedById', params.submittedById.toString());
    if (params?.organizationId) queryParams.append('organizationId', params.organizationId.toString());
    if (params?.departmentId) queryParams.append('departmentId', params.departmentId.toString());
    return api.get(`/admin/reports?${queryParams.toString()}`);
  },
  getStatistics: () => api.get('/admin/reports/statistics'),
  updateRequest: (id: number, data: {
    title: string;
    description?: string;
    deadline?: string;
    status?: string;
    organizationIds?: number[];
    departmentIds?: number[];
  }) => api.put(`/admin/reports/requests/${id}`, data),
  updateResponse: (id: number, data: {
    note?: string;
    score?: number;
    comment?: string;
    selfScore?: number;
    items?: any[];
  }) => api.put(`/admin/reports/responses/${id}`, data),
  getResponseById: (id: number) => api.get(`/admin/reports/responses/${id}`),
  getScoreStatistics: (userId?: number) => {
    const params = userId ? `?userId=${userId}` : '';
    return api.get(`/admin/reports/score-statistics${params}`);
  },
};

// Zalo OAuth API
export const zaloOAuthApi = {
  init: (refreshToken: string) => api.post('/admin/zalo/oauth/init', { refreshToken }),
  getInfo: () => api.get('/admin/zalo/oauth/info'),
  getStatus: () => api.get('/admin/zalo/oauth/status'),
  refresh: () => api.post('/admin/zalo/oauth/refresh'),
};

export default api;

