import api from './api';

const authService = {
  /**
   * Login user
   * @param {string} email
   * @param {string} password
   * @param {string} totpCode - Optional 2FA code
   * @returns {Promise} API response with token and user data
   */
  login: async (email, password, totpCode = null) => {
    const payload = { email, password };
    if (totpCode) {
      payload.totpCode = totpCode;
    }

    const response = await api.post('/auth/login', payload);
    
    if (response.data.success && response.data.data.accessToken) {
      localStorage.setItem('token', response.data.data.accessToken);
      localStorage.setItem('user', JSON.stringify(response.data.data.user));
    }
    
    return response.data;
  },

  /**
   * Register new user
   * @param {Object} data - Registration data
   * @param {string} data.email
   * @param {string} data.password
   * @param {string} data.name
   * @returns {Promise} API response
   */
  register: async (data) => {
    // Remove confirmPassword before sending to backend
    const { confirmPassword, ...registerData } = data;
    const response = await api.post('/auth/register', registerData);
    return response.data;
  },

  /**
   * Logout user
   */
  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login';
  },

  /**
   * Get current user from localStorage
   * @returns {Object|null}
   */
  getCurrentUser: () => {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  },

  /**
   * Get token from localStorage
   * @returns {string|null}
   */
  getToken: () => {
    return localStorage.getItem('token');
  },

  /**
   * Check if user is authenticated
   * @returns {boolean}
   */
  isAuthenticated: () => {
    return !!localStorage.getItem('token');
  },

  /**
   * Request password reset
   * @param {string} email
   * @returns {Promise}
   */
  forgotPassword: async (email) => {
    const response = await api.post('/auth/forgot-password', { email });
    return response.data;
  },

  /**
   * Reset password with token
   * @param {string} token
   * @param {string} newPassword
   * @param {string} confirmPassword
   * @returns {Promise}
   */
  resetPassword: async (token, newPassword, confirmPassword) => {
    const response = await api.post('/auth/reset-password', {
      token,
      newPassword,
      confirmPassword,
    });
    return response.data;
  },

  /**
   * Verify email with token
   * @param {string} email
   * @param {string} token
   * @returns {Promise}
   */
  verifyEmail: async (email, token) => {
    const response = await api.post('/auth/verify-email', { email, token });
    return response.data;
  },
};

export default authService;
