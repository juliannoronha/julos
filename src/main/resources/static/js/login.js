/* =============================================================================
 * Login Page JavaScript Module
 * 
 * PURPOSE: Manages login form animations and message handling
 * DEPENDENCIES: None (vanilla JavaScript)
 * SCOPE: Login page functionality
 * 
 * FEATURES:
 * - Automatic logout message fadeout
 * - Animated form submission
 * - Graceful transitions
 * 
 * @note Relies on CSS classes for animations
 * @note Assumes specific DOM structure from login.html
 * ============================================================================= */

/* -----------------------------------------------------------------------------
 * Logout Message Handler
 * 
 * @purpose Manages the automatic fadeout of logout messages
 * @note Uses setTimeout for delayed animations
 * -------------------------------------------------------------------------- */
var logoutMessage = document.getElementById('logoutMessage');
if (logoutMessage) {
    setTimeout(function() {
        logoutMessage.classList.add('fade-out');
        setTimeout(function() {
            logoutMessage.style.display = 'none';
        }, 600);  // Matches CSS animation duration
    }, 3000);    // Message display duration
}

/* -----------------------------------------------------------------------------
 * Form Submission Handler
 * 
 * @purpose Provides smooth transition animation during form submission
 * @note Prevents default form action until animation completes
 * @dependency Requires 'loginContainer' element and fade-out CSS class
 * -------------------------------------------------------------------------- */
document.querySelector('form').addEventListener('submit', function(e) {
    e.preventDefault();  // Pause submission for animation
    document.getElementById('loginContainer').classList.add('fade-out');
    
    // Submit form after animation
    setTimeout(() => {
        this.submit();
    }, 500);  // Matches CSS animation duration
});

/* @todo [ENHANCEMENT] Add form validation
 * @todo [SECURITY] Add rate limiting for submission attempts
 * @todo [UX] Add loading indicator during submission
 */