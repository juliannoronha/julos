/* =============================================================================
 * Wellca Management Module
 * 
 * PURPOSE: Handles tab navigation, form submissions, and dynamic calculations
 * for the Wellca management system
 * ============================================================================= */

import wellcaApi from './services/wellca-api.js';
import { 
    INPUT_FIELDS, 
    CSS_CLASSES, 
    DISPLAY_IDS, 
    DATE_CONFIG,
    DEFAULT_VALUES,
    TIME_CONSTANTS
} from './config/wellcaconstants.js';

let messageContainer;
let reportChart = null;

document.addEventListener('DOMContentLoaded', function() {
    setupDeliveryForm();
    setupRxSalesForm();
    setupServicesForm();
    initializeTabs();
    setupFormHandlers();
    
    // Fix for date input initialization
    const dateInput = document.getElementById(INPUT_FIELDS.DATE);
    if (dateInput) {
        // Get today's date and set it at noon to avoid timezone issues
        const today = new Date();
        today.setHours(12, 0, 0, 0);
        dateInput.value = today.toISOString().split(DATE_CONFIG.ISO_FORMAT)[0];
    }

    // Initialize start and end dates for reports
    const startDateInput = document.getElementById(INPUT_FIELDS.START_DATE);
    const endDateInput = document.getElementById(INPUT_FIELDS.END_DATE);
    if (startDateInput && endDateInput) {
        const today = new Date();
        today.setHours(12, 0, 0, 0);
        const formattedDate = today.toISOString().split(DATE_CONFIG.ISO_FORMAT)[0];
        startDateInput.value = formattedDate;
        endDateInput.value = formattedDate;
    }

    if (!messageContainer) {
        messageContainer = document.createElement('div');
        messageContainer.className = CSS_CLASSES.MESSAGE_CONTAINER;
        document.body.appendChild(messageContainer);
    }

    // Initialize chart when reports tab is shown
    const reportsTab = document.querySelector(`[${TAB_CONFIG.DATA_ATTRIBUTE}="${TAB_CONFIG.REPORTS_ID}"]`);
    if (reportsTab) {
        reportsTab.addEventListener('click', function() {
            if (!reportChart) {
                initializeChart();
            }
        });
    }
});

/* ------------------------------------------------------------------------- 
 * Tab Navigation Functions
 * --------------------------------------------------------------------- */
function initializeTabs() {
    const tabButtons = document.querySelectorAll(TAB_CONFIG.BUTTON_SELECTOR);
    const tabPanes = document.querySelectorAll(TAB_CONFIG.PANE_SELECTOR);
    
    // Hide all tabs except the first one
    tabPanes.forEach((pane, index) => {
        if (index !== 0) {
            pane.style.display = 'none';
        }
    });

    // Add click handlers to tab buttons
    tabButtons.forEach(button => {
        button.addEventListener('click', (e) => {
            // Remove active class from all buttons and panes
            tabButtons.forEach(btn => btn.classList.remove(CSS_CLASSES.ACTIVE));
            tabPanes.forEach(pane => pane.style.display = 'none');

            // Add active class to clicked button
            button.classList.add(CSS_CLASSES.ACTIVE);

            // Show corresponding pane
            const tabId = button.getAttribute(TAB_CONFIG.DATA_ATTRIBUTE);
            const pane = document.getElementById(tabId);
            if (pane) {
                pane.style.display = 'block';
                
                // If switching to reports tab, refresh the data
                if (tabId === TAB_CONFIG.REPORTS_ID) {
                    refreshReportData();
                }
            }
        });
    });
}

/* ------------------------------------------------------------------------- 
 * Form Setup and Handlers
 * --------------------------------------------------------------------- */
function setupFormHandlers() {
    // Setup delivery form calculations
    setupDeliveryCalculations();
    
    // Setup RX sales calculations
    setupRxCalculations();
    
    // Setup profiles calculations
    setupProfilesCalculations();
    
    // Setup services form
    setupServicesForm();
}

function setupDeliveryCalculations() {
    const deliveryInputs = INPUT_FIELDS.DELIVERY;
    
    deliveryInputs.forEach(id => {
        document.getElementById(id)?.addEventListener('input', () => {
            const total = deliveryInputs
                .map(input => parseInt(document.getElementById(input)?.value) || DEFAULT_VALUES.NUMERIC_FIELDS)
                .reduce((sum, current) => sum + current, 0);
            
            document.getElementById(DISPLAY_IDS.TOTAL_DELIVERIES).textContent = total;
        });
    });
}

function setupRxCalculations() {
    const rxInputs = INPUT_FIELDS.RX_SALES;
    
    rxInputs.forEach(id => {
        document.getElementById(id)?.addEventListener('input', () => {
            calculateRxTotals();
        });
    });
}

function calculateRxTotals() {
    const newRx = parseInt(document.getElementById(INPUT_FIELDS.RX_SALES[0])?.value) || DEFAULT_VALUES.NUMERIC_FIELDS;
    const refill = parseInt(document.getElementById(INPUT_FIELDS.RX_SALES[1])?.value) || DEFAULT_VALUES.NUMERIC_FIELDS;
    const reAuth = parseInt(document.getElementById(INPUT_FIELDS.RX_SALES[2])?.value) || DEFAULT_VALUES.NUMERIC_FIELDS;
    const hold = parseInt(document.getElementById(INPUT_FIELDS.RX_SALES[3])?.value) || DEFAULT_VALUES.NUMERIC_FIELDS;

    const totalFilled = newRx + refill + reAuth;
    const totalEntered = totalFilled + hold;

    document.getElementById(DISPLAY_IDS.TOTAL_FILLED).textContent = totalFilled;
    document.getElementById(DISPLAY_IDS.TOTAL_ENTERED).textContent = totalEntered;
    
    // Calculate per hour (using TIME_CONSTANTS.WORK_HOURS_PER_DAY)
    const perHour = (totalEntered / TIME_CONSTANTS.WORK_HOURS_PER_DAY).toFixed(2);
    document.getElementById(DISPLAY_IDS.TOTAL_PER_HOUR).textContent = perHour;
}

function setupProfilesCalculations() {
    INPUT_FIELDS.PROFILES.forEach(id => {
        document.getElementById(id)?.addEventListener('input', calculateActivePercentage);
    });
}

function calculateActivePercentage() {
    const profilesEntered = parseInt(document.getElementById(INPUT_FIELDS.PROFILES[0])?.value) || DEFAULT_VALUES.NUMERIC_FIELDS;
    const whoFilledRx = parseInt(document.getElementById(INPUT_FIELDS.PROFILES[1])?.value) || DEFAULT_VALUES.NUMERIC_FIELDS;
    
    if (profilesEntered > 0) {
        const percentage = (whoFilledRx / profilesEntered * 100).toFixed(2);
        document.getElementById(DISPLAY_IDS.ACTIVE_PERCENTAGE).value = percentage;
    }
}

function setupServicesForm() {
    const servicesForm = document.getElementById(FORM_IDS.SERVICES);
    if (servicesForm) {
        // Remove any existing event listeners
        const clonedForm = servicesForm.cloneNode(true);
        servicesForm.parentNode.replaceChild(clonedForm, servicesForm);
        
        clonedForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            e.stopPropagation();
            
            const submitButton = clonedForm.querySelector('button[type="submit"]');
            if (submitButton) {
                submitButton.disabled = true;
            }

            try {
                // Get form values with enhanced validation
                const serviceType = document.getElementById(INPUT_FIELDS.SERVICES[0]).value;
                const serviceCost = parseFloat(document.getElementById(INPUT_FIELDS.SERVICES[1]).value) || DEFAULT_VALUES.SERVICE_COST;
                const patientName = document.getElementById(INPUT_FIELDS.SERVICES[2]).value;
                const patientDob = document.getElementById(INPUT_FIELDS.SERVICES[3]).value;
                const pharmacistName = document.getElementById(INPUT_FIELDS.SERVICES[4]).value;

                if (!serviceType || !serviceCost || !patientName || !patientDob || !pharmacistName) {
                    console.error('Service validation failed: missing required fields');
                    showMessage(VALIDATION_MESSAGES.REQUIRED_FIELDS, MESSAGE_TYPES.ERROR);
                    return;
                }

                const formData = {
                    date: document.getElementById(INPUT_FIELDS.DATE).value,
                    serviceType: serviceType,
                    serviceCost: serviceCost,
                    patientName: patientName,
                    patientDob: patientDob,
                    pharmacistName: pharmacistName,
                    // Initialize other fields with default values
                    purolator: DEFAULT_VALUES.NUMERIC_FIELDS,
                    fedex: DEFAULT_VALUES.NUMERIC_FIELDS,
                    oneCourier: DEFAULT_VALUES.NUMERIC_FIELDS,
                    goBolt: DEFAULT_VALUES.NUMERIC_FIELDS,
                    newRx: DEFAULT_VALUES.NUMERIC_FIELDS,
                    refill: DEFAULT_VALUES.NUMERIC_FIELDS,
                    reAuth: DEFAULT_VALUES.NUMERIC_FIELDS,
                    hold: DEFAULT_VALUES.NUMERIC_FIELDS,
                    profilesEntered: DEFAULT_VALUES.NUMERIC_FIELDS,
                    whoFilledRx: DEFAULT_VALUES.NUMERIC_FIELDS,
                    activePercentage: DEFAULT_VALUES.ACTIVE_PERCENTAGE
                };

                console.log('Validating Professional Services input:', {
                    serviceType,
                    serviceCost,
                    patientName,
                    patientDob,
                    pharmacistName
                });

                const response = await wellcaApi.submitForm(formData);
                console.log('Professional Services submission response:', response);

                showMessage(VALIDATION_MESSAGES.SUBMISSION_SUCCESS, MESSAGE_TYPES.SUCCESS);
                clonedForm.reset();

                if (submitButton) {
                    submitButton.disabled = false;
                }

                // Refresh the report data
                await refreshReportData();
                generateReport();

            } catch (error) {
                console.error('Error submitting Professional Services data:', error);
                showMessage(VALIDATION_MESSAGES.SUBMISSION_ERROR + error.message, MESSAGE_TYPES.ERROR);
                
                if (submitButton) {
                    submitButton.disabled = false;
                }
            }
        });
    } else {
        console.error('Services form not found in DOM');
    }
}

/* ------------------------------------------------------------------------- 
 * Form Submission Handlers
 * --------------------------------------------------------------------- */
async function handleServiceSubmission(e) {
    e.preventDefault();
    
    const serviceData = {
        date: document.getElementById(INPUT_FIELDS.DATE).value,
        serviceType: document.getElementById(INPUT_FIELDS.SERVICES[0]).value,
        serviceCost: parseFloat(document.getElementById(INPUT_FIELDS.SERVICES[1]).value) || DEFAULT_VALUES.SERVICE_COST
    };

    try {
        const response = await wellcaApi.submitServiceData(serviceData);
        showMessage(VALIDATION_MESSAGES.SERVICE_ADDED, MESSAGE_TYPES.SUCCESS);
        updateServicesList();
    } catch (error) {
        showMessage(error.message, MESSAGE_TYPES.ERROR);
    }
}

/* ------------------------------------------------------------------------- 
 * Utility Functions
 * --------------------------------------------------------------------- */
function showMessage(message, type = MESSAGE_TYPES.SUCCESS) {
    const messageElement = document.createElement('div');
    messageElement.className = `${CSS_CLASSES.MESSAGE_BUBBLE} ${type}-bubble ${CSS_CLASSES.SLIDE_IN_FADE}`;
    messageElement.textContent = message;
    
    messageContainer.appendChild(messageElement);

    setTimeout(() => {
        messageElement.classList.add(CSS_CLASSES.FADE_OUT);
        setTimeout(() => {
            messageContainer.removeChild(messageElement);
        }, TIME_CONSTANTS.ANIMATION_DURATION);
    }, TIME_CONSTANTS.MESSAGE_DISPLAY_DURATION);
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    try {
        const date = new Date(`${dateString}${DATE_CONFIG.ISO_FORMAT}`);
        return date.toLocaleDateString(undefined, DATE_CONFIG.DISPLAY_FORMAT);
    } catch (error) {
        console.error('Date formatting error:', error);
        return 'Invalid Date';
    }
}

function formatServiceType(type) {
    return type ? type.replace(/_/g, ' ').toLowerCase()
        .replace(/\b\w/g, char => char.toUpperCase()) : 'N/A';
}

/* ------------------------------------------------------------------------- 
 * API Calls
 * --------------------------------------------------------------------- */
async function refreshReportData() {
    try {
        const startDateInput = document.getElementById(INPUT_FIELDS.START_DATE);
        const endDateInput = document.getElementById(INPUT_FIELDS.END_DATE);
        
        if (!startDateInput.value || !endDateInput.value) {
            showMessage(VALIDATION_MESSAGES.INVALID_DATE_RANGE, MESSAGE_TYPES.ERROR);
            return;
        }

        const data = await wellcaApi.getReportData(
            startDateInput.value,
            endDateInput.value
        );
        
        updateReportDisplay(data);
    } catch (error) {
        console.error('Error refreshing report data:', error);
        showMessage(VALIDATION_MESSAGES.REPORT_ERROR + error.message, MESSAGE_TYPES.ERROR);
    }
}

// Add date validation function
function validateDateRange() {
    const startDateInput = document.getElementById(INPUT_FIELDS.START_DATE).value;
    const endDateInput = document.getElementById(INPUT_FIELDS.END_DATE).value;
    
    // Create dates at noon to avoid timezone issues
    const startDate = new Date(startDateInput + DATE_CONFIG.ISO_FORMAT);
    const endDate = new Date(endDateInput + DATE_CONFIG.ISO_FORMAT);
    
    if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) {
        showMessage(VALIDATION_MESSAGES.INVALID_DATE_RANGE, MESSAGE_TYPES.ERROR);
        return false;
    }
    
    if (endDate < startDate) {
        showMessage(VALIDATION_MESSAGES.INVALID_DATE_RANGE, MESSAGE_TYPES.ERROR);
        return false;
    }
    
    return true;
}

async function generateReport() {
    console.log('Generating report...');
    
    // Validate dates before proceeding
    if (!validateDateRange()) {
        return;
    }

    try {
        // Show loading state
        const generateButton = document.querySelector('.generate-button');
        const originalText = generateButton.textContent;
        generateButton.textContent = 'Generating...';
        generateButton.disabled = true;
        generateButton.classList.add(CSS_CLASSES.LOADING);

        // Refresh the report data
        await refreshReportData();

        // Update UI elements
        document.querySelectorAll('.report-section').forEach(section => {
            section.style.display = 'block';
        });

        // Reset button state
        generateButton.textContent = originalText;
        generateButton.disabled = false;
        generateButton.classList.remove(CSS_CLASSES.LOADING);

        // Show success message
        showMessage(VALIDATION_MESSAGES.SUBMISSION_SUCCESS, MESSAGE_TYPES.SUCCESS);

    } catch (error) {
        console.error('Error generating report:', error);
        showMessage(VALIDATION_MESSAGES.REPORT_ERROR + error.message, MESSAGE_TYPES.ERROR);
        
        // Reset button state
        const generateButton = document.querySelector('.generate-button');
        generateButton.textContent = 'Generate Report';
        generateButton.disabled = false;
        generateButton.classList.remove(CSS_CLASSES.LOADING);
    }
}

// Helper function to update the report display with the fetched data
function updateReportDisplay(data) {
    try {
        // Prepare chart data
        const chartData = {
            labels: [],
            datasets: {
                rxCount: [],
                deliveries: [],
                rxPerDelivery: [],
                services: []
            }
        };

        // Process data for chart
        if (data.length > 0) {
            // Create a map to store service counts by date
            const serviceCountsByDate = new Map();
            
            // First pass: Count services for each date
            data.forEach(entry => {
                if (entry.serviceType) {
                    const dateKey = new Date(entry.date + DATE_CONFIG.ISO_FORMAT).toLocaleDateString();
                    serviceCountsByDate.set(dateKey, (serviceCountsByDate.get(dateKey) || 0) + 1);
                }
            });

            // Second pass: Process all data for chart
            data.forEach(entry => {
                // Add date label with consistent timezone handling
                const date = new Date(entry.date + DATE_CONFIG.ISO_FORMAT);
                const dateKey = date.toLocaleDateString();
                chartData.labels.push(dateKey);
                
                // Calculate totals for each metric
                const totalRx = (entry.newRx || DEFAULT_VALUES.NUMERIC_FIELDS) + 
                               (entry.refill || DEFAULT_VALUES.NUMERIC_FIELDS) + 
                               (entry.reAuth || DEFAULT_VALUES.NUMERIC_FIELDS);
                               
                const totalDeliveries = (entry.purolator || DEFAULT_VALUES.NUMERIC_FIELDS) + 
                                      (entry.fedex || DEFAULT_VALUES.NUMERIC_FIELDS) + 
                                      (entry.oneCourier || DEFAULT_VALUES.NUMERIC_FIELDS) + 
                                      (entry.goBolt || DEFAULT_VALUES.NUMERIC_FIELDS);
                
                // Get actual service count for this date
                const servicesCount = serviceCountsByDate.get(dateKey) || DEFAULT_VALUES.NUMERIC_FIELDS;
                
                // Add data points
                chartData.datasets.rxCount.push(totalRx);
                chartData.datasets.deliveries.push(totalDeliveries);
                chartData.datasets.rxPerDelivery.push(
                    totalDeliveries > 0 ? totalRx / totalDeliveries : DEFAULT_VALUES.NUMERIC_FIELDS
                );
                chartData.datasets.services.push(servicesCount);
            });
        }

        // Update the chart
        updateChart(chartData);

        // Update Delivery Statistics
        if (data.length > 0) {
            let totalPurolator = 0, totalFedex = 0, totalOneCourier = 0, totalGoBolt = 0;
            
            data.forEach(entry => {
                totalPurolator += entry.purolator || DEFAULT_VALUES.NUMERIC_FIELDS;
                totalFedex += entry.fedex || DEFAULT_VALUES.NUMERIC_FIELDS;
                totalOneCourier += entry.oneCourier || DEFAULT_VALUES.NUMERIC_FIELDS;
                totalGoBolt += entry.goBolt || DEFAULT_VALUES.NUMERIC_FIELDS;
            });

            const deliveryElements = {
                'totalPurolator': totalPurolator,
                'totalFedex': totalFedex,
                'totalOneCourier': totalOneCourier,
                'totalGoBolt': totalGoBolt,
                'reportTotalDeliveries': totalPurolator + totalFedex + totalOneCourier + totalGoBolt
            };

            Object.entries(deliveryElements).forEach(([id, value]) => {
                const element = document.getElementById(id);
                if (element) {
                    element.textContent = value;
                }
            });
        }

        // Make report sections visible
        document.querySelectorAll('.report-section').forEach(section => {
            section.style.display = 'block';
        });

    } catch (error) {
        console.error('Error updating report display:', error);
        showMessage(VALIDATION_MESSAGES.REPORT_ERROR + error.message, MESSAGE_TYPES.ERROR);
    }
}

function updateChart(data) {
    const ctx = document.getElementById(DISPLAY_IDS.REPORT_CHART)?.getContext('2d');
    if (!ctx) return;

    if (reportChart) {
        reportChart.destroy();
    }

    reportChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: data.labels,
            datasets: CHART_CONFIG.DATASETS.map((config, index) => ({
                ...config,
                data: Object.values(data.datasets)[index],
                fill: false,
                tension: 0.4
            }))
        },
        options: CHART_CONFIG.OPTIONS
    });
}

// Export any functions that might be needed by other modules
export {
    initializeTabs,
    setupFormHandlers,
    refreshReportData,
    generateReport,
    showMessage,
    formatDate,
    formatServiceType,
    updateReportDisplay,
    updateChart
};
