/* =============================================================================
 * PacMed Form Controller Module
 * 
 * PURPOSE: Manages form validation, submission, and data visualization for 
 * medication pouch checking workflow
 * 
 * DEPENDENCIES:
 * - Chart.js for data visualization
 * - Server-sent events (SSE) for real-time updates
 * - CSRF token from meta tag
 * 
 * ARCHITECTURE:
 * - Form validation and submission
 * - Real-time data updates via SSE
 * - Chart rendering and management
 * - Error handling and user feedback
 * ============================================================================= */

document.addEventListener('DOMContentLoaded', function() {
    /* ------------------------------------------------------------------------- 
     * Core Element References & Initialization
     * --------------------------------------------------------------------- */
    const form = document.getElementById('questionForm');
    const successMessage = document.getElementById('successMessage');
    const startTime = document.getElementById('startTime');
    const endTime = document.getElementById('endTime');

    // Validate CSRF token availability
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    if (!csrfToken) {
        console.error('CSRF token not found');
        return;
    }

    /* ------------------------------------------------------------------------- 
     * Time Input Validation
     * @note Uses 24-hour format (HH:mm)
     * --------------------------------------------------------------------- */
    function validateTimeInput(input) {
        const timeRegex = /^([01]\d|2[0-3]):([0-5]\d)$/;
        return timeRegex.test(input.value);
    }

    function updateEndTimeMin() {
        if (startTime.value) {
            endTime.min = startTime.value;
        }
    }

    /* ------------------------------------------------------------------------- 
     * Event Listeners for Time Inputs
     * --------------------------------------------------------------------- */
    startTime.addEventListener('change', function() {
        if (!validateTimeInput(this)) {
            document.getElementById('startTimeError').textContent = 'Please enter a valid time';
        } else {
            document.getElementById('startTimeError').textContent = '';
            updateEndTimeMin();
        }
    });

    endTime.addEventListener('change', function() {
        if (!validateTimeInput(this)) {
            document.getElementById('endTimeError').textContent = 'Please enter a valid time';
        } else if (this.value <= startTime.value) {
            document.getElementById('endTimeError').textContent = 'End time must be after start time';
        } else {
            document.getElementById('endTimeError').textContent = '';
        }
    });

    /* ------------------------------------------------------------------------- 
     * Form Submission Handler
     * @note Includes validation and error handling
     * --------------------------------------------------------------------- */
    form.addEventListener('submit', function(e) {
        e.preventDefault();
        
        // Reset error state
        document.querySelectorAll('.error').forEach(el => el.textContent = '');

        // Validate all inputs
        let isValid = true;
        if (!validateTimeInput(startTime)) {
            document.getElementById('startTimeError').textContent = 'Please enter a valid time';
            isValid = false;
        }
        if (!validateTimeInput(endTime)) {
            document.getElementById('endTimeError').textContent = 'Please enter a valid time';
            isValid = false;
        } else if (endTime.value <= startTime.value) {
            document.getElementById('endTimeError').textContent = 'End time must be after start time';
            isValid = false;
        }

        if (!isValid) return;

        // Prepare form data
        const formData = new FormData(form);
        const currentDate = new Date().toISOString().split('T')[0];
        const pacData = {
            store: formData.get('store'),
            startTime: `${currentDate}T${formData.get('startTime')}:00`,
            endTime: `${currentDate}T${formData.get('endTime')}:00`,
            pouchesChecked: parseInt(formData.get('pouchesChecked'))
        };

        // Submit data to server
        fetch('/submit-questions', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': csrfToken
            },
            body: JSON.stringify(pacData)
        })
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.text();
        })
        .then(data => {
            successMessage.textContent = data;
            successMessage.style.display = 'block';
            form.reset();
            setTimeout(() => {
                successMessage.style.display = 'none';
            }, 5000);
        })
        .catch(error => {
            console.error('Error:', error);
            showErrorMessage('An error occurred. Please try again later.');
        });
    });

    /* ------------------------------------------------------------------------- 
     * Message Display System
     * @note Handles both success and error messages
     * --------------------------------------------------------------------- */
    function showErrorMessage(message) {
        const errorDiv = document.getElementById('errorMessage');
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
        setTimeout(() => {
            errorDiv.style.display = 'none';
        }, 5000);
    }

    function showMessage(message, type = 'error') {
        const messageDiv = document.getElementById(`${type}Message`);
        if (!messageDiv) return;
        
        messageDiv.textContent = message;
        messageDiv.style.display = 'block';
        setTimeout(() => {
            messageDiv.style.display = 'none';
        }, 5000);
    }

    /* ------------------------------------------------------------------------- 
     * Form Validation System
     * --------------------------------------------------------------------- */
    function validateForm() {
        let isValid = true;
        const pouches = document.getElementsByName('pouchesChecked')[0].value;

        if (isNaN(pouches) || pouches < 0) {
            document.getElementById('pouchesError').textContent = 'Please enter a valid number';
            isValid = false;
        } else {
            document.getElementById('pouchesError').textContent = '';
        }

        return isValid;
    }

    /* ------------------------------------------------------------------------- 
     * Data Fetching and Dashboard Updates
     * --------------------------------------------------------------------- */
    function fetchOverallProductivity() {
        console.log('Fetching productivity data...');
        fetch('/api/overall-productivity')
            .then(response => {
                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
                return response.json();
            })
            .then(data => {
                updateDashboard(data);
                if (data.chartData) createPacMedChart(data.chartData);
            })
            .catch(error => console.error('Error fetching overall productivity:', error));
    }

    function updateDashboard(data) {
        document.getElementById('totalSubmissions').textContent = data.totalSubmissions ?? 'N/A';
        document.getElementById('avgTimePerPouch').textContent = 
            data.avgTimePerPouch != null ? formatDuration(data.avgTimePerPouch) : 'N/A';
        document.getElementById('avgPouchesPerHour').textContent = 
            data.avgPouchesPerHour != null ? data.avgPouchesPerHour.toFixed(2) : 'N/A';
        document.getElementById('totalPouchesChecked').textContent = data.totalPouchesChecked ?? 'N/A';
    }

    function formatDuration(seconds) {
        const minutes = Math.floor(seconds / 60);
        const remainingSeconds = Math.round(seconds % 60);
        return `${minutes}m ${remainingSeconds.toString().padStart(2, '0')}s`;
    }

    /* ------------------------------------------------------------------------- 
     * Server-Sent Events (SSE) Connection
     * @note Maintains real-time updates with automatic reconnection
     * --------------------------------------------------------------------- */
    let eventSource = null;

    function setupSSEConnection() {
        if (eventSource) eventSource.close();
        
        eventSource = new EventSource('/api/overall-productivity-stream');
        eventSource.onmessage = function(event) {
            try {
                const data = JSON.parse(event.data);
                if (Array.isArray(data)) {
                    // Handle individual updates
                } else {
                    updateDashboard(data);
                }
            } catch (error) {
                console.error('Error parsing SSE data:', error);
            }
        };

        eventSource.onerror = function(error) {
            console.error('Error in SSE connection:', error);
            eventSource.close();
            setTimeout(setupSSEConnection, 5000);
        };
    }

    /* ------------------------------------------------------------------------- 
     * Real-time Form Validation
     * @note Uses debouncing to prevent excessive validation calls
     * --------------------------------------------------------------------- */
    function validateInput(input) {
        const errorId = `${input.id}Error`;
        const errorElement = document.getElementById(errorId) || createErrorElement(input, errorId);
        if (!errorElement) return;

        if (input.validity.valid) {
            errorElement.textContent = '';
            input.classList.remove('invalid');
        } else {
            const errorMessage = getErrorMessage(input);
            errorElement.textContent = errorMessage;
            input.classList.add('invalid');
        }
    }

    function createErrorElement(input, errorId) {
        if (!input.parentElement) return null;
        const newErrorElement = document.createElement('div');
        newErrorElement.id = errorId;
        newErrorElement.className = 'error';
        input.parentElement.appendChild(newErrorElement);
        return newErrorElement;
    }

    function getErrorMessage(input) {
        switch(input.id) {
            case 'startTime':
            case 'endTime':
                return 'Please enter a valid time';
            case 'store':
                return 'Please select a store';
            case 'pouchesChecked':
                return 'Please enter a valid number';
            default:
                return input.validationMessage;
        }
    }

    function debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    function addRealTimeValidation() {
        const form = document.getElementById('questionForm');
        if (!form) return;

        const inputs = form.querySelectorAll('input, select');
        const debouncedValidate = debounce(validateInput, 300);
        
        inputs.forEach(input => {
            input.addEventListener('input', function() {
                debouncedValidate(this);
            });
        });
    }

    /* ------------------------------------------------------------------------- 
     * Chart Rendering
     * @note Uses Chart.js for visualization
     * --------------------------------------------------------------------- */
    let pacMedChart = null;

    function createPacMedChart(data) {
        if (!data?.labels || !data?.pouchesChecked) {
            console.error('Invalid chart data');
            return;
        }

        const ctx = document.getElementById('pacMedChart');
        if (!ctx) {
            console.error('Canvas element not found');
            return;
        }

        pacMedChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: data.labels,
                datasets: [{
                    label: 'Pouches Checked',
                    data: data.pouchesChecked,
                    borderColor: 'rgb(75, 192, 192)',
                    tension: 0.1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: { beginAtZero: true }
                },
                plugins: {
                    title: {
                        display: true,
                        text: 'Pouches Checked (Last 7 Days)',
                        font: { size: 16 }
                    },
                    subtitle: {
                        display: true,
                        text: data.pouchesChecked.every(val => val === 0) ? 
                              'Sample data shown (no actual data available)' : '',
                        color: 'red',
                        font: { size: 14, style: 'italic' }
                    }
                }
            }
        });
    }

    /* ------------------------------------------------------------------------- 
     * Initialization and Cleanup
     * --------------------------------------------------------------------- */
    fetchOverallProductivity();
    setupSSEConnection();
    addRealTimeValidation();

    // Cleanup on page unload
    window.addEventListener('beforeunload', () => {
        if (eventSource) eventSource.close();
    });
});