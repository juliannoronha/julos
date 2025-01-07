/* =============================================================================
 * Audit Page JavaScript Module
 * Purpose: Handles audit page interactions and navigation
 * ============================================================================= */

/**
 * Handles page navigation with fade effect
 * @param {string} url - Target URL
 * @param {Event} event - Click event
 */
function fadeOutAndNavigate(url, event) {
    event.preventDefault();
    document.querySelector('.container').style.opacity = '0';
    setTimeout(() => window.location.href = url, 500);
}

/**
 * Shows confirmation dialog for clearing logs
 * @security Requires admin confirmation
 */
function confirmClearLogs() {
    const dialog = document.createElement('div');
    dialog.className = 'confirmation-dialog';
    dialog.innerHTML = `
        <h3>Clear Audit Logs</h3>
        <p>Are you sure you want to clear all audit logs? This action cannot be undone.</p>
        <div class="dialog-buttons">
            <button onclick="cancelClearLogs()" class="back-button">
                <i class="fas fa-times"></i> Cancel
            </button>
            <button onclick="clearLogs()" class="clear-logs-button">
                <i class="fas fa-trash-alt"></i> Clear All
            </button>
        </div>
    `;

    const overlay = document.createElement('div');
    overlay.className = 'overlay';

    document.body.appendChild(overlay);
    document.body.appendChild(dialog);

    overlay.style.display = 'block';
    dialog.style.display = 'block';
}

/**
 * Closes the confirmation dialog
 */
function cancelClearLogs() {
    const dialog = document.querySelector('.confirmation-dialog');
    const overlay = document.querySelector('.overlay');
    
    if (dialog && overlay) {
        dialog.remove();
        overlay.remove();
    }
}

/**
 * Handles the actual log clearing
 * @security Requires admin role
 */
function clearLogs() {
    // Get CSRF tokens
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    // Prepare headers
    const headers = {
        'Content-Type': 'application/json'
    };
    
    // Add CSRF header if available
    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    fetch('/api/audit/clear', {
        method: 'POST',
        headers: headers
    })
    .then(response => {
        if (response.ok) {
            window.location.reload();
        } else {
            throw new Error('Failed to clear logs');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Failed to clear logs. Please try again.');
    })
    .finally(() => {
        cancelClearLogs();
    });
}

/* -----------------------------------------------------------------------------
 * Page Load Handlers
 * -------------------------------------------------------------------------- */

document.addEventListener('DOMContentLoaded', function() {
    // Future enhancement: Add filters and search functionality
});
