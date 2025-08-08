package koolpump.user

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import koolpump.user.Subscription
import koolpump.user.Payment
import koolpump.user.KpUser

@Transactional
@Slf4j
class EmailService {

    def grailsApplication

    def sendRegistrationEmail(KpUser user, String token) {
        try {
            def mailService = grails.util.Holders.applicationContext.getBean("mailService")
            def baseUrl = grailsApplication.config.app.baseUrl ?: "http://www.koolpump.com"
            def verificationUrl = "${baseUrl}/registration/verify?token=${token}"
            
            mailService.sendMail {
                to user.email
                from 'noreply@koolpump.com'
                subject 'Welcome to KoolPump - Verify Your Email'
                html view: '/email/registration', model: [user: user, verificationUrl: verificationUrl]
            }
            
            log.info "Registration email sent successfully to ${user.email}"
            return [success: true]
        } catch (Exception e) {
            log.error "Failed to send registration email to ${user.email}: ${e.message}", e
            return [success: false, error: e.message]
        }
    }

    def sendPasswordResetEmail(KpUser user, String token) {
        try {
            def mailService = grails.util.Holders.applicationContext.getBean("mailService")
            def baseUrl = grailsApplication.config.app.baseUrl ?: "http://www.koolpump.com"
            def resetUrl = "${baseUrl}/password-reset/reset?token=${token}"
            
            mailService.sendMail {
                to user.email
                from 'noreply@koolpump.com'
                subject 'KoolPump - Password Reset Request'
                html view: '/email/passwordReset', model: [user: user, resetUrl: resetUrl]
            }
            
            log.info "Password reset email sent successfully to ${user.email}"
            return [success: true]
        } catch (Exception e) {
            log.error "Failed to send password reset email to ${user.email}: ${e.message}", e
            return [success: false, error: e.message]
        }
    }

    def sendSubscriptionConfirmation(KpUser user, Subscription subscription) {
        try {
            def mailService = grails.util.Holders.applicationContext.getBean("mailService")
            
            mailService.sendMail {
                to user.email
                from 'noreply@koolpump.com'
                subject 'KoolPump - Subscription Confirmed'
                html view: '/email/subscriptionConfirmation', model: [user: user, subscription: subscription]
            }
            
            log.info "Subscription confirmation email sent successfully to ${user.email}"
            return [success: true]
        } catch (Exception e) {
            log.error "Failed to send subscription confirmation email to ${user.email}: ${e.message}", e
            return [success: false, error: e.message]
        }
    }

    def sendTrialExpiryReminder(KpUser user, int daysRemaining) {
        try {
            def mailService = grails.util.Holders.applicationContext.getBean("mailService")
            
            mailService.sendMail {
                to user.email
                from 'noreply@koolpump.com'
                subject "KoolPump - Your trial expires in ${daysRemaining} days"
                html view: '/email/trialExpiry', model: [user: user, daysRemaining: daysRemaining]
            }
            
            log.info "Trial expiry reminder email sent successfully to ${user.email}"
            return [success: true]
        } catch (Exception e) {
            log.error "Failed to send trial expiry reminder email to ${user.email}: ${e.message}", e
            return [success: false, error: e.message]
        }
    }

    def sendPaymentFailedNotification(KpUser user, Payment payment) {
        try {
            def mailService = grails.util.Holders.applicationContext.getBean("mailService")
            
            mailService.sendMail {
                to user.email
                from 'noreply@koolpump.com'
                subject 'KoolPump - Payment Failed'
                html view: '/email/paymentFailed', model: [user: user, payment: payment]
            }
            
            log.info "Payment failed notification email sent successfully to ${user.email}"
            return [success: true]
        } catch (Exception e) {
            log.error "Failed to send payment failed notification email to ${user.email}: ${e.message}", e
            return [success: false, error: e.message]
        }
    }

    def sendPaymentSuccessNotification(KpUser user, Payment payment) {
        try {
            def mailService = grails.util.Holders.applicationContext.getBean("mailService")
            
            mailService.sendMail {
                to user.email
                from 'noreply@koolpump.com'
                subject 'KoolPump - Payment Successful'
                html view: '/email/paymentSuccess', model: [user: user, payment: payment]
            }
            
            log.info "Payment success notification email sent successfully to ${user.email}"
            return [success: true]
        } catch (Exception e) {
            log.error "Failed to send payment success notification email to ${user.email}: ${e.message}", e
            return [success: false, error: e.message]
        }
    }
}