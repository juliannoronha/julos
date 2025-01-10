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
        today.setUTCHours(12, 0, 0, 0);
        const formattedDate = today.toISOString().split('T')[0];
        dateInput.value = formattedDate;
    }

    // Initialize start and end dates for reports
    const startDateInput = document.getElementById(INPUT_FIELDS.START_DATE);
    const endDateInput = document.getElementById(INPUT_FIELDS.END_DATE);
    if (startDateInput && endDateInput) {
        const today = new Date();
        today.setUTCHours(12, 0, 0, 0);
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
            try {
                if (!reportChart) {
                    console.log('Initializing chart...');
                    initializeChart();
                }
            } catch (error) {
                console.error('Error in reports tab click handler:', error);
                showMessage(VALIDATION_MESSAGES.CHART_ERROR + error.message, MESSAGE_TYPES.ERROR);
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
async function updateReportDisplay(data) {
    console.log('Received report data:', data);
    
    try {
        // Update delivery statistics
        const deliveryStats = {
            purolator: 0,
            fedex: 0,
            oneCourier: 0,
            goBolt: 0
        };

        // Update RX statistics
        const rxStats = {
            newRx: 0,
            refill: 0,
            reAuth: 0,
            hold: 0,
            totalProcessed: 0
        };

        // Process data
        data.forEach(entry => {
            console.log('Processing entry:', entry);
            
            // Delivery stats
            deliveryStats.purolator += entry.purolator || 0;
            deliveryStats.fedex += entry.fedex || 0;
            deliveryStats.oneCourier += entry.oneCourier || 0;
            deliveryStats.goBolt += entry.goBolt || 0;

            // RX stats
            rxStats.newRx += entry.newRx || 0;
            rxStats.refill += entry.refill || 0;
            rxStats.reAuth += entry.reAuth || 0;
            rxStats.hold += entry.hold || 0;
        });

        // Calculate total processed
        rxStats.totalProcessed = rxStats.newRx + rxStats.refill + rxStats.reAuth + rxStats.hold;

        console.log('Processed delivery stats:', deliveryStats);
        console.log('Processed RX stats:', rxStats);

        // Update delivery statistics display
        document.getElementById('totalPurolator').textContent = deliveryStats.purolator;
        document.getElementById('totalFedex').textContent = deliveryStats.fedex;
        document.getElementById('totalOneCourier').textContent = deliveryStats.oneCourier;
        document.getElementById('totalGoBolt').textContent = deliveryStats.goBolt;
        document.getElementById('reportTotalDeliveries').textContent = 
            deliveryStats.purolator + deliveryStats.fedex + deliveryStats.oneCourier + deliveryStats.goBolt;

        // Update RX statistics display
        document.getElementById('totalNewRx').textContent = rxStats.newRx;
        document.getElementById('totalRefills').textContent = rxStats.refill;
        document.getElementById('totalReAuth').textContent = rxStats.reAuth;
        document.getElementById('totalHold').textContent = rxStats.hold;
        document.getElementById('reportTotalProcessed').textContent = rxStats.totalProcessed;

        // Update chart with the processed data
        await updateChart(data);
    } catch (error) {
        console.error('Error updating report display:', error);
        showMessage(VALIDATION_MESSAGES.REPORT_DISPLAY_ERROR + error.message, MESSAGE_TYPES.ERROR);
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

async function updateChart(data) {
    try {
        console.log('Updating chart with data:', data);
        
        const chartData = {
            labels: [],
            rxCount: [],
            deliveries: [],
            rxPerDelivery: [],
            services: []
        };

        // Group data by date to avoid duplicates
        const groupedData = data.reduce((acc, entry) => {
            if (!acc[entry.date]) {
                acc[entry.date] = {
                    rxCount: 0,
                    deliveries: 0,
                    services: 0
                };
            }
            
            // Sum up the values for each date
            acc[entry.date].rxCount += (entry.newRx || 0) + (entry.refill || 0) + (entry.reAuth || 0);
            acc[entry.date].deliveries += (entry.purolator || 0) + (entry.fedex || 0) + 
                                        (entry.oneCourier || 0) + (entry.goBolt || 0);
            acc[entry.date].services += entry.serviceCost || 0;
            
            return acc;
        }, {});

        // Convert grouped data to arrays
        Object.entries(groupedData).forEach(([date, values]) => {
            // Create date at noon UTC
            const displayDate = new Date(date);
            displayDate.setUTCHours(12, 0, 0, 0);
            chartData.labels.push(displayDate.toLocaleDateString());
            chartData.rxCount.push(values.rxCount);
            chartData.deliveries.push(values.deliveries);
            chartData.rxPerDelivery.push(
                values.deliveries > 0 ? values.rxCount / values.deliveries : 0
            );
            chartData.services.push(values.services);
        });

        // Update chart data
        reportChart.data.labels = chartData.labels;
        reportChart.data.datasets[0].data = chartData.rxCount;
        reportChart.data.datasets[1].data = chartData.deliveries;
        reportChart.data.datasets[2].data = chartData.rxPerDelivery;
        reportChart.data.datasets[3].data = chartData.services;

        reportChart.update();
        console.log('Chart updated successfully');
    } catch (error) {
        console.error('Error updating chart:', error);
        showMessage(VALIDATION_MESSAGES.CHART_ERROR + error.message, MESSAGE_TYPES.ERROR);
    }
}

/* ------------------------------------------------------------------------- 
 * Chart Initialization
 * --------------------------------------------------------------------- */
function initializeChart() {
    try {
        const ctx = document.getElementById('reportChart').getContext('2d');
        if (!ctx) {
            console.error('Could not get chart context');
            return;
        }

        const chartColors = {
            RX_COUNT: '#4CAF50',
            DELIVERIES: '#2196F3',
            RX_PER_DELIVERY: '#FFC107',
            SERVICES: '#9C27B0'
        };

        reportChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [
                    {
                        label: 'RX Count',
                        borderColor: chartColors.RX_COUNT,
                        data: [],
                        fill: false
                    },
                    {
                        label: 'Deliveries',
                        borderColor: chartColors.DELIVERIES,
                        data: [],
                        fill: false
                    },
                    {
                        label: 'RX per Delivery',
                        borderColor: chartColors.RX_PER_DELIVERY,
                        data: [],
                        fill: false
                    },
                    {
                        label: 'Services Count',
                        borderColor: chartColors.SERVICES,
                        data: [],
                        fill: false
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
        
        console.log('Chart initialized successfully');
    } catch (error) {
        console.error('Error initializing chart:', error);
        showMessage(VALIDATION_MESSAGES.CHART_ERROR + error.message, MESSAGE_TYPES.ERROR);
    }
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

function standardizeDate(dateString) {
    // Create date at noon UTC to avoid timezone shifts
    const date = new Date(dateString);
    date.setUTCHours(12, 0, 0, 0);
    return date.toISOString().split('T')[0];
}
