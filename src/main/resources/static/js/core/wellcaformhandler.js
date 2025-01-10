/* =============================================================================
 * Wellca Form Handler Module
 * 
 * PURPOSE: Handles form submissions and calculations for the Wellca management system
 * ============================================================================= */

import wellcaApi from '../services/wellca-api.js';
import { 
    INPUT_FIELDS, 
    CSS_CLASSES, 
    DISPLAY_IDS, 
    DATE_CONFIG,
    DEFAULT_VALUES,
    TIME_CONSTANTS,
    FORM_IDS,
    VALIDATION_MESSAGES,
    MESSAGE_TYPES
} from '../config/wellcaconstants.js';
import { 
    showMessage, 
    showErrorMessage 
} from '../services/wellcamessage.js';

/* ------------------------------------------------------------------------- 
 * Form Setup and Handlers
 * --------------------------------------------------------------------- */
export function setupFormHandlers() {
    setupDeliveryCalculations();
    setupRxCalculations();
    setupProfilesCalculations();
    setupServicesForm();

    // Add form submission handlers
    const deliveryForm = document.getElementById(FORM_IDS.DELIVERY);
    const rxForm = document.getElementById('rxSalesForm');
    
    if (deliveryForm) {
        deliveryForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const submitButton = deliveryForm.querySelector('button[type="submit"]');
            if (submitButton) submitButton.disabled = true;
            
            try {
                const formData = {
                    date: document.getElementById(INPUT_FIELDS.DATE).value,
                    // Delivery fields
                    purolator: parseInt(document.getElementById(INPUT_FIELDS.DELIVERY[0]).value) || 0,
                    fedex: parseInt(document.getElementById(INPUT_FIELDS.DELIVERY[1]).value) || 0,
                    oneCourier: parseInt(document.getElementById(INPUT_FIELDS.DELIVERY[2]).value) || 0,
                    goBolt: parseInt(document.getElementById(INPUT_FIELDS.DELIVERY[3]).value) || 0,
                    // Initialize RX fields with defaults
                    newRx: DEFAULT_VALUES.NUMERIC_FIELDS,
                    refill: DEFAULT_VALUES.NUMERIC_FIELDS,
                    reAuth: DEFAULT_VALUES.NUMERIC_FIELDS,
                    hold: DEFAULT_VALUES.NUMERIC_FIELDS,
                    // Initialize profile fields with defaults
                    profilesEntered: DEFAULT_VALUES.NUMERIC_FIELDS,
                    whoFilledRx: DEFAULT_VALUES.NUMERIC_FIELDS,
                    activePercentage: DEFAULT_VALUES.ACTIVE_PERCENTAGE,
                    // Initialize service fields with defaults
                    serviceType: '',
                    serviceCost: DEFAULT_VALUES.SERVICE_COST,
                    patientName: '',
                    patientDob: '',
                    pharmacistName: ''
                };
                
                await wellcaApi.submitForm(formData);
                showMessage(VALIDATION_MESSAGES.SUBMISSION_SUCCESS, MESSAGE_TYPES.SUCCESS);
                deliveryForm.reset();
            } catch (error) {
                showMessage(VALIDATION_MESSAGES.SUBMISSION_ERROR + error.message, MESSAGE_TYPES.ERROR);
            } finally {
                if (submitButton) submitButton.disabled = false;
            }
        });
    }

    if (rxForm) {
        rxForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            e.stopPropagation();
            console.log('RX form submission intercepted');
            
            const submitButton = rxForm.querySelector('button[type="submit"]');
            if (submitButton) submitButton.disabled = true;
            
            try {
                const formData = {
                    date: document.getElementById(INPUT_FIELDS.DATE).value,
                    // RX fields - using RX_SALES instead of RX
                    newRx: parseInt(document.getElementById(INPUT_FIELDS.RX_SALES[0]).value) || 0,
                    refill: parseInt(document.getElementById(INPUT_FIELDS.RX_SALES[1]).value) || 0,
                    reAuth: parseInt(document.getElementById(INPUT_FIELDS.RX_SALES[2]).value) || 0,
                    hold: parseInt(document.getElementById(INPUT_FIELDS.RX_SALES[3]).value) || 0,
                    // Initialize delivery fields with defaults
                    purolator: DEFAULT_VALUES.NUMERIC_FIELDS,
                    fedex: DEFAULT_VALUES.NUMERIC_FIELDS,
                    oneCourier: DEFAULT_VALUES.NUMERIC_FIELDS,
                    goBolt: DEFAULT_VALUES.NUMERIC_FIELDS,
                    // Initialize profile fields with defaults
                    profilesEntered: DEFAULT_VALUES.NUMERIC_FIELDS,
                    whoFilledRx: DEFAULT_VALUES.NUMERIC_FIELDS,
                    activePercentage: DEFAULT_VALUES.ACTIVE_PERCENTAGE,
                    // Initialize service fields with defaults
                    serviceType: '',
                    serviceCost: DEFAULT_VALUES.SERVICE_COST,
                    patientName: '',
                    patientDob: '',
                    pharmacistName: ''
                };
                
                console.log('Submitting RX form data:', formData);
                await wellcaApi.submitForm(formData);
                console.log('RX form submission successful');
                showMessage(VALIDATION_MESSAGES.SUBMISSION_SUCCESS, MESSAGE_TYPES.SUCCESS);
                rxForm.reset();
            } catch (error) {
                console.error('RX form submission error:', error);
                showMessage(VALIDATION_MESSAGES.SUBMISSION_ERROR + error.message, MESSAGE_TYPES.ERROR);
            } finally {
                if (submitButton) submitButton.disabled = false;
            }
        });
    }
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
    
    // Calculate per hour
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
                    serviceType,
                    serviceCost,
                    patientName,
                    patientDob,
                    pharmacistName,
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

                const response = await wellcaApi.submitForm(formData);
                showMessage(VALIDATION_MESSAGES.SERVICE_ADDED, MESSAGE_TYPES.SUCCESS);
                clonedForm.reset();

                if (submitButton) {
                    submitButton.disabled = false;
                }

            } catch (error) {
                console.error('Error submitting service form:', error);
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

// Export form-related functions
export {
    setupDeliveryCalculations,
    setupRxCalculations,
    setupProfilesCalculations,
    setupServicesForm,
    calculateRxTotals,
    calculateActivePercentage
};
