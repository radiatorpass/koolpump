package koolpump.user

class CsrfInterceptor {

    SecurityTagLib securityTagLib = new SecurityTagLib()

    CsrfInterceptor() {
        // Apply to all POST, PUT, DELETE requests except specific endpoints
        match(method: 'POST')
        match(method: 'PUT')
        match(method: 'DELETE')
        match(method: 'PATCH')
        
        // Exclude Stripe webhook and OAuth callbacks
        except(controller: 'stripeWebhook')
        except(controller: 'oauth', action: 'googleCallback')
        except(controller: 'oauth', action: 'microsoftCallback')
    }

    boolean before() {
        // Skip CSRF check for API endpoints with Bearer token
        def authHeader = request.getHeader('Authorization')
        if (authHeader?.startsWith('Bearer ')) {
            return true
        }
        
        // Skip for requests with valid API key
        def apiKey = request.getHeader('X-API-Key')
        if (apiKey && validateApiKey(apiKey)) {
            return true
        }
        
        // Validate CSRF token for form submissions
        if (request.method in ['POST', 'PUT', 'DELETE', 'PATCH']) {
            def sessionToken = session.csrfToken
            def requestToken = params.csrfToken ?: request.getHeader('X-CSRF-Token')
            
            if (!sessionToken || !requestToken || sessionToken != requestToken) {
                log.warn "CSRF token validation failed for ${request.forwardURI} from ${request.remoteAddr}"
                
                response.status = 403
                render(view: '/error/csrf', model: [message: 'Invalid or missing CSRF token'])
                return false
            }
        }
        
        return true
    }

    boolean after() { 
        // Generate new CSRF token after successful POST to prevent token fixation
        if (request.method == 'POST' && response.status == 200) {
            session.csrfToken = generateNewToken()
        }
        return true 
    }

    void afterView() {
    }
    
    private boolean validateApiKey(String apiKey) {
        // Implement API key validation logic
        // This is a placeholder - implement proper API key validation
        return false
    }
    
    private String generateNewToken() {
        byte[] bytes = new byte[32]
        new java.security.SecureRandom().nextBytes(bytes)
        return bytes.encodeBase64().toString()
    }
}