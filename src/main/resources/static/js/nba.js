/**
 * @fileoverview NBA Search Interface Controller
 * Manages the search section visibility and persistence
 * 
 * @dependencies
 * - DOM Elements: #searchToggle, #searchSection
 * - Browser: localStorage API
 * 
 * @version 1.0
 */

/* ========== Event Listeners ========== */

/**
 * Initializes search interface functionality on DOM load
 * @listens DOMContentLoaded
 */
document.addEventListener('DOMContentLoaded', function() {
    /* ---- DOM Element References ---- */
    const searchToggle = document.getElementById('searchToggle');
    const searchSection = document.getElementById('searchSection');

    /**
     * Toggles search section visibility and persists state
     * @listens click
     * @note Maintains UI state across page refreshes
     */
    searchToggle.addEventListener('click', function() {
        const isVisible = searchSection.style.display === 'flex';
        searchSection.style.display = isVisible ? 'none' : 'flex';
        searchToggle.classList.toggle('active');
        
        // Persist visibility state
        localStorage.setItem('searchSectionVisible', !isVisible);
    });

    /* ---- State Restoration ---- */
    /**
     * Restores previous search section visibility state
     * @note Defaults to hidden if no stored state exists
     */
    const wasVisible = localStorage.getItem('searchSectionVisible') === 'true';
    if (wasVisible) {
        searchSection.style.display = 'flex';
        searchToggle.classList.add('active');
    }
});

/**
 * @todo Consider adding:
 * - Error handling for localStorage failures
 * - Transition animations for smoother toggling
 * - Keyboard shortcuts for accessibility
 */