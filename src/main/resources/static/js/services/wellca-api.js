/* =============================================================================
 * Wellca API Service
 * 
 * PURPOSE: Centralizes all API calls for the Wellca management system
 * Handles CSRF protection, error handling, and response processing
 * ============================================================================= */

class WellcaApiService {
    constructor() {
        // Get CSRF tokens from meta tags
        this.csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        this.csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
        this.baseUrl = '/wellca-management';
    }

    /**
     * Generic request handler with error handling and CSRF protection
     * @param {string} endpoint - API endpoint
     * @param {Object} options - Request options
     * @returns {Promise} - API response
     */
    async makeRequest(endpoint, options = {}) {
        try {
            const defaultOptions = {
                method: options.method || 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                    [this.csrfHeader]: this.csrfToken,
                    ...options.headers
                }
            };

            if (options.body) {
                defaultOptions.body = JSON.stringify(options.body);
            }

            const response = await fetch(`${this.baseUrl}${endpoint}`, defaultOptions);
            
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`API Error (${response.status}): ${errorText}`);
            }

            return await response.json();
        } catch (error) {
            console.error('API Request failed:', error);
            throw error;
        }
    }

    /**
     * Submit form data to the server
     * @param {Object} formData - The form data to submit
     * @returns {Promise} - Submission response
     */
    async submitForm(formData) {
        return this.makeRequest('/submit', {
            method: 'POST',
            body: formData
        });
    }

    /**
     * Fetch report data for a date range
     * @param {string} startDate - Start date in YYYY-MM-DD format
     * @param {string} endDate - End date in YYYY-MM-DD format
     * @returns {Promise} - Report data
     */
    async getReportData(startDate, endDate) {
        const params = new URLSearchParams({
            startDate: startDate,
            endDate: endDate
        });
        
        return this.makeRequest(`/range?${params}`);
    }

    /**
     * Submit service-specific data
     * @param {Object} serviceData - Service data to submit
     * @returns {Promise} - Submission response
     */
    async submitServiceData(serviceData) {
        return this.makeRequest('/service', {
            method: 'POST',
            body: serviceData
        });
    }

    /**
     * Update existing record
     * @param {string} id - Record ID
     * @param {Object} data - Updated data
     * @returns {Promise} - Update response
     */
    async updateRecord(id, data) {
        return this.makeRequest(`/update/${id}`, {
            method: 'PUT',
            body: data
        });
    }

    /**
     * Delete a record
     * @param {string} id - Record ID
     * @returns {Promise} - Deletion response
     */
    async deleteRecord(id) {
        return this.makeRequest(`/delete/${id}`, {
            method: 'DELETE'
        });
    }

    /**
     * Validate form data before submission
     * @param {Object} data - Data to validate
     * @returns {Promise} - Validation response
     */
    async validateData(data) {
        return this.makeRequest('/validate', {
            method: 'POST',
            body: data
        });
    }

    /**
     * Get summary statistics
     * @param {string} period - Time period for statistics
     * @returns {Promise} - Statistics data
     */
    async getStatistics(period = 'daily') {
        return this.makeRequest(`/statistics?period=${period}`);
    }
}

// Create and export a singleton instance
const wellcaApi = new WellcaApiService();
export default wellcaApi;

// Example usage:
/*
try {
    // Submit form data
    const response = await wellcaApi.submitForm({
        date: '2024-03-20',
        serviceType: 'DELIVERY',
        serviceCost: 100
    });
    console.log('Form submitted:', response);

    // Get report data
    const reportData = await wellcaApi.getReportData('2024-03-01', '2024-03-20');
    console.log('Report data:', reportData);

} catch (error) {
    console.error('API Error:', error.message);
}
*/
