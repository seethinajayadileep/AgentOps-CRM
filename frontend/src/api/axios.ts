import axios from 'axios';

/**
 * Centralized Axios client configuration.
 * All API modules must use this client to ensure consistent baseURL handling.
 * 
 * Production (Vercel): set VITE_API_BASE_URL at build time to the Railway
 * origin plus /api, e.g. https://your-service.up.railway.app/api
 * 
 * @version 0.2.0
 */

// Local Vite must use the /api proxy. frontend/.env often points at a remote
// API, which leaves recordings on a cross-origin URL (readyState 0).
const rawBaseUrl = import.meta.env.DEV
  ? '/api'
  : import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Remove trailing slashes to avoid double-slashing
const baseURL = rawBaseUrl.replace(/\/+$/, '');

export const API_BASE_URL = baseURL;

export const apiClient = axios.create({
  baseURL,
  timeout: 30000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = typeof localStorage === 'undefined' ? null : localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // Extract error message from backend response
    let errorMessage = 'Network error occurred';

    if (error.response?.data && !(error.response.data instanceof Blob)) {
      const data = error.response.data;

      // Handle validation errors with field-specific errors
      if (data.error === 'VALIDATION_ERROR' && data.errors) {
        const fieldErrors = Object.entries(data.errors)
          .map(([field, msg]) => `${field}: ${msg}`)
          .join(', ');
        errorMessage = fieldErrors || data.message || 'Validation failed';
      }
      // Handle other backend errors
      else if (data.message) {
        errorMessage = data.message;
      }
      else if (data.error === 'SHOWCASE_ACTION_DISABLED') {
        errorMessage = data.message || 'This action is currently disabled.';
      }
      else if (data.error === 'EMAIL_ALREADY_EXISTS') {
        errorMessage = data.message || 'An account with this email already exists.';
      }
      else if (data.error === 'INVALID_CREDENTIALS') {
        errorMessage = data.message || 'Email or password is incorrect.';
      }
      // Handle BusinessAlreadyExistsException
      else if (data.error === 'BUSINESS_ALREADY_EXISTS') {
        errorMessage = 'A business with this website URL already exists';
      }
      // Handle BusinessNotFoundException
      else if (data.error === 'BUSINESS_NOT_FOUND') {
        errorMessage = 'Business not found';
      }
    }
    // Handle network errors
    else if (error.message) {
      errorMessage = error.message;
    }

    // Create a custom error object with the message
    const customError = {
      ...error,
      message: errorMessage,
      isValidationError: error.response?.data?.error === 'VALIDATION_ERROR',
      validationErrors: error.response?.data?.errors || {},
    };

    const status = error.response?.status;
    const requestUrl = String(error.config?.url || '');
    const isAuthRequest = requestUrl.includes('/auth/login') || requestUrl.includes('/auth/signup') || requestUrl.includes('/auth/me');
    if (status === 401 && !isAuthRequest) {
      window.dispatchEvent(new Event('auth:unauthorized'));
    }

    return Promise.reject(customError);
  }
);

export default apiClient;
