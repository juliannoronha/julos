/* =============================================================================
 * Wellca Constants
 * 
 * PURPOSE: Centralizes all constants used in the Wellca management system
 * ============================================================================= */

// Form IDs
export const FORM_IDS = {
    DELIVERY: 'deliveryForm',
    RX_SALES: 'rxSalesForm',
    SERVICES: 'servicesForm',
    PROFILES: 'profilesForm'
};

// Input Field IDs
export const INPUT_FIELDS = {
    DELIVERY: ['purolator', 'fedex', 'oneCourier', 'goBolt'],
    RX_SALES: ['newRx', 'refill', 'reAuth', 'hold'],
    PROFILES: ['profilesEntered', 'whoFilledRx'],
    SERVICES: ['serviceType', 'serviceCost', 'patientName', 'patientDob', 'pharmacistName'],
    DATE: 'date',
    START_DATE: 'startDate',
    END_DATE: 'endDate'
};

// Display Element IDs
export const DISPLAY_IDS = {
    TOTAL_DELIVERIES: 'totalDeliveries',
    TOTAL_FILLED: 'totalFilled',
    TOTAL_ENTERED: 'totalEntered',
    TOTAL_PER_HOUR: 'totalPerHour',
    ACTIVE_PERCENTAGE: 'activePercentage',
    SERVICE_BREAKDOWN: 'serviceBreakdown',
    TOTAL_SERVICES: 'totalServices',
    REPORT_CHART: 'reportChart',
    WEEKLY_TOTAL_PROFILES: 'weeklyTotalProfiles',
    WEEKLY_AVERAGE_ACTIVE: 'weeklyAverageActive'
};

// Message Types
export const MESSAGE_TYPES = {
    SUCCESS: 'success',
    ERROR: 'error',
    INFO: 'info',
    WARNING: 'warning'
};

// Chart Configuration
export const CHART_CONFIG = {
    COLORS: {
        RX_COUNT: '#4CAF50',
        DELIVERIES: '#2196F3',
        RX_PER_DELIVERY: '#FFC107',
        SERVICES: '#9C27B0'
    },
    OPTIONS: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
            y: {
                beginAtZero: true
            }
        }
    }
};

// Time Constants
export const TIME_CONSTANTS = {
    WORK_HOURS_PER_DAY: 8,
    MESSAGE_DISPLAY_DURATION: 3000,
    ANIMATION_DURATION: 300
};

// CSS Classes
export const CSS_CLASSES = {
    ACTIVE: 'active',
    LOADING: 'loading',
    MESSAGE_CONTAINER: 'message-container',
    MESSAGE_BUBBLE: 'message-bubble',
    SLIDE_IN_FADE: 'slide-in-fade',
    FADE_OUT: 'fade-out',
    SERVICE_TYPE_ROW: 'service-type-row',
    SERVICE_DETAILS: 'service-details',
    MONTH_SECTION: 'month-section',
    DETAIL_ITEM: 'detail-item'
};

// Tab Configuration
export const TAB_CONFIG = {
    BUTTON_SELECTOR: '.tab-button',
    PANE_SELECTOR: '.tab-pane',
    DATA_ATTRIBUTE: 'data-tab',
    REPORTS_ID: 'reports'
};

// Form Field Default Values
export const DEFAULT_VALUES = {
    NUMERIC_FIELDS: 0,
    SERVICE_TYPE: null,
    SERVICE_COST: 0,
    ACTIVE_PERCENTAGE: 0
};

// Date Format Configuration
export const DATE_CONFIG = {
    ISO_FORMAT: 'T12:00:00',
    DISPLAY_FORMAT: {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    }
};

// Validation Messages
export const VALIDATION_MESSAGES = {
    REQUIRED_FIELDS: 'Please fill in all required fields',
    INVALID_DATE_RANGE: 'Please select both start and end dates',
    SUBMISSION_SUCCESS: 'Successfully Submitted!',
    SUBMISSION_ERROR: 'Failed to save data: ',
    SERVICE_ADDED: 'Service added successfully',
    REPORT_ERROR: 'Error loading report: '
};

// Export all constants as a single object for convenience
export const WELLCA_CONSTANTS = {
    FORM_IDS,
    INPUT_FIELDS,
    DISPLAY_IDS,
    MESSAGE_TYPES,
    CHART_CONFIG,
    TIME_CONSTANTS,
    CSS_CLASSES,
    TAB_CONFIG,
    DEFAULT_VALUES,
    DATE_CONFIG,
    VALIDATION_MESSAGES
};

export default WELLCA_CONSTANTS;
