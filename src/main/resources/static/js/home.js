/* =============================================================================
 * Home Page JavaScript Module
 * 
 * PURPOSE: Manages dashboard functionality, animations, and real-time updates
 * DEPENDENCIES: 
 * - Chart.js for data visualization
 * - Server-Sent Events (SSE) API for real-time updates
 * 
 * ARCHITECTURE:
 * - Event-driven initialization
 * - Role-based access control
 * - Real-time data streaming
 * - Memory-managed chart rendering
 * ============================================================================= */

/* -----------------------------------------------------------------------------
 * Core Initialization
 * -------------------------------------------------------------------------- */

/**
 * Main initialization handler
 * @note Establishes core functionality on page load
 * @dependencies DOM content, user role data
 */
document.addEventListener('DOMContentLoaded', function() {
    initWelcomeMessage();
    
    // Temporarily disabled PAC functionality
    // if (hasRequiredRole()) {
    //     fetchOverallProductivity();
    //     setupSSEConnection(); 
    // }
    
    // // Periodic cleanup to prevent memory leaks
    // chartCleanupInterval = setInterval(cleanupChartResources, 5 * 60 * 1000);
});

/* -----------------------------------------------------------------------------
 * UI Animation System
 * -------------------------------------------------------------------------- */

/**
 * Manages welcome message animation and cleanup
 * @note Removes element from DOM after animation for memory efficiency
 */
function initWelcomeMessage() {
    const welcomeMessage = document.getElementById('welcomeMessage');
    setTimeout(() => {
        welcomeMessage.style.opacity = '0';
        setTimeout(() => welcomeMessage.style.display = 'none', 1000);
    }, 6000);
}

/**
 * Handles page transitions with fade effect
 * @param {string} url - Target navigation URL
 * @param {Event} event - Click event object
 * @note Prevents default navigation for smooth animation
 */
function fadeOutAndNavigate(url, event) {
    event.preventDefault();
    fadeOutElements();
    setTimeout(() => window.location.href = url, 500);
}

/**
 * Handles form submissions with fade animation
 * @param {HTMLFormElement} form - Form to submit
 * @param {Event} event - Submit event
 */
function fadeOutAndSubmit(form, event) {
    event.preventDefault();
    fadeOutElements();
    setTimeout(() => form.submit(), 500);
}

/**
 * Fades out main UI elements
 */
function fadeOutElements() {
    document.getElementById('mainNav').classList.add('fade-out');
    document.getElementById('mainContent').classList.add('fade-out');
}

/* -----------------------------------------------------------------------------
 * Access Control & Authorization
 * @security Critical section - handles user permissions
 * -------------------------------------------------------------------------- */

/**
 * Displays temporary access denied message
 * @note Message auto-hides after 3 seconds
 */
function showAccessDeniedMessage() {
    const message = document.getElementById('accessDeniedMessage');
    message.classList.remove('hide');
    message.classList.add('show');
    message.style.display = 'block';
    
    setTimeout(() => {
        message.classList.remove('show');
        message.classList.add('hide');
        setTimeout(() => {
            message.style.display = 'none';
            message.classList.remove('hide');
        }, 500);
    }, 3000);
}

/**
 * Checks user authorization before navigation
 * @param {string} url - Target URL to check access for
 */
function handleUnauthorizedAccess(url) {
    fetch(url, {
        method: 'GET',
        credentials: 'same-origin',
        headers: {
            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
        }
    }).then(response => {
        response.status === 403 ? showAccessDeniedMessage() : window.location.href = url;
    }).catch(error => console.error('Error:', error));
}

/* -----------------------------------------------------------------------------
 * Navigation Handlers (TEMP DISABLED - PAC)
 * -------------------------------------------------------------------------- */

// /**
//  * Handles PacMed section access
//  * @param {Event} event - Click event
//  */
// function handlePacMedClick(event) {
//     event.preventDefault();
//     const pacmedButton = document.getElementById('pacmedButton');
    
//     if (hasRequiredRole()) {
//         window.location.href = '/packmed';
//     } else {
//         showAccessDeniedMessage();
//         flashButton(pacmedButton);
//     }
// }

/**
 * Handles Wellca section access
 * @param {Event} event - Click event
 */
function handleWellcaClick(event) {
    event.preventDefault();
    const wellcaButton = document.getElementById('wellcaButton');
    
    if (hasRequiredRole()) {
        window.location.href = '/wellca';
    } else {
        showAccessDeniedMessage();
        flashButton(wellcaButton);
    }
}

/**
 * Handles NBA section access
 * @param {Event} event - Click event
 */
function handleNBAClick(event) {
    event.preventDefault();
    const nbaButton = document.getElementById('nbaButton');
    
    if (hasNBAAccess()) {
        window.location.href = '/nbaplayers';
    } else {
        showAccessDeniedMessage();
        flashButton(nbaButton);
    }
}

/**
 * Handles Audit Log access
 * @param {Event} event - Click event
 * @security Admin-only access enforced
 */
function handleAuditLogClick(event) {
    event.preventDefault();
    const auditButton = document.getElementById('auditButton');
    
    if (hasAdminRole()) {
        window.location.href = '/audit';
    } else {
        showAccessDeniedMessage();
        flashButton(auditButton);
    }
}

/* -----------------------------------------------------------------------------
 * Role & Access Validation
 * -------------------------------------------------------------------------- */

/**
 * Role validation for admin features
 * @returns {boolean} True if user has sufficient privileges
 * @security Enforces role-based access control
 */
function hasRequiredRole() {
    const userRole = document.body.dataset.userRole;
    return ['ROLE_MODERATOR', 'ROLE_ADMIN', 'ROLE_CHECKER', 
            'ROLE_SHIPPING', 'ROLE_INVENTORY'].includes(userRole);
}

/**
 * NBA section access validation
 * @returns {boolean} True if user has NBA access
 * @note Separate permission set from admin features
 */
function hasNBAAccess() {
    const userRole = document.body.dataset.userRole;
    return ['ROLE_USER', 'ROLE_ADMIN', 'ROLE_MODERATOR'].includes(userRole);
}

/**
 * Admin role validation
 * @returns {boolean} True if user has admin privileges
 * @security Enforces admin-only access
 */
function hasAdminRole() {
    const userRole = document.body.dataset.userRole;
    return ['ROLE_ADMIN'].includes(userRole);
}

/* -----------------------------------------------------------------------------
 * UI Feedback & Animations
 * -------------------------------------------------------------------------- */

/**
 * Flashes button red to indicate access denied
 * @param {HTMLElement} button - Button to animate
 */
function flashButton(button) {
    button.classList.add('flash-red');
    setTimeout(() => button.classList.remove('flash-red'), 1500);
}

/* -----------------------------------------------------------------------------
 * Dashboard Data Management (TEMP DISABLED - PAC)
 * -------------------------------------------------------------------------- */

// /**
//  * Fetches and processes productivity metrics
//  * @throws {Error} If API response is invalid
//  * @note Implements error handling and data validation
//  */
// function fetchOverallProductivity() {
//     fetch('/api/overall-productivity')
//         .then(response => {
//             if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
//             return response.json();
//         })
//         .then(data => {
//             if (!data || typeof data !== 'object') {
//                 throw new Error('Invalid data format received');
//             }
//             updateDashboard(data);
//             if (data.chartData && Object.keys(data.chartData).length > 0) {
//                 createPacMedChart(data.chartData);
//             }
//         })
//         .catch(error => {
//             console.error('Error:', error);
//             updateDashboardError(error.message);
//         });
// }

// /**
//  * Updates dashboard with productivity metrics
//  * @param {Object} data - Dashboard data object
//  */
// function updateDashboard(data) {
//     document.getElementById('totalSubmissions').textContent = data.totalSubmissions ?? 'N/A';
//     document.getElementById('totalPouchesChecked').textContent = data.totalPouchesChecked ?? 'N/A';
//     document.getElementById('avgTimePerPouch').textContent = 
//         data.avgTimePerPouch != null ? formatDuration(data.avgTimePerPouch) : 'N/A';
//     document.getElementById('avgPouchesPerHour').textContent = 
//         data.avgPouchesPerHour != null ? data.avgPouchesPerHour.toFixed(2) : 'N/A';
// }

// /**
//  * Displays error message in dashboard
//  * @param {string} errorMessage - Error message to display
//  */
// function updateDashboardError(errorMessage) {
//     const errorText = 'Error: ' + errorMessage;
//     ['totalSubmissions', 'totalPouchesChecked', 
//      'avgTimePerPouch', 'avgPouchesPerHour'].forEach(id => {
//         document.getElementById(id).textContent = errorText;
//     });
// }

// /**
//  * Formats duration in minutes and seconds
//  * @param {number} seconds - Duration in seconds
//  * @returns {string} Formatted duration string
//  */
// function formatDuration(seconds) {
//     const minutes = Math.floor(seconds / 60);
//     const remainingSeconds = Math.round(seconds % 60);
//     return `${minutes}m ${remainingSeconds.toString().padStart(2, '0')}s`;
// }

/* -----------------------------------------------------------------------------
 * Server-Sent Events (SSE) Management (TEMP DISABLED - PAC)
 * -------------------------------------------------------------------------- */

// let eventSource;

/**
 * Sets up SSE connection for real-time updates
 * @note Includes auto-reconnect on error
 */
// function setupSSEConnection() {
//     if (eventSource) eventSource.close();
    
//     eventSource = new EventSource('/api/overall-productivity-stream');
    
//     window.addEventListener('beforeunload', () => {
//         if (eventSource) eventSource.close();
//     });
    
//     eventSource.onmessage = function(event) {
//         try {
//             const data = JSON.parse(event.data);
//             if (!Array.isArray(data)) updateDashboard(data);
//         } catch (error) {
//             console.error('Error parsing SSE data:', error);
//         }
//     };
    
//     eventSource.onerror = function(error) {
//         console.error('Error in SSE connection:', error);
//         eventSource.close();
//         setTimeout(setupSSEConnection, 5000);
//     };
// }

/* -----------------------------------------------------------------------------
 * Chart Management (TEMP DISABLED - PAC)
 * -------------------------------------------------------------------------- */

// let chartDataCache = new Map();
// let pacMedChart = null;
// let chartCleanupInterval;

// /**
//  * Creates/updates productivity chart
//  * @param {Object} data - Chart data object
//  */
// function createPacMedChart(data) {
//     if (!data?.labels || !data?.pouchesChecked) {
//         console.error('Invalid chart data');
//         return;
//     }

//     const ctx = document.getElementById('pacMedChart');
//     if (!ctx) {
//         console.error('Canvas element not found');
//         return;
//     }

//     if (pacMedChart) {
//         pacMedChart.destroy();
//         pacMedChart = null;
//     }

//     chartDataCache.set('latest', {
//         data: data,
//         timestamp: Date.now()
//     });

//     pacMedChart = new Chart(ctx, {
//         type: 'line',
//         data: {
//             labels: data.labels,
//             datasets: [{
//                 label: 'Pouches Checked',
//                 data: data.pouchesChecked,
//                 borderColor: 'rgb(75, 192, 192)',
//                 tension: 0.1
//             }]
//         },
//         options: {
//             responsive: true,
//             maintainAspectRatio: false,
//             scales: { y: { beginAtZero: true } },
//             plugins: {
//                 title: {
//                     display: true,
//                     text: 'Pouches Checked (Last 7 Days)',
//                     font: { size: 16 }
//                 },
//                 subtitle: {
//                     display: true,
//                     text: data.pouchesChecked.every(val => val === 0) ? 
//                           'Sample data shown (no actual data available)' : '',
//                     color: 'red',
//                     font: { size: 14, style: 'italic' }
//                 }
//             }
//         }
//     });
// }

// /**
//  * Cleans up chart resources to prevent memory leaks
//  * @note Runs every 5 minutes
//  */
// function cleanupChartResources() {
//     const CACHE_TIMEOUT = 30 * 60 * 1000; // 30 minutes
//     const now = Date.now();

//     chartDataCache.forEach((value, key) => {
//         if (now - value.timestamp > CACHE_TIMEOUT) {
//             chartDataCache.delete(key);
//         }
//     });

//     if (pacMedChart && !document.getElementById('pacMedChart')) {
//         pacMedChart.destroy();
//         pacMedChart = null;
//     }
// }

// // Cleanup on page unload
// window.addEventListener('beforeunload', function() {
//     if (chartCleanupInterval) clearInterval(chartCleanupInterval);
//     if (pacMedChart) {
//         pacMedChart.destroy();
//         pacMedChart = null;
//     }
//     chartDataCache.clear();
// });

// Initial data fetch
// document.addEventListener('DOMContentLoaded', fetchOverallProductivity);

function handleNavClick(panel) {
    const panels = ['chat', 'notification', 'userProfile'];
    panels.forEach(p => {
        const element = document.getElementById(`${p}Panel`);
        if (element) {
            if (p === panel) {
                const isOpening = element.style.display === 'none';
                if (isOpening) {
                    element.classList.remove('closing');
                    element.style.display = 'block';
                } else {
                    element.classList.add('closing');
                    // Wait for animation to complete before hiding
                    setTimeout(() => {
                        element.style.display = 'none';
                        element.classList.remove('closing');
                    }, 300); // Match animation duration
                }
            } else {
                element.style.display = 'none';
            }
        }
    });
}

function handleActiveMenu() {
    const nav = document.getElementById('mainNav');
    nav.classList.toggle('collapsed');
}

// Close panels when clicking outside
document.addEventListener('click', (event) => {
    if (!event.target.closest('.nav-button') && 
        !event.target.closest('.nav-panel') && 
        !event.target.closest('.profile-button')) {
        document.querySelectorAll('.nav-panel').forEach(panel => {
            panel.style.display = 'none';
        });
    }
});

// Notification System
let notificationSocket;
let unreadNotifications = 0;

/**
 * Initialize the notification system
 * This will be called when the notification panel is opened
 */
function initializeNotifications() {
    const notificationList = document.querySelector('.notification-list');
    if (!notificationList) return;

    // Initially show a placeholder or loading state
    notificationList.innerHTML = `
        <div class="notification-item">
            <div class="notification-item-content">
                <p class="notification-message">No new notifications</p>
            </div>
        </div>
    `;

    // Setup WebSocket when you're ready to implement real-time notifications
    // setupNotificationSocket();
}

/**
 * Add a new notification to the list
 * @param {Object} notification - { type, message, desc, image }
 */
function addNotification(notification) {
    const notificationList = document.querySelector('.notification-list');
    if (!notificationList) return;

    // Remove the "No new notifications" placeholder if it exists
    if (notificationList.querySelector('.notification-message')?.textContent === 'No new notifications') {
        notificationList.innerHTML = '';
    }

    const notificationElement = document.createElement('div');
    notificationElement.className = 'notification-item';
    notificationElement.innerHTML = `
        <img src="${notification.image || '/images/default-notification.png'}" alt="notification" />
        <div class="notification-item-content">
            <p class="notification-message">${notification.message}</p>
            ${notification.desc ? `<p class="notification-desc">${notification.desc}</p>` : ''}
        </div>
    `;

    // Add new notification at the top of the list
    notificationList.insertBefore(notificationElement, notificationList.firstChild);
    updateNotificationBadge(1);
}

/**
 * Update the notification badge count
 * @param {number} increment - Number to increment by (or exact number if setExact is true)
 * @param {boolean} setExact - If true, set the count to increment value instead of incrementing
 */
function updateNotificationBadge(increment = 0, setExact = false) {
    const badge = document.querySelector('.new-notifications-badge');
    if (!badge) return;

    if (setExact) {
        unreadNotifications = increment;
    } else {
        unreadNotifications += increment;
    }

    badge.textContent = `${unreadNotifications} New`;
    badge.style.display = unreadNotifications > 0 ? 'block' : 'none';
}

/**
 * Clear all notifications
 */
function clearNotifications() {
    const notificationList = document.querySelector('.notification-list');
    if (!notificationList) return;

    notificationList.innerHTML = `
        <div class="notification-item">
            <div class="notification-item-content">
                <p class="notification-message">No new notifications</p>
            </div>
        </div>
    `;
    updateNotificationBadge(0, true);
}

/**
 * Setup WebSocket connection for real-time notifications
 * To be implemented when ready for real-time features
 */
function setupNotificationSocket() {
    // Commented out until ready to implement WebSocket
    /*
    notificationSocket = new WebSocket(`ws://${window.location.host}/ws/notifications`);
    
    notificationSocket.onmessage = function(event) {
        const notification = JSON.parse(event.data);
        addNotification(notification);
    };

    notificationSocket.onclose = function() {
        setTimeout(setupNotificationSocket, 5000);
    };
    */
}

// Initialize notifications when the page loads
document.addEventListener('DOMContentLoaded', function() {
    initializeNotifications();
});

/**
 * Handle profile action clicks
 * @param {string} action - The action type ('settings')
 */
function handleProfileAction(action) {
    if (action === 'settings') {
        window.location.href = '/settings';
    }
}

