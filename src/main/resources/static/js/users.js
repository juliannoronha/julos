/* =============================================================================
 * User Management Module
 * 
 * PURPOSE: Handles user deletion and search functionality with modal confirmation
 * 
 * DEPENDENCIES:
 * - CSRF tokens from meta tags
 * - DOM elements: deleteModal, searchInput, usersTable
 * - Fetch API for AJAX requests
 * 
 * SECURITY:
 * - CSRF protection
 * - Input sanitization via encodeURIComponent
 * - Modal confirmation for destructive actions
 * ============================================================================= */

/* ------------------------------------------------------------------------- 
 * Core Variables & Security
 * --------------------------------------------------------------------- */
let currentUsername = '';
const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

/* ------------------------------------------------------------------------- 
 * Modal Management Functions
 * @note Handles user deletion confirmation flow
 * --------------------------------------------------------------------- */
function showDeleteConfirmation(username) {
    currentUsername = username;
    document.getElementById('deleteModal').style.display = 'block';
}

/* ------------------------------------------------------------------------- 
 * Event Listeners for Modal Actions
 * --------------------------------------------------------------------- */
document.getElementById('confirmDelete').addEventListener('click', async function() {
    try {
        const response = await fetch('/users/delete?username=' + encodeURIComponent(currentUsername), {
            method: 'POST',
            headers: {
                [csrfHeader]: csrfToken
            }
        });
        
        if (!response.ok) throw new Error('Delete operation failed');
        
        location.reload();
    } catch (error) {
        console.error('Error:', error);
        alert('Error deleting user: ' + error.message);
    } finally {
        document.getElementById('deleteModal').style.display = 'none';
    }
});

document.getElementById('cancelDelete').addEventListener('click', function() {
    document.getElementById('deleteModal').style.display = 'none';
});

window.onclick = function(event) {
    const modal = document.getElementById('deleteModal');
    if (event.target == modal) {
        modal.style.display = 'none';
    }
}

/* ------------------------------------------------------------------------- 
 * Live Search Implementation
 * @note Uses real-time filtering without server requests
 * --------------------------------------------------------------------- */
document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('searchInput');
    const usersTable = document.getElementById('usersTable');
    const rows = usersTable.getElementsByTagName('tr');

    const searchHandler = function() {
        const searchTerm = searchInput.value.toLowerCase();

        // Skip header row (i=1)
        for (let i = 1; i < rows.length; i++) {
            const username = rows[i].getElementsByTagName('td')[0].textContent.toLowerCase();
            rows[i].style.display = username.includes(searchTerm) ? '' : 'none';
        }
    };
    
    searchInput.addEventListener('keyup', searchHandler);
    
    // Cleanup event listeners on page unload
    window.addEventListener('unload', function() {
        searchInput.removeEventListener('keyup', searchHandler);
    });
});

/* ------------------------------------------------------------------------- 
 * User Deletion API
 * @param {string} username - Username of account to delete
 * @throws {Error} When network request fails
 * --------------------------------------------------------------------- */
function deleteUser(username) {
    const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    fetch('/users/delete?username=' + encodeURIComponent(username), {
        method: 'POST',
        headers: {
            [header]: token
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.text();
    })
    .then(data => {
        alert(data);
        location.reload();
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error deleting user: ' + error.message);
    });
}