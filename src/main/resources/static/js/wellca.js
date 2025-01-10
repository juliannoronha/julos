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
    TIME_CONSTANTS,
    TAB_CONFIG,
    MESSAGE_TYPES,
    VALIDATION_MESSAGES,
    CHART_CONFIG
} from './config/wellcaconstants.js';
import { setupFormHandlers } from './core/wellcaformhandler.js';
import { 
    showMessage, 
    initializeMessageContainer 
} from './services/wellcamessage.js';

let reportChart = null;

document.addEventListener('DOMContentLoaded', function() {
    // Initialize form handlers from the new module
    setupFormHandlers();
    
    // Initialize tabs
    initializeTabs();
    
    // Fix for date input initialization
    const dateInput = document.getElementById(INPUT_FIELDS.DATE);
    if (dateInput) {
        const today = new Date();
        today.setHours(12, 0, 0, 0);
        const formattedDate = today.toISOString().split('T')[0];
        dateInput.value = formattedDate;
    }

    // Initialize start and end dates for reports
    const startDateInput = document.getElementById(INPUT_FIELDS.START_DATE);
    const endDateInput = document.getElementById(INPUT_FIELDS.END_DATE);
    if (startDateInput && endDateInput) {
        const today = new Date();
        today.setHours(12, 0, 0, 0);
        const formattedDate = today.toISOString().split('T')[0];
        startDateInput.value = formattedDate;
        endDateInput.value = formattedDate;
    }

    // Initialize message container
    initializeMessageContainer();

    // Initialize chart when reports tab is shown
    const reportsTab = document.querySelector(`[${TAB_CONFIG.DATA_ATTRIBUTE}="${TAB_CONFIG.REPORTS_ID}"]`);
    if (reportsTab) {
        reportsTab.addEventListener('click', function() {
            if (!reportChart) {
                initializeChart();
            }
        });
    }

    // Add date range validation listeners
    if (startDateInput && endDateInput) {
        startDateInput.addEventListener('change', validateDateRange);
        endDateInput.addEventListener('change', validateDateRange);
    }
    
    // Add generate report listener
    const generateButton = document.querySelector('.generate-button');
    if (generateButton) {
        generateButton.addEventListener('click', generateReport);
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
                    refreshReportData().catch(error => {
                        showMessage(VALIDATION_MESSAGES.TAB_SWITCH_ERROR + error.message, MESSAGE_TYPES.ERROR);
                    });
                }
            } else {
                showMessage(VALIDATION_MESSAGES.TAB_NOT_FOUND, MESSAGE_TYPES.ERROR);
            }
        });
    });
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

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    try {
        const date = new Date(`${dateString}${DATE_CONFIG.ISO_FORMAT}`);
        return date.toLocaleDateString(undefined, DATE_CONFIG.DISPLAY_FORMAT);
    } catch (error) {
        console.error('Date formatting error:', error);
        showMessage(VALIDATION_MESSAGES.DATE_FORMAT_ERROR, MESSAGE_TYPES.WARNING);
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
    if (!validateDateRange()) {
        return;
    }

    try {
        const generateButton = document.querySelector('.generate-button');
        const originalText = generateButton.textContent;
        generateButton.textContent = 'Generating...';
        generateButton.disabled = true;
        generateButton.classList.add(CSS_CLASSES.LOADING);

        await refreshReportData();

        document.querySelectorAll('.report-section').forEach(section => {
            section.style.display = 'block';
        });

        generateButton.textContent = originalText;
        generateButton.disabled = false;
        generateButton.classList.remove(CSS_CLASSES.LOADING);

        showMessage(VALIDATION_MESSAGES.REPORT_GENERATED, MESSAGE_TYPES.SUCCESS);

    } catch (error) {
        console.error('Error generating report:', error);
        showMessage(VALIDATION_MESSAGES.REPORT_ERROR + error.message, MESSAGE_TYPES.ERROR);
        
        const generateButton = document.querySelector('.generate-button');
        generateButton.textContent = 'Generate Report';
        generateButton.disabled = false;
        generateButton.classList.remove(CSS_CLASSES.LOADING);
    }
}

/* ------------------------------------------------------------------------- 
 * Chart and Display Functions
 * --------------------------------------------------------------------- */
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
                
                const servicesCount = serviceCountsByDate.get(dateKey) || DEFAULT_VALUES.NUMERIC_FIELDS;
                
                chartData.datasets.rxCount.push(totalRx);
                chartData.datasets.deliveries.push(totalDeliveries);
                chartData.datasets.rxPerDelivery.push(
                    totalDeliveries > 0 ? totalRx / totalDeliveries : DEFAULT_VALUES.NUMERIC_FIELDS
                );
                chartData.datasets.services.push(servicesCount);
            });
        } else {
            showMessage(VALIDATION_MESSAGES.NO_DATA_AVAILABLE, MESSAGE_TYPES.WARNING);
        }

        updateChart(chartData);
        updateDeliveryStatistics(data);

    } catch (error) {
        console.error('Error updating report display:', error);
        showMessage(VALIDATION_MESSAGES.CHART_UPDATE_ERROR + error.message, MESSAGE_TYPES.ERROR);
    }
}

function updateDeliveryStatistics(data) {
    if (!data || data.length === 0) {
        showMessage(VALIDATION_MESSAGES.NO_DELIVERY_DATA, MESSAGE_TYPES.WARNING);
        return;
    }

    try {
        const totals = {
            purolator: 0,
            fedex: 0,
            oneCourier: 0,
            goBolt: 0
        };
        
        data.forEach(entry => {
            totals.purolator += entry.purolator || DEFAULT_VALUES.NUMERIC_FIELDS;
            totals.fedex += entry.fedex || DEFAULT_VALUES.NUMERIC_FIELDS;
            totals.oneCourier += entry.oneCourier || DEFAULT_VALUES.NUMERIC_FIELDS;
            totals.goBolt += entry.goBolt || DEFAULT_VALUES.NUMERIC_FIELDS;
        });

        const deliveryElements = {
            'totalPurolator': totals.purolator,
            'totalFedex': totals.fedex,
            'totalOneCourier': totals.oneCourier,
            'totalGoBolt': totals.goBolt,
            'reportTotalDeliveries': Object.values(totals).reduce((a, b) => a + b, 0)
        };

        Object.entries(deliveryElements).forEach(([id, value]) => {
            const element = document.getElementById(id);
            if (element) {
                element.textContent = value;
            } else {
                console.warn(`Element with id ${id} not found`);
            }
        });

    } catch (error) {
        console.error('Error updating delivery statistics:', error);
        showMessage(VALIDATION_MESSAGES.STATISTICS_UPDATE_ERROR + error.message, MESSAGE_TYPES.ERROR);
    }
}

function updateChart(data) {
    const ctx = document.getElementById(DISPLAY_IDS.REPORT_CHART)?.getContext('2d');
    if (!ctx) {
        showMessage(VALIDATION_MESSAGES.CHART_CONTEXT_ERROR, MESSAGE_TYPES.ERROR);
        return;
    }

    try {
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
    } catch (error) {
        console.error('Error creating chart:', error);
        showMessage(VALIDATION_MESSAGES.CHART_CREATE_ERROR + error.message, MESSAGE_TYPES.ERROR);
    }
}

/* ------------------------------------------------------------------------- 
 * Chart Initialization
 * --------------------------------------------------------------------- */
function initializeChart() {
    const ctx = document.getElementById(DISPLAY_IDS.REPORT_CHART)?.getContext('2d');
    if (!ctx) {
        showMessage(VALIDATION_MESSAGES.CHART_CONTEXT_ERROR, MESSAGE_TYPES.ERROR);
        return;
    }

    reportChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: CHART_CONFIG.DATASETS.map(config => ({
                ...config,
                data: [],
                fill: false,
                tension: 0.4
            }))
        },
        options: CHART_CONFIG.OPTIONS
    });
}

/* ------------------------------------------------------------------------- 
 * Exports
 * --------------------------------------------------------------------- */
export {
    // Core UI Functions
    initializeTabs,
    showMessage,
    
    // Report Functions
    refreshReportData,
    generateReport,
    validateDateRange,
    
    // Chart Functions
    initializeChart,
    updateReportDisplay,
    updateChart,
    updateDeliveryStatistics,
    
    // Utility Functions
    formatDate,
    formatServiceType
};
