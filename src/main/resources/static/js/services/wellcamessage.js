/* =============================================================================
 * Wellca Message Service Module
 * 
 * PURPOSE: Handles message display and animations for the Wellca management system
 * ============================================================================= */

import { 
    CSS_CLASSES, 
    TIME_CONSTANTS,
    MESSAGE_TYPES
} from '../config/wellcaconstants.js';

let messageContainer = null;

/* ------------------------------------------------------------------------- 
 * Message Container Setup
 * --------------------------------------------------------------------- */
function initializeMessageContainer() {
    if (!messageContainer) {
        messageContainer = document.createElement('div');
        messageContainer.className = CSS_CLASSES.MESSAGE_CONTAINER;
        document.body.appendChild(messageContainer);
    }
    return messageContainer;
}

/* ------------------------------------------------------------------------- 
 * Message Display Functions
 * --------------------------------------------------------------------- */
function showMessage(message, type = MESSAGE_TYPES.SUCCESS) {
    const container = initializeMessageContainer();
    const messageElement = createMessageElement(message, type);
    
    container.appendChild(messageElement);
    animateMessage(messageElement);
}

function createMessageElement(message, type) {
    const messageElement = document.createElement('div');
    messageElement.className = `${CSS_CLASSES.MESSAGE_BUBBLE} ${type}-bubble ${CSS_CLASSES.SLIDE_IN_FADE}`;
    messageElement.textContent = message;
    return messageElement;
}

function animateMessage(messageElement) {
    // Start fade out after display duration
    setTimeout(() => {
        messageElement.classList.add(CSS_CLASSES.FADE_OUT);
        
        // Remove element after animation completes
        setTimeout(() => {
            if (messageElement.parentNode === messageContainer) {
                messageContainer.removeChild(messageElement);
            }
        }, TIME_CONSTANTS.ANIMATION_DURATION);
    }, TIME_CONSTANTS.MESSAGE_DISPLAY_DURATION);
}

/* ------------------------------------------------------------------------- 
 * Message Queue Management
 * --------------------------------------------------------------------- */
const messageQueue = [];
let isProcessingQueue = false;

function queueMessage(message, type = MESSAGE_TYPES.SUCCESS) {
    messageQueue.push({ message, type });
    if (!isProcessingQueue) {
        processMessageQueue();
    }
}

async function processMessageQueue() {
    if (messageQueue.length === 0) {
        isProcessingQueue = false;
        return;
    }

    isProcessingQueue = true;
    const { message, type } = messageQueue.shift();
    
    showMessage(message, type);

    // Wait for message to complete before showing next
    await new Promise(resolve => 
        setTimeout(resolve, TIME_CONSTANTS.MESSAGE_DISPLAY_DURATION + TIME_CONSTANTS.ANIMATION_DURATION)
    );

    processMessageQueue();
}

/* ------------------------------------------------------------------------- 
 * Error Message Handling
 * --------------------------------------------------------------------- */
function showErrorMessage(error) {
    const errorMessage = error instanceof Error ? error.message : error;
    showMessage(errorMessage, MESSAGE_TYPES.ERROR);
}

function showWarningMessage(message) {
    showMessage(message, MESSAGE_TYPES.WARNING);
}

function showSuccessMessage(message) {
    showMessage(message, MESSAGE_TYPES.SUCCESS);
}

/* ------------------------------------------------------------------------- 
 * Exports
 * --------------------------------------------------------------------- */
export {
    showMessage,
    queueMessage,
    showErrorMessage,
    showWarningMessage,
    showSuccessMessage,
    initializeMessageContainer
};
