package koolpump.user

import koolpump.user.Subscription
import koolpump.user.UserActivityLog

class TrialExpiryJob {
    
    EmailService emailService
    UserService userService
    
    static triggers = {
        // Run daily at 2 AM
        cron name: 'trialExpiryTrigger', cronExpression: '0 0 2 * * ?'
    }

    def execute() {
        log.info "Starting trial expiry check job..."
        
        checkExpiringTrials()
        expireTrials()
        
        log.info "Trial expiry check job completed"
    }
    
    private void checkExpiringTrials() {
        // Send reminders for trials expiring in 3 days
        def threeDaysFromNow = new Date() + 3
        def fourDaysFromNow = new Date() + 4
        
        def expiringTrials = Subscription.findAllByStatusAndTrialEndDateBetween(
            'TRIAL', 
            threeDaysFromNow.clearTime(), 
            fourDaysFromNow.clearTime()
        )
        
        expiringTrials.each { subscription ->
            try {
                emailService.sendTrialExpiryReminder(subscription.user, 3)
                userService.logActivity(subscription.user, 'TRIAL_EXPIRY_REMINDER_SENT', 'SYSTEM')
                log.info "Sent trial expiry reminder to ${subscription.user.email}"
            } catch (Exception e) {
                log.error "Failed to send trial expiry reminder to ${subscription.user.email}: ${e.message}"
            }
        }
        
        // Send reminders for trials expiring tomorrow
        def tomorrow = new Date() + 1
        def dayAfterTomorrow = new Date() + 2
        
        def expiringTomorrow = Subscription.findAllByStatusAndTrialEndDateBetween(
            'TRIAL',
            tomorrow.clearTime(),
            dayAfterTomorrow.clearTime()
        )
        
        expiringTomorrow.each { subscription ->
            try {
                emailService.sendTrialExpiryReminder(subscription.user, 1)
                userService.logActivity(subscription.user, 'TRIAL_FINAL_REMINDER_SENT', 'SYSTEM')
                log.info "Sent final trial reminder to ${subscription.user.email}"
            } catch (Exception e) {
                log.error "Failed to send final trial reminder to ${subscription.user.email}: ${e.message}"
            }
        }
    }
    
    private void expireTrials() {
        // Expire trials that have ended
        def today = new Date().clearTime()
        
        def expiredTrials = Subscription.findAllByStatusAndTrialEndDateLessThan('TRIAL', today)
        
        expiredTrials.each { subscription ->
            try {
                subscription.status = 'EXPIRED'
                subscription.endDate = subscription.trialEndDate
                subscription.save(flush: true)
                
                userService.logActivity(subscription.user, 'TRIAL_EXPIRED', 'SYSTEM')
                log.info "Expired trial for user ${subscription.user.email}"
            } catch (Exception e) {
                log.error "Failed to expire trial for ${subscription.user.email}: ${e.message}"
            }
        }
    }
}