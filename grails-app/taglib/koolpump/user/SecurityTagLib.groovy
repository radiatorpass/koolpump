package koolpump.user

import java.security.SecureRandom

class SecurityTagLib {
    
    static namespace = "kpSec"
    
    private static final SecureRandom random = new SecureRandom()
    
    /**
     * Generates a CSRF token and stores it in the session
     * Usage: <kpSec:csrfToken/>
     */
    def csrfToken = { attrs ->
        def token = session.csrfToken
        if (!token) {
            token = generateToken()
            session.csrfToken = token
        }
        
        out << """<input type="hidden" name="csrfToken" value="${token}"/>"""
    }
    
    /**
     * Generates a meta tag with CSRF token for AJAX requests
     * Usage: <kpSec:csrfMeta/>
     */
    def csrfMeta = { attrs ->
        def token = session.csrfToken
        if (!token) {
            token = generateToken()
            session.csrfToken = token
        }
        
        out << """<meta name="csrf-token" content="${token}"/>"""
    }
    
    /**
     * Validates CSRF token from request
     * Usage: <g:if test="${kpSec.validateCsrf()}">...</g:if>
     */
    def validateCsrf = { attrs ->
        def sessionToken = session.csrfToken
        def requestToken = params.csrfToken ?: request.getHeader('X-CSRF-Token')
        
        return sessionToken && requestToken && sessionToken == requestToken
    }
    
    /**
     * Rate limit check
     * Usage: <g:if test="${kpSec.isRateLimited(key: 'login', max: 5)}">...</g:if>
     */
    def isRateLimited = { attrs ->
        def key = attrs.key ?: 'default'
        def max = attrs.int('max') ?: 10
        def duration = attrs.int('duration') ?: 60 // seconds
        
        def cacheKey = "ratelimit:${key}:${request.remoteAddr}"
        def count = session[cacheKey] ?: 0
        
        return count >= max
    }
    
    /**
     * Increment rate limit counter
     * Usage: <kpSec:incrementRateLimit key="login"/>
     */
    def incrementRateLimit = { attrs ->
        def key = attrs.key ?: 'default'
        def cacheKey = "ratelimit:${key}:${request.remoteAddr}"
        
        def count = session[cacheKey] ?: 0
        session[cacheKey] = count + 1
        
        // Set expiry (simplified - in production use proper cache with TTL)
        if (count == 0) {
            session["${cacheKey}:timestamp"] = System.currentTimeMillis()
        }
    }
    
    /**
     * Sanitize HTML output
     * Usage: <kpSec:sanitize>${untrustedContent}</kpSec:sanitize>
     */
    def sanitize = { attrs, body ->
        def content = body()
        out << org.owasp.encoder.Encode.forHtml(content)
    }
    
    /**
     * Check if user has role
     * Usage: <kpSec:ifRole role="ADMIN">...</kpSec:ifRole>
     */
    def ifRole = { attrs, body ->
        def role = attrs.role
        def user = session.user
        
        if (user && role == 'ADMIN' && user.isAdmin) {
            out << body()
        } else if (user && role == 'USER') {
            out << body()
        }
    }
    
    
    private String generateToken() {
        byte[] bytes = new byte[32]
        random.nextBytes(bytes)
        return bytes.encodeBase64().toString()
    }
}