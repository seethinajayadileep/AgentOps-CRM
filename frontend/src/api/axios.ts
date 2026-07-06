import axios from 'axios';

/**
 * Centralized Axios client configuration.
 * All API modules must use this client to ensure consistent baseURL handling.
 * 
 * Production deployment: VITE_API_BASE_URL must be set in Vercel.
 * Example: https://upbeat-blessing-production-0f39.up.railway.app/api
 * 
 * @version 0.2.0
 */

const rawBaseUrl =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Remove trailing slashes to avoid double-slashing
const baseURL = rawBaseUrl.replace(/\/+$/, '');

export const apiClient = axios.create({
  baseURL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    // Add auth token if available
    const token = localStorage.getItem('auth_token');
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

    if (error.response?.data) {
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

    return Promise.reject(customError);
  }
);

export default apiClient;
