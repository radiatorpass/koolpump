package koolpump.user

import koolpump.user.KpUser
import koolpump.user.UserToken
import koolpump.user.UserActivityLog

class PasswordExpiryJob {
    
    UserService userService
    EmailService emailService
    
    static triggers = {
        // Run daily at 3 AM
        cron name: 'passwordExpiryTrigger', cronExpression: '0 0 3 * * ?'
    }

    def execute() {
        log.info "Starting password expiry check job..."
        
        checkPasswordExpiry()
        cleanupExpiredTokens()
        
        log.info "Password expiry check job completed"
    }
    
    private void checkPasswordExpiry() {
        // Check for passwords that haven't been changed in 90 days
        def ninetyDaysAgo = new Date() - 90
        
        // Find users who haven't changed password in 90 days
        def lastPasswordChanges = UserActivityLog.executeQuery(
            """SELECT DISTINCT u.user 
               FROM UserActivityLog u 
               WHERE u.activity = 'PASSWORD_CHANGED' 
               AND u.timestamp = (
                   SELECT MAX(u2.timestamp) 
                   FROM UserActivityLog u2 
                   WHERE u2.user = u.user 
                   AND u2.activity = 'PASSWORD_CHANGED'
               )
               AND u.timestamp < :cutoffDate""",
            [cutoffDate: ninetyDaysAgo]
        )
        
        lastPasswordChanges.each { user ->
            if (!user.passwordExpired) {
                user.passwordExpired = true
                user.save(flush: true)
                
                userService.logActivity(user, 'PASSWORD_EXPIRED', 'SYSTEM')
                log.info "Marked password as expired for user ${user.email}"
                
                // Send notification email
                try {
                    def mailService = grails.util.Holders.applicationContext.getBean("mailService")
                    mailService.sendMail {
                        to user.email
                        from 'noreply@koolpump.com'
                        subject 'KoolPump - Password Expired'
                        html """
                            <p>Hello ${user.firstName},</p>
                            <p>Your password has expired due to our security policy.</p>
                            <p>Please log in and change your password to continue using KoolPump.</p>
                            <p>Best regards,<br>KoolPump Team</p>
                        """
                    }
                } catch (Exception e) {
                    log.error "Failed to send password expiry email to ${user.email}: ${e.message}"
                }
            }
        }
        
        // Send warning for passwords expiring in 7 days
        def eightyThreeDaysAgo = new Date() - 83
        def eightyFourDaysAgo = new Date() - 84
        
        def warningUsers = UserActivityLog.executeQuery(
            """SELECT DISTINCT u.user 
               FROM UserActivityLog u 
               WHERE u.activity = 'PASSWORD_CHANGED' 
               AND u.timestamp BETWEEN :start AND :end
               AND u.timestamp = (
                   SELECT MAX(u2.timestamp) 
                   FROM UserActivityLog u2 
                   WHERE u2.user = u.user 
                   AND u2.activity = 'PASSWORD_CHANGED'
               )""",
            [start: eightyFourDaysAgo, end: eightyThreeDaysAgo]
        )
        
        warningUsers.each { user ->
            try {
                def mailService = grails.util.Holders.applicationContext.getBean("mailService")
                mailService.sendMail {
                    to user.email
                    from 'noreply@koolpump.com'
                    subject 'KoolPump - Password Expiring Soon'
                    html """
                        <p>Hello ${user.firstName},</p>
                        <p>Your password will expire in 7 days.</p>
                        <p>Please log in and change your password to avoid any interruption.</p>
                        <p>Best regards,<br>KoolPump Team</p>
                    """
                }
                userService.logActivity(user, 'PASSWORD_EXPIRY_WARNING_SENT', 'SYSTEM')
                log.info "Sent password expiry warning to ${user.email}"
            } catch (Exception e) {
                log.error "Failed to send password expiry warning to ${user.email}: ${e.message}"
            }
        }
    }
    
    private void cleanupExpiredTokens() {
        // Delete expired tokens
        def now = new Date()
        
        def expiredTokens = UserToken.findAllByExpiryDateLessThan(now)
        def count = expiredTokens.size()
        
        expiredTokens.each { token ->
            try {
                token.delete(flush: true)
            } catch (Exception e) {
                log.error "Failed to delete expired token ${token.id}: ${e.message}"
            }
        }
        
        if (count > 0) {
            log.info "Deleted ${count} expired tokens"
        }
    }
}