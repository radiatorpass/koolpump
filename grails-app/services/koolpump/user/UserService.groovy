package koolpump.user

import grails.gorm.transactions.Transactional
import koolpump.user.UserActivityLog
import koolpump.user.KpUser
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.security.MessageDigest
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit

@Transactional
class UserService {

    // Rate limiting cache: email -> attempt count
    private static final Cache<String, Integer> loginAttemptCache = Caffeine.newBuilder()
        .expireAfterWrite(15, TimeUnit.MINUTES)
        .maximumSize(10000)
        .build()
    
    private static final int MAX_LOGIN_ATTEMPTS = 5
    private static final int LOCKOUT_DURATION_MINUTES = 15

    def authenticate(String email, String password) {
        // Check rate limiting
        if (isBlocked(email)) {
            logActivity(null, 'LOGIN_BLOCKED', email)
            return [success: false, error: "Too many failed attempts. Please try again later.", blocked: true]
        }
        
        def user = KpUser.findByEmailAndEnabled(email, true)
        
        if (user && validatePassword(password, user.password)) {
            if (user.accountLocked) {
                return [success: false, error: "Account is locked", user: null]
            }
            if (user.accountExpired) {
                return [success: false, error: "Account has expired", user: null]
            }
            
            // Reset login attempts on successful login
            resetLoginAttempts(email)
            return [success: true, user: user]
        }
        
        // Record failed attempt
        recordFailedAttempt(email)
        return [success: false, error: "Invalid credentials", user: null]
    }

    def hashPassword(String password) {
        // Use BCrypt with a work factor of 12 (good balance of security and performance)
        return BCrypt.hashpw(password, BCrypt.gensalt(12))
    }

    def validatePassword(String plainPassword, String hashedPassword) {
        try {
            // Handle both BCrypt and legacy SHA-256 passwords
            if (hashedPassword.startsWith('$2a$') || hashedPassword.startsWith('$2b$')) {
                // BCrypt password
                return BCrypt.checkpw(plainPassword, hashedPassword)
            } else {
                // Legacy SHA-256 password - validate and migrate
                def legacyValid = validateLegacyPassword(plainPassword, hashedPassword)
                if (legacyValid) {
                    // Migrate to BCrypt on successful login
                    log.info "Migrating password from SHA-256 to BCrypt for user"
                }
                return legacyValid
            }
        } catch (Exception e) {
            log.error "Password validation error: ${e.message}"
            return false
        }
    }
    
    private boolean validateLegacyPassword(String plainPassword, String hashedPassword) {
        MessageDigest md = MessageDigest.getInstance("SHA-256")
        md.update(plainPassword.getBytes("UTF-8"))
        byte[] digest = md.digest()
        return digest.encodeBase64().toString() == hashedPassword
    }
    
    private boolean isBlocked(String email) {
        Integer attempts = loginAttemptCache.getIfPresent(email)
        return attempts != null && attempts >= MAX_LOGIN_ATTEMPTS
    }
    
    private void recordFailedAttempt(String email) {
        Integer attempts = loginAttemptCache.getIfPresent(email) ?: 0
        loginAttemptCache.put(email, attempts + 1)
        
        if (attempts + 1 >= MAX_LOGIN_ATTEMPTS) {
            log.warn "KpUser ${email} has been temporarily blocked due to ${MAX_LOGIN_ATTEMPTS} failed login attempts"
        }
    }
    
    private void resetLoginAttempts(String email) {
        loginAttemptCache.invalidate(email)
    }

    def generateToken() {
        SecureRandom random = new SecureRandom()
        byte[] bytes = new byte[32]
        random.nextBytes(bytes)
        return bytes.encodeBase64().toString().replaceAll("[^A-Za-z0-9]", "")
    }

    def generateVerificationToken(KpUser user) {
        def token = generateToken()
        def expiryDate = new Date() + 1 // Expires in 24 hours
        
        def userToken = new UserToken(
            user: user,
            token: token,
            tokenType: 'EMAIL_VERIFICATION',
            expiryDate: expiryDate
        )
        userToken.save(flush: true)
        
        return token
    }

    def generateResetToken(KpUser user) {
        def token = generateToken()
        def expiryDate = new Date() + 0.5 // Expires in 12 hours
        
        def userToken = new UserToken(
            user: user,
            token: token,
            tokenType: 'RESET_PASSWORD',
            expiryDate: expiryDate
        )
        userToken.save(flush: true)
        
        return token
    }

    def revokeExistingTokens(KpUser user, String tokenType) {
        def tokens = UserToken.findAllByUserAndTokenTypeAndUsed(user, tokenType, false)
        tokens.each { token ->
            token.used = true
            token.save(flush: true)
        }
    }

    def revokeAllTokens(KpUser user) {
        def tokens = UserToken.findAllByUserAndUsed(user, false)
        tokens.each { token ->
            token.used = true
            token.save(flush: true)
        }
    }

    def logActivity(KpUser user, String activity, String ipAddress) {
        def log = new UserActivityLog(
            user: user,
            activity: activity,
            ipAddress: ipAddress,
            timestamp: new Date()
        )
        log.save(flush: true)
    }

    def getRecentActivity(KpUser user, int limit = 10) {
        return UserActivityLog.findAllByUser(user, [max: limit, sort: 'timestamp', order: 'desc'])
    }

    def getActiveSessions(KpUser user) {
        def cutoffTime = new Date() - 0.02083 // 30 minutes ago
        return UserActivityLog.findAllByUserAndActivityAndTimestampGreaterThan(
            user, 'LOGIN', cutoffTime, [sort: 'timestamp', order: 'desc']
        )
    }

    def createUserFromOAuth(String email, String firstName, String lastName, String provider, String providerId) {
        def user = new KpUser(
            email: email,
            firstName: firstName,
            lastName: lastName,
            password: generateToken(), // Random password for OAuth users
            enabled: true,
            oauthProvider: provider,
            oauthProviderId: providerId
        )
        user.save(flush: true)
        return user
    }

    def findOrCreateOAuthUser(String email, String firstName, String lastName, String provider, String providerId) {
        def user = KpUser.findByEmailAndOauthProvider(email, provider)
        
        if (!user) {
            user = KpUser.findByEmail(email)
            if (user) {
                // Link existing account with OAuth
                user.oauthProvider = provider
                user.oauthProviderId = providerId
                user.save(flush: true)
            } else {
                // Create new OAuth user
                user = createUserFromOAuth(email, firstName, lastName, provider, providerId)
            }
        }
        
        return user
    }
}