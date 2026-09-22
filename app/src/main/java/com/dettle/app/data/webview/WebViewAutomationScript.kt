package com.dettle.app.data.webview

/**
 * The JavaScript automation script injected into every provider WebView.
 *
 * This script:
 * 1. Waits for the page to be ready
 * 2. Checks if the user is logged in
 * 3. Provides sendPrompt(text, selectors) that:
 *    - Finds the input field, sets text via React-compatible synthetic events
 *    - Clicks the send button
 *    - MutationObserver watches for streaming text chunks
 *    - Each chunk fires AndroidBridge.onChunkReceived()
 *    - When streaming indicator disappears: fires AndroidBridge.onComplete()
 *    - Scans response for <tool_call> XML tags and fires AndroidBridge.onToolCallDetected()
 */
object WebViewAutomationScript {

    /**
     * The main JS injected once per WebView session (on page load).
     * Uses selectors from the ProviderSelectors config.
     */
    fun buildInitScript(selectors: ProviderSelectors): String = """
        (function() {
            'use strict';

            window._dettleSelectors = {
                input: '${selectors.input}',
                send: '${selectors.send}',
                response: '${selectors.response}',
                streamingIndicator: '${selectors.streamingIndicator}',
                loginCheck: '${selectors.loginCheck}'
            };

            window._dettleObserver = null;
            window._dettleLastText = '';
            window._dettleStreaming = false;

            // ── Check login state ────────────────────────────────────────
            function checkLogin() {
                const loginEl = document.querySelector(window._dettleSelectors.loginCheck);
                return loginEl !== null;
            }

            // ── Set value on React-controlled inputs ─────────────────────
            // React intercepts DOM events; we must dispatch synthetic events
            // so React's internal state updates and sees our text
            function setReactValue(element, value) {
                const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
                    window.HTMLTextAreaElement.prototype, 'value'
                ) || Object.getOwnPropertyDescriptor(
                    window.HTMLInputElement.prototype, 'value'
                );
                if (nativeInputValueSetter) {
                    nativeInputValueSetter.set.call(element, value);
                }
                element.dispatchEvent(new Event('input', { bubbles: true }));
                element.dispatchEvent(new Event('change', { bubbles: true }));
            }

            // ── Set value on ContentEditable (Claude's ProseMirror) ──────
            function setContentEditableValue(element, value) {
                element.focus();
                document.execCommand('selectAll', false, null);
                document.execCommand('insertText', false, value);
                element.dispatchEvent(new InputEvent('input', { bubbles: true, inputType: 'insertText', data: value }));
            }

            // ── Extract text from response elements ──────────────────────
            function extractText(element) {
                return element ? element.innerText || element.textContent || '' : '';
            }

            // ── Stop the observer and signal completion ──────────────────
            function signalComplete(fullText) {
                if (window._dettleObserver) {
                    window._dettleObserver.disconnect();
                    window._dettleObserver = null;
                }
                window._dettleStreaming = false;

                // Scan for embedded tool calls
                const toolCallMatch = fullText.match(/<tool_call>([\s\S]*?)<\/tool_call>/);
                if (toolCallMatch) {
                    try {
                        const parsed = JSON.parse(toolCallMatch[1]);
                        AndroidBridge.onToolCallDetected(JSON.stringify(parsed));
                    } catch(e) {
                        AndroidBridge.log('Tool call parse failed: ' + e.message);
                    }
                }

                AndroidBridge.onComplete(fullText);
            }

            // ── Watch for streaming response ─────────────────────────────
            function startObserver() {
                window._dettleLastText = '';
                window._dettleStreaming = true;

                // Wait up to 3s for the response container to appear
                let attempts = 0;
                const waitForResponse = setInterval(function() {
                    attempts++;
                    const responseEls = document.querySelectorAll(window._dettleSelectors.response);
                    const lastEl = responseEls.length > 0 ? responseEls[responseEls.length - 1] : null;

                    if (lastEl || attempts > 30) {
                        clearInterval(waitForResponse);
                        if (!lastEl) {
                            AndroidBridge.onError('Response element not found after 3s');
                            return;
                        }

                        window._dettleObserver = new MutationObserver(function() {
                            const currentText = extractText(lastEl);
                            if (currentText !== window._dettleLastText) {
                                const newChunk = currentText.slice(window._dettleLastText.length);
                                if (newChunk) {
                                    AndroidBridge.onChunkReceived(newChunk);
                                }
                                window._dettleLastText = currentText;
                            }

                            // Check if streaming has stopped
                            const isStreaming = document.querySelector(window._dettleSelectors.streamingIndicator) !== null;
                            if (!isStreaming && window._dettleStreaming && window._dettleLastText.length > 0) {
                                // Wait 300ms to make sure it's really done (not just a brief pause)
                                setTimeout(function() {
                                    const streamingNow = document.querySelector(window._dettleSelectors.streamingIndicator) !== null;
                                    if (!streamingNow) {
                                        signalComplete(window._dettleLastText);
                                    }
                                }, 300);
                            }
                        });

                        window._dettleObserver.observe(lastEl, {
                            childList: true,
                            subtree: true,
                            characterData: true
                        });
                    }
                }, 100);
            }

            // ── Main: send a prompt ──────────────────────────────────────
            window.dettleSendPrompt = function(text) {
                // 1. Check login
                if (!checkLogin()) {
                    AndroidBridge.onNeedsLogin();
                    return;
                }

                // 2. Find and fill input
                const input = document.querySelector(window._dettleSelectors.input);
                if (!input) {
                    AndroidBridge.onError('Input field not found: ' + window._dettleSelectors.input);
                    return;
                }

                // Handle both textarea and contenteditable
                if (input.tagName === 'TEXTAREA' || input.tagName === 'INPUT') {
                    setReactValue(input, text);
                } else {
                    setContentEditableValue(input, text);
                }

                AndroidBridge.log('Input filled, length=' + text.length);

                // 3. Wait briefly then click Send
                setTimeout(function() {
                    const sendBtn = document.querySelector(window._dettleSelectors.send);
                    if (!sendBtn) {
                        AndroidBridge.onError('Send button not found: ' + window._dettleSelectors.send);
                        return;
                    }

                    sendBtn.click();
                    AndroidBridge.log('Send button clicked');

                    // 4. Start watching for the response
                    startObserver();
                }, 100);
            };

            // ── Report ready ─────────────────────────────────────────────
            if (document.readyState === 'complete') {
                AndroidBridge.onReady();
            } else {
                window.addEventListener('load', function() {
                    AndroidBridge.onReady();
                });
            }

            AndroidBridge.log('Dettle automation script loaded');
        })();
    """.trimIndent()

    /**
     * Inject an updated selector config without reloading the page.
     * Call this when the remote registry updates during a session.
     */
    fun buildSelectorUpdateScript(selectors: ProviderSelectors): String = """
        window._dettleSelectors = {
            input: '${selectors.input}',
            send: '${selectors.send}',
            response: '${selectors.response}',
            streamingIndicator: '${selectors.streamingIndicator}',
            loginCheck: '${selectors.loginCheck}'
        };
        AndroidBridge.log('Selectors updated');
    """.trimIndent()

    /** Trigger a prompt send from Kotlin */
    fun buildSendScript(prompt: String): String {
        // Escape the prompt for safe JS string embedding
        val escaped = prompt
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "")
        return "window.dettleSendPrompt('$escaped');"
    }
}
