;(function () {
  'use strict'

  var expectedParentOrigin = null
  var bridgeScriptLoaded = false

  // Read expected parent origin from query parameter
  try {
    var urlParams = new URLSearchParams(window.location.search)
    expectedParentOrigin = urlParams.get('_parentOrigin')
    console.log('[editor-bootstrap] Expected parent origin:', expectedParentOrigin)
  } catch (e) {
    console.error('[editor-bootstrap] Failed to read parent origin:', e)
  }

  // Listen for bridge script code from parent
  function handleBootstrapMessage(event) {
    var data = event.data
    if (!data || !data.type) return

    // CRITICAL SECURITY: Require _parentOrigin query param
    // Without this, any embedding parent could inject malicious code via eval
    if (!expectedParentOrigin) {
      console.error('[editor-bootstrap] Message rejected: _parentOrigin query param missing (security requirement)')
      return
    }

    // Validate origin matches the expected parent origin
    if (event.origin !== expectedParentOrigin) {
      console.error('[editor-bootstrap] Message rejected: origin mismatch', {
        expected: expectedParentOrigin,
        received: event.origin
      })
      return
    }

    // Validate source is parent window (prevents spoofing from sibling iframes)
    if (event.source !== window.parent) {
      console.error('[editor-bootstrap] Message rejected: not from parent')
      return
    }

    if (data.type === 'EDITOR_LOAD_SCRIPT') {
      if (bridgeScriptLoaded) {
        console.warn('[editor-bootstrap] Bridge script already loaded, ignoring')
        return
      }

      try {
        // Execute the bridge script code
        console.log('[editor-bootstrap] Loading bridge script...')
        var scriptCode = data.payload.code

        // Use indirect eval to execute in global scope
        var executeGlobal = eval
        executeGlobal(scriptCode)

        bridgeScriptLoaded = true
        console.log('[editor-bootstrap] Bridge script loaded successfully')

        // Remove bootstrap listener (bridge will add its own)
        window.removeEventListener('message', handleBootstrapMessage)
      } catch (e) {
        console.error('[editor-bootstrap] Failed to load bridge script:', e)
      }
    }
  }

  window.addEventListener('message', handleBootstrapMessage, false)

  // Notify parent that bootstrap is ready
  try {
    // Note: Using '*' for initial handshake is acceptable here because:
    // 1. BOOTSTRAP_READY contains no sensitive data (empty payload)
    // 2. All subsequent messages (EDITOR_LOAD_SCRIPT) require origin validation
    // 3. The bridge script (loaded after this) enforces strict origin checking
    window.parent.postMessage({ type: 'BOOTSTRAP_READY', payload: {} }, '*')
    console.log('[editor-bootstrap] Bootstrap ready, waiting for script...')
  } catch (e) {
    console.error('[editor-bootstrap] Failed to notify parent:', e)
  }
})()
