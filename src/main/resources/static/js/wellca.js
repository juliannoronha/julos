/* =============================================================================
 * Wellca Management Module
 * 
 * PURPOSE: Handles tab navigation, form submissions, and dynamic calculations
 * for the Wellca management system
 * ============================================================================= */

let messageContainer;

document.addEventListener('DOMContentLoaded', function() {
    setupDeliveryForm();
    setupRxSalesForm();
    setupServicesForm();
    initializeTabs();
    setupFormHandlers();
    
    // Fix for date input initialization
    const dateInput = document.getElementById('date');
    if (dateInput) {
        // Get today's date and set it at noon to avoid timezone issues
        const today = new Date();
        today.setHours(12, 0, 0, 0);
        dateInput.value = today.toISOString().split('T')[0];
    }

    // Initialize start and end dates for reports
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');
    if (startDateInput && endDateInput) {
        const today = new Date();
        today.setHours(12, 0, 0, 0);
        const formattedDate = today.toISOString().split('T')[0];
        startDateInput.value = formattedDate;
        endDateInput.value = formattedDate;
    }

    if (!messageContainer) {
        messageContainer = document.createElement('div');
        messageContainer.className = 'message-container';
        document.body.appendChild(messageContainer);
    }
});

/* ------------------------------------------------------------------------- 
 * Tab Navigation Functions
 * --------------------------------------------------------------------- */
function initializeTabs() {
    const tabButtons = document.querySelectorAll('.tab-button');
    const tabPanes = document.querySelectorAll('.tab-pane');
    
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
            tabButtons.forEach(btn => btn.classList.remove('active'));
            tabPanes.forEach(pane => pane.style.display = 'none');

            // Add active class to clicked button
            button.classList.add('active');

            // Show corresponding pane
            const tabId = button.getAttribute('data-tab');
            const pane = document.getElementById(tabId);
            if (pane) {
                pane.style.display = 'block';
                
                // If switching to reports tab, refresh the data
                if (tabId === 'reports') {
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
    const deliveryInputs = ['purolator', 'fedex', 'oneCourier', 'goBolt'];
    
    deliveryInputs.forEach(id => {
        document.getElementById(id)?.addEventListener('input', () => {
            const total = deliveryInputs
                .map(input => parseInt(document.getElementById(input)?.value) || 0)
                .reduce((sum, current) => sum + current, 0);
            
            document.getElementById('totalDeliveries').textContent = total;
        });
    });
}

function setupRxCalculations() {
    const rxInputs = ['newRx', 'refill', 'reAuth', 'hold'];
    
    rxInputs.forEach(id => {
        document.getElementById(id)?.addEventListener('input', () => {
            calculateRxTotals();
        });
    });
}

function calculateRxTotals() {
    const newRx = parseInt(document.getElementById('newRx')?.value) || 0;
    const refill = parseInt(document.getElementById('refill')?.value) || 0;
    const reAuth = parseInt(document.getElementById('reAuth')?.value) || 0;
    const hold = parseInt(document.getElementById('hold')?.value) || 0;

    const totalFilled = newRx + refill + reAuth;
    const totalEntered = totalFilled + hold;

    document.getElementById('totalFilled').textContent = totalFilled;
    document.getElementById('totalEntered').textContent = totalEntered;
    
    // Calculate per hour (assuming 8-hour workday)
    const perHour = (totalEntered / 8).toFixed(2);
    document.getElementById('totalPerHour').textContent = perHour;
}

function setupProfilesCalculations() {
    ['profilesEntered', 'whoFilledRx'].forEach(id => {
        document.getElementById(id)?.addEventListener('input', calculateActivePercentage);
    });
}

function calculateActivePercentage() {
    const profilesEntered = parseInt(document.getElementById('profilesEntered')?.value) || 0;
    const whoFilledRx = parseInt(document.getElementById('whoFilledRx')?.value) || 0;
    
    if (profilesEntered > 0) {
        const percentage = (whoFilledRx / profilesEntered * 100).toFixed(2);
        document.getElementById('activePercentage').value = percentage;
    }
}

function setupServicesForm() {
    const servicesForm = document.getElementById('servicesForm');
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
                const serviceType = document.getElementById('serviceType').value;
                const serviceCost = parseFloat(document.getElementById('serviceCost').value) || 0;
                const patientName = document.getElementById('patientName').value;
                const patientDob = document.getElementById('patientDob').value;
                const pharmacistName = document.getElementById('pharmacistName').value;

                if (!serviceType || !serviceCost || !patientName || !patientDob || !pharmacistName) {
                    console.error('Service validation failed: missing required fields');
                    showMessage('Please fill in all required fields', 'error');
                    return;
                }

                const formData = {
                    date: document.getElementById('date').value,
                    serviceType: serviceType,
                    serviceCost: serviceCost,
                    patientName: patientName,
                    patientDob: patientDob,
                    pharmacistName: pharmacistName,
                    // Delivery fields
                    purolator: 0,
                    fedex: 0,
                    oneCourier: 0,
                    goBolt: 0,
                    // RX Sales fields
                    newRx: 0,
                    refill: 0,
                    reAuth: 0,
                    hold: 0,
                    // Profile fields
                    profilesEntered: 0,
                    whoFilledRx: 0,
                    activePercentage: 0
                };

                console.log('Validating Professional Services input:', {
                    serviceType,
                    serviceCost,
                    patientName,
                    patientDob,
                    pharmacistName
                });

                const response = await submitForm(formData);
                console.log('Professional Services submission response:', response);

                showMessage('Successfully Submitted!');
                clonedForm.reset();

                if (submitButton) {
                    submitButton.disabled = false;
                }

                // Refresh the report data
                await refreshReportData();
                generateReport();

            } catch (error) {
                console.error('Error submitting Professional Services data:', error);
                showMessage('Failed to save Professional Services data: ' + error.message, 'error');
                
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
        date: document.getElementById('date').value,
        serviceType: document.getElementById('serviceType').value,
        serviceCost: parseFloat(document.getElementById('serviceCost').value)
    };

    try {
        const response = await submitServiceData(serviceData);
        if (response.ok) {
            showSuccessMessage('Service added successfully');
            updateServicesList();
        } else {
            showErrorMessage('Error adding service');
        }
    } catch (error) {
        showErrorMessage('Error: ' + error.message);
    }
}

/* ------------------------------------------------------------------------- 
 * Utility Functions
 * --------------------------------------------------------------------- */
function showSuccessMessage(message) {
    const successDiv = document.getElementById('successMessage');
    if (successDiv) {
        successDiv.textContent = message;
        successDiv.style.display = 'block';
        setTimeout(() => successDiv.style.display = 'none', 3000);
    }
}

function showErrorMessage(message) {
    const errorDiv = document.getElementById('errorMessage');
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
        setTimeout(() => errorDiv.style.display = 'none', 3000);
    }
}

/* ------------------------------------------------------------------------- 
 * API Calls
 * --------------------------------------------------------------------- */
async function submitServiceData(data) {
    return fetch('/wellca-management/submit', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify(data)
    });
}

async function refreshReportData() {
    try {
        console.log('Refreshing report data');
        
        const startDateInput = document.getElementById('startDate');
        const endDateInput = document.getElementById('endDate');
        
        if (!startDateInput.value || !endDateInput.value) {
            showErrorMessage('Please select both start and end dates');
            return;
        }

        // Use the input values directly without Date object conversion
        const startDate = startDateInput.value;
        const endDate = endDateInput.value;
        
        console.log('Date range:', startDate, 'to', endDate);

        const response = await fetch(`/wellca-management/range?startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
                [csrfHeader]: csrfToken
            }
        });
        
        if (!response.ok) {
            const errorData = await response.text();
            console.error('Server response:', errorData);
            throw new Error('Failed to fetch report data');
        }

        const data = await response.json();
        console.log('Report data:', data);
        
        updateReportDisplay(data);
    } catch (error) {
        console.error('Error refreshing report data:', error);
        showErrorMessage(`Error loading report: ${error.message}`);
    }
}

async function submitForm(formData) {
    try {
        // Ensure all required fields are present and properly formatted
        const sanitizedData = {
            date: formData.date,
            // Service specific fields
            serviceType: formData.serviceType,
            serviceCost: Number(formData.serviceCost) || 0,
            // Patient info
            patientName: formData.patientName?.trim() || '',
            patientDob: formData.patientDob?.trim() || '',
            pharmacistName: formData.pharmacistName?.trim() || '',
            // Ensure all numeric fields are initialized to 0, not null
            purolator: Number(formData.purolator) || 0,
            fedex: Number(formData.fedex) || 0,
            oneCourier: Number(formData.oneCourier) || 0,
            goBolt: Number(formData.goBolt) || 0,
            newRx: Number(formData.newRx) || 0,
            refill: Number(formData.refill) || 0,
            reAuth: Number(formData.reAuth) || 0,
            hold: Number(formData.hold) || 0,
            profilesEntered: Number(formData.profilesEntered) || 0,
            whoFilledRx: Number(formData.whoFilledRx) || 0,
            activePercentage: Number(formData.activePercentage) || 0
        };

        console.log('Submitting sanitized form data:', JSON.stringify(sanitizedData, null, 2));

        const response = await fetch('/wellca-management/submit', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeader]: csrfToken
            },
            body: JSON.stringify(sanitizedData)
        });

        console.log('Response status:', response.status);
        
        if (!response.ok) {
            const errorText = await response.text();
            console.error('Server response:', errorText);
            throw new Error(`Submission failed: ${errorText}`);
        }

        const data = await response.json();
        console.log('Submission successful:', data);
        
        return data;
    } catch (error) {
        console.error('Error submitting form:', error);
        throw error;
    }
}

// Add date validation function
function validateDateRange() {
    const startDate = new Date(document.getElementById('startDate').value);
    const endDate = new Date(document.getElementById('endDate').value);
    
    if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) {
        showErrorMessage('Please select valid dates');
        return false;
    }
    
    if (endDate < startDate) {
        showErrorMessage('End date must be after start date');
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

        // Refresh the report data
        await refreshReportData();

        // Update UI elements
        document.querySelectorAll('.report-section').forEach(section => {
            section.style.display = 'block';
        });

        // Reset button state
        generateButton.textContent = originalText;
        generateButton.disabled = false;

        // Show success message
        showSuccessMessage('Report generated successfully');

    } catch (error) {
        console.error('Error generating report:', error);
        showErrorMessage('Failed to generate report: ' + error.message);
        
        // Reset button state
        const generateButton = document.querySelector('.generate-button');
        generateButton.textContent = 'Generate Report';
        generateButton.disabled = false;
    }
}

// Helper function to update the report display with the fetched data
function updateReportDisplay(data) {
    try {
        // Update Delivery Statistics
        if (data.length > 0) {
            let totalPurolator = 0, totalFedex = 0, totalOneCourier = 0, totalGoBolt = 0;
            
            data.forEach(entry => {
                totalPurolator += entry.purolator || 0;
                totalFedex += entry.fedex || 0;
                totalOneCourier += entry.oneCourier || 0;
                totalGoBolt += entry.goBolt || 0;
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

        // Update RX Sales Statistics
        if (data.length > 0) {
            let totalNewRx = 0, totalRefills = 0, totalReAuth = 0, totalHold = 0;
            
            data.forEach(entry => {
                totalNewRx += entry.newRx || 0;
                totalRefills += entry.refill || 0;
                totalReAuth += entry.reAuth || 0;
                totalHold += entry.hold || 0;
            });

            const rxElements = {
                'totalNewRx': totalNewRx,
                'totalRefills': totalRefills,
                'totalReAuth': totalReAuth,
                'totalHold': totalHold,
                'reportTotalProcessed': totalNewRx + totalRefills + totalReAuth
            };

            Object.entries(rxElements).forEach(([id, value]) => {
                const element = document.getElementById(id);
                if (element) {
                    element.textContent = value;
                }
            });
        }

        // Update Professional Services Summary
        if (data.length > 0) {
            console.log('Processing Professional Services data...');
            
            // Group services by month
            const monthlyServices = new Map();
            let grandTotal = 0;

            data.forEach(entry => {
                if (entry.serviceType && entry.serviceCost) {
                    const date = new Date(entry.date);
                    const monthKey = `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}`;
                    
                    if (!monthlyServices.has(monthKey)) {
                        monthlyServices.set(monthKey, new Map());
                    }
                    
                    const monthData = monthlyServices.get(monthKey);
                    if (!monthData.has(entry.serviceType)) {
                        monthData.set(entry.serviceType, []);
                    }
                    
                    monthData.get(entry.serviceType).push(entry);
                    grandTotal += parseFloat(entry.serviceCost);
                }
            });

            // Update the service breakdown display
            const serviceBreakdown = document.getElementById('serviceBreakdown');
            if (serviceBreakdown) {
                let html = '';
                
                monthlyServices.forEach((services, monthKey) => {
                    const [year, month] = monthKey.split('-');
                    const monthName = new Date(year, month - 1).toLocaleString('default', { month: 'long' });
                    
                    html += `<div class="month-section">
                        <h5>${monthName} ${year}</h5>`;
                    
                    services.forEach((entries, serviceType) => {
                        const totalCost = entries.reduce((sum, entry) => 
                            sum + (parseFloat(entry.serviceCost) || 0), 0);
                        
                        html += `
                            <div class="service-type-row" data-service-type="${serviceType}">
                                <div class="service-summary">
                                    ${formatServiceType(serviceType)}: ${entries.length} - Total: $${totalCost.toFixed(2)}
                                </div>
                                <div class="service-details">
                                    ${entries.map(entry => `
                                        <div class="detail-item">
                                            <div>
                                                <span class="detail-label">Patient:</span>
                                                <span class="detail-value">${entry.patientName || 'N/A'}</span>
                                            </div>
                                            <div>
                                                <span class="detail-label">DOB:</span>
                                                <span class="detail-value">${formatDate(entry.patientDob)}</span>
                                            </div>
                                            <div>
                                                <span class="detail-label">Pharmacist:</span>
                                                <span class="detail-value">${entry.pharmacistName || 'N/A'}</span>
                                            </div>
                                            <div>
                                                <span class="detail-label">Date:</span>
                                                <span class="detail-value">${formatDate(entry.date)}</span>
                                            </div>
                                            <div>
                                                <span class="detail-label">Cost:</span>
                                                <span class="detail-value">$${parseFloat(entry.serviceCost).toFixed(2)}</span>
                                            </div>
                                        </div>
                                    `).join('')}
                                </div>
                            </div>`;
                    });
                    
                    html += '</div>';
                });
                
                serviceBreakdown.innerHTML = html;

                // Update total services cost
                const totalServicesElement = document.getElementById('totalServices');
                if (totalServicesElement) {
                    totalServicesElement.textContent = grandTotal.toFixed(2);
                }

                // Add click handlers
                document.querySelectorAll('.service-type-row').forEach(row => {
                    row.addEventListener('click', function(e) {
                        const details = this.querySelector('.service-details');
                        if (details && !details.contains(e.target)) {
                            details.classList.toggle('active');
                            
                            // Close other open details
                            document.querySelectorAll('.service-details.active').forEach(detail => {
                                if (detail !== details) {
                                    detail.classList.remove('active');
                                }
                            });
                        }
                    });
                });
            }
        }

        // Make report sections visible
        document.querySelectorAll('.report-section').forEach(section => {
            section.style.display = 'block';
        });

    } catch (error) {
        console.error('Error updating report display:', error);
        showErrorMessage('Error updating report display: ' + error.message);
    }
}

// Helper function to format month-year
function formatMonthYear(monthKey) {
    const [year, month] = monthKey.split('-');
    const date = new Date(year, parseInt(month) - 1);
    return date.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
}

// Helper function to format service type
function formatServiceType(type) {
    if (!type) return 'Unknown';
    return type.split('_').map(word => 
        word.charAt(0) + word.slice(1).toLowerCase()
    ).join(' ');
}

function setupDeliveryForm() {
    const deliveryForm = document.getElementById('deliveryForm');
    if (deliveryForm) {
        deliveryForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const submitButton = e.target.querySelector('button[type="submit"]');
            if (submitButton) {
                submitButton.disabled = true;
                submitButton.classList.add('loading');
            }

            try {
                console.log('Submitting delivery form...');

                const formData = {
                    date: document.getElementById('date').value,
                    // Delivery data
                    purolator: parseInt(document.getElementById('purolator').value) || 0,
                    fedex: parseInt(document.getElementById('fedex').value) || 0,
                    oneCourier: parseInt(document.getElementById('oneCourier').value) || 0,
                    goBolt: parseInt(document.getElementById('goBolt').value) || 0,
                    // Initialize other fields to 0 or null
                    newRx: 0,
                    refill: 0,
                    reAuth: 0,
                    hold: 0,
                    profilesEntered: 0,
                    whoFilledRx: 0,
                    activePercentage: 0,
                    serviceType: null,
                    serviceCost: 0
                };

                const response = await submitForm(formData);
                console.log('Delivery submission response:', response);
                showMessage('Successfully Submitted!');

                // Reset the form after successful submission
                deliveryForm.reset();

                if (submitButton) {
                    submitButton.disabled = false;
                    submitButton.classList.remove('loading');
                }
            } catch (error) {
                console.error('Error submitting delivery data:', error);
                showMessage('Failed to save delivery data: ' + error.message, 'error');
                
                if (submitButton) {
                    submitButton.disabled = false;
                    submitButton.classList.remove('loading');
                }
            }
        });
    } else {
        console.error('Delivery form not found in DOM');
    }
}

function setupRxSalesForm() {
    const rxSalesForm = document.getElementById('rxSalesForm');
    if (rxSalesForm) {
        // Remove any existing event listeners
        const clonedForm = rxSalesForm.cloneNode(true);
        rxSalesForm.parentNode.replaceChild(clonedForm, rxSalesForm);
        
        clonedForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            e.stopPropagation();
            
            const submitButton = clonedForm.querySelector('button[type="submit"]');
            if (submitButton) {
                submitButton.disabled = true;
            }

            console.log('Submitting RX Sales form...');

            const formData = {
                date: document.getElementById('date').value,
                // RX Sales data
                newRx: parseInt(document.getElementById('newRx').value) || 0,
                refill: parseInt(document.getElementById('refill').value) || 0,
                reAuth: parseInt(document.getElementById('reAuth').value) || 0,
                hold: parseInt(document.getElementById('hold').value) || 0,
                // Initialize other category fields to 0
                purolator: 0,
                fedex: 0,
                oneCourier: 0,
                goBolt: 0,
                profilesEntered: 0,
                whoFilledRx: 0,
                activePercentage: 0,
                serviceType: null,
                serviceCost: 0
            };

            console.log('RX Sales form data:', formData);

            try {
                const response = await submitForm(formData);
                console.log('RX Sales submission response:', response);
                showMessage('Successfully Submitted!');
                
                // Calculate and update totals
                const totalFilled = formData.newRx + formData.refill + formData.reAuth;
                const totalEntered = totalFilled + formData.hold;
                
                // Update the display totals
                if (document.getElementById('totalFilled')) {
                    document.getElementById('totalFilled').textContent = totalFilled;
                }
                if (document.getElementById('totalEntered')) {
                    document.getElementById('totalEntered').textContent = totalEntered;
                }
                if (document.getElementById('totalPerHour')) {
                    document.getElementById('totalPerHour').textContent = (totalEntered / 8).toFixed(2);
                }

                // Reset the form after successful submission
                clonedForm.reset();
                
                if (submitButton) {
                    submitButton.disabled = false;
                }

            } catch (error) {
                console.error('Error submitting RX Sales data:', error);
                showMessage('Failed to save RX Sales data: ' + error.message, 'error');
            }
        });
    } else {
        console.error('RX Sales form not found in DOM');
    }
}

function setupProfilesForm() {
    const profilesForm = document.getElementById('profilesForm');
    if (profilesForm) {
        // Remove any existing event listeners
        const clonedForm = profilesForm.cloneNode(true);
        profilesForm.parentNode.replaceChild(clonedForm, profilesForm);
        
        clonedForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            e.stopPropagation();
            
            const submitButton = clonedForm.querySelector('button[type="submit"]');
            if (submitButton) {
                submitButton.disabled = true;
            }

            console.log('Submitting Weekly Profiles form...');

            // Validate inputs
            const profilesEntered = parseInt(document.getElementById('profilesEntered').value) || 0;
            const whoFilledRx = parseInt(document.getElementById('whoFilledRx').value) || 0;
            let activePercentage = parseFloat(document.getElementById('activePercentage').value) || 0;

            // Calculate active percentage if not manually entered
            if (activePercentage === 0 && profilesEntered > 0) {
                activePercentage = (whoFilledRx / profilesEntered) * 100;
            }

            const formData = {
                date: document.getElementById('date').value,
                // Weekly Profiles data
                profilesEntered: profilesEntered,
                whoFilledRx: whoFilledRx,
                activePercentage: activePercentage,
                // Initialize other fields to 0
                purolator: 0,
                fedex: 0,
                oneCourier: 0,
                goBolt: 0,
                newRx: 0,
                refill: 0,
                reAuth: 0,
                hold: 0,
                serviceType: null,
                serviceCost: 0
            };

            console.log('Weekly Profiles form data:', formData);

            try {
                const response = await submitForm(formData);
                console.log('Weekly Profiles submission response:', response);
                showMessage('Successfully Submitted!');

                // Update weekly summary
                if (document.getElementById('weeklyTotalProfiles')) {
                    document.getElementById('weeklyTotalProfiles').textContent = profilesEntered;
                }
                if (document.getElementById('weeklyAverageActive')) {
                    document.getElementById('weeklyAverageActive').textContent = 
                        `${activePercentage.toFixed(2)}%`;
                }

                // Reset the form after successful submission
                clonedForm.reset();

                if (submitButton) {
                    submitButton.disabled = false;
                }

            } catch (error) {
                console.error('Error submitting Weekly Profiles data:', error);
                showMessage('Failed to save Weekly Profiles data: ' + error.message, 'error');
            }
        });
    } else {
        console.error('Profiles form not found in DOM');
    }
}

function showMessage(message, type = 'success') {
    const messageContainer = document.querySelector('.message-container');
    const messageElement = document.createElement('div');
    messageElement.className = `message-bubble ${type}-bubble`;
    messageElement.textContent = message;

    // Add animation class
    messageElement.classList.add('slide-in-fade');
    
    messageContainer.appendChild(messageElement);

    // Remove the message after animation completes
    setTimeout(() => {
        messageElement.classList.add('fade-out');
        setTimeout(() => {
            messageContainer.removeChild(messageElement);
        }, 300); // Match the fade-out animation duration
    }, 3000);
}

function updateServiceBreakdown(data) {
    const serviceBreakdown = document.getElementById('serviceBreakdown');
    if (!serviceBreakdown) return;

    // Group services by type
    const servicesByType = new Map();
    data.forEach(entry => {
        if (!servicesByType.has(entry.serviceType)) {
            servicesByType.set(entry.serviceType, []);
        }
        servicesByType.get(entry.serviceType).push(entry);
    });

    // Generate HTML for each service type
    servicesByType.forEach((services, type) => {
        const row = document.createElement('div');
        row.className = 'service-type-row';
        row.innerHTML = `
            ${type}: ${services.length}
            <div class="service-details">
                ${services.map(service => `
                    <div class="detail-item">
                        <div class="detail-label">Patient:</div>
                        <div class="detail-value">${service.patientName || 'N/A'}</div>
                        <div class="detail-label">Date:</div>
                        <div class="detail-value">${formatDate(service.date)}</div>
                        <div class="detail-label">Cost:</div>
                        <div class="detail-value">$${service.serviceCost.toFixed(2)}</div>
                    </div>
                `).join('')}
            </div>
        `;

        // Add click handler
        row.addEventListener('click', function(e) {
            const details = this.querySelector('.service-details');
            if (details) {
                // Toggle only if click is on the row itself, not the details
                if (!details.contains(e.target)) {
                    details.classList.toggle('active');
                    
                    // Close other open details
                    document.querySelectorAll('.service-details.active').forEach(detail => {
                        if (detail !== details) {
                            detail.classList.remove('active');
                        }
                    });
                }
            }
        });

        serviceBreakdown.appendChild(row);
    });
}

// Helper function to format dates
function formatDate(dateString) {
    if (!dateString) return 'N/A';
    try {
        // Add one day to compensate for timezone display issue
        const date = new Date(dateString);
        date.setDate(date.getDate() + 1);
        return date.toLocaleDateString();
    } catch (error) {
        console.error('Error formatting date:', error);
        return 'Invalid Date';
    }
}

// Helper function to format service type display
function formatServiceType(type) {
    if (!type) return 'Unknown';
    return type.split('_').map(word => 
        word.charAt(0) + word.slice(1).toLowerCase()
    ).join(' ');
}

// Update the service breakdown rendering
function renderServiceBreakdown(data) {
    console.log('=== Render Service Breakdown ===');
    console.log('Data received for rendering:', data);

    // Filter out entries that have serviceType (Professional Services only)
    const serviceEntries = data.filter(entry => entry.serviceType !== null);
    console.log('Filtered service entries:', serviceEntries);

    // Group by month
    const monthlyServices = new Map();
    
    serviceEntries.forEach(entry => {
        const date = new Date(entry.date);
        const monthKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
        
        if (!monthlyServices.has(monthKey)) {
            monthlyServices.set(monthKey, new Map());
        }
        
        const monthData = monthlyServices.get(monthKey);
        if (!monthData.has(entry.serviceType)) {
            monthData.set(entry.serviceType, []);
        }
        
        monthData.get(entry.serviceType).push(entry);
    });

    // Update the display
    const serviceBreakdown = document.getElementById('serviceBreakdown');
    if (serviceBreakdown) {
        let html = '';
        let grandTotal = 0;
        
        monthlyServices.forEach((services, monthKey) => {
            const [year, month] = monthKey.split('-');
            const monthName = new Date(year, month - 1).toLocaleString('default', { month: 'long' });
            
            html += `<div class="month-section">
                <h5>${monthName} ${year}</h5>`;
            
            services.forEach((entries, serviceType) => {
                const totalCost = entries.reduce((sum, entry) => 
                    sum + (parseFloat(entry.serviceCost) || 0), 0);
                grandTotal += totalCost;
                
                html += `
                    <div class="service-type-row" data-service-type="${serviceType}">
                        <div class="service-summary">
                            ${formatServiceType(serviceType)}: ${entries.length} - Total: $${totalCost.toFixed(2)}
                        </div>
                        <div class="service-details">
                            ${entries.map(entry => `
                                <div class="detail-item">
                                    <div>
                                        <span class="detail-label">Patient:</span>
                                        <span class="detail-value">${entry.patientName || 'N/A'}</span>
                                    </div>
                                    <div>
                                        <span class="detail-label">DOB:</span>
                                        <span class="detail-value">${formatDate(entry.patientDob)}</span>
                                    </div>
                                    <div>
                                        <span class="detail-label">Pharmacist:</span>
                                        <span class="detail-value">${entry.pharmacistName || 'N/A'}</span>
                                    </div>
                                    <div>
                                        <span class="detail-label">Date:</span>
                                        <span class="detail-value">${formatDate(entry.date)}</span>
                                    </div>
                                    <div>
                                        <span class="detail-label">Cost:</span>
                                        <span class="detail-value">$${parseFloat(entry.serviceCost).toFixed(2)}</span>
                                    </div>
                                </div>
                            `).join('')}
                        </div>
                    </div>`;
            });
            
            html += '</div>';
        });
        
        serviceBreakdown.innerHTML = html;

        // Update total services cost
        const totalServicesElement = document.getElementById('totalServices');
        if (totalServicesElement) {
            totalServicesElement.textContent = grandTotal.toFixed(2);
        }

        // Add click handlers for expanding/collapsing details
        document.querySelectorAll('.service-type-row').forEach(row => {
            row.addEventListener('click', function(e) {
                const details = this.querySelector('.service-details');
                if (details && !details.contains(e.target)) {
                    details.classList.toggle('active');
                    
                    // Close other open details
                    document.querySelectorAll('.service-details.active').forEach(detail => {
                        if (detail !== details) {
                            detail.classList.remove('active');
                        }
                    });
                }
            });
        });
    }
}
