package koolpump.user

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import koolpump.user.KpUser

@Transactional
@Slf4j
class NewsletterService {

    EmailService emailService
    UserService userService
    def grailsApplication

    def createNewsletter(String subject, String content, String contentHtml, KpUser createdBy) {
        try {
            def newsletter = new Newsletter(
                subject: subject,
                content: content,
                contentHtml: contentHtml ?: convertToHtml(content),
                status: 'DRAFT',
                createdBy: createdBy
            )
            
            if (newsletter.save(flush: true)) {
                userService.logActivity(createdBy, 'NEWSLETTER_CREATED', 'SYSTEM')
                log.info "Newsletter created successfully: ${newsletter.subject}"
                return newsletter
            }
            
            log.warn "Failed to save newsletter: ${newsletter.errors}"
            return null
        } catch (Exception e) {
            log.error "Failed to create newsletter: ${e.message}", e
            return null
        }
    }

    def scheduleNewsletter(Newsletter newsletter, Date scheduledDate) {
        try {
            newsletter.status = 'SCHEDULED'
            newsletter.scheduledDate = scheduledDate
            
            if (newsletter.save(flush: true)) {
                userService.logActivity(newsletter.createdBy, 'NEWSLETTER_SCHEDULED', 'SYSTEM')
                log.info "Newsletter scheduled successfully: ${newsletter.subject} for ${scheduledDate}"
                return true
            }
            
            log.warn "Failed to schedule newsletter: ${newsletter.errors}"
            return false
        } catch (Exception e) {
            log.error "Failed to schedule newsletter ${newsletter.subject}: ${e.message}", e
            return false
        }
    }

    def sendNewsletter(Newsletter newsletter, boolean testMode = false) {
        if (newsletter.status == 'SENT') {
            return [success: false, error: "Newsletter already sent"]
        }
        
        newsletter.status = 'SENDING'
        newsletter.save(flush: true)
        
        def recipients = testMode ? 
            [newsletter.createdBy] : 
            KpUser.findAllBySubscribeNewsletterAndEnabled(true, true)
        
        newsletter.recipientCount = recipients.size()
        newsletter.successCount = 0
        newsletter.failureCount = 0
        
        // Use thread pool for bulk sending
        def executor = Executors.newFixedThreadPool(5)
        
        try {
            recipients.each { user ->
                executor.submit {
                    try {
                        sendNewsletterToUser(newsletter, user)
                        newsletter.successCount++
                    } catch (Exception e) {
                        log.error "Failed to send newsletter to ${user.email}: ${e.message}"
                        newsletter.failureCount++
                    }
                }
            }
            
            executor.shutdown()
            executor.awaitTermination(30, TimeUnit.MINUTES)
            
            newsletter.status = newsletter.failureCount == 0 ? 'SENT' : 'SENT_WITH_ERRORS'
            newsletter.sentDate = new Date()
            newsletter.save(flush: true)
            
            userService.logActivity(newsletter.createdBy, 'NEWSLETTER_SENT', 'SYSTEM')
            
            return [
                success: true,
                recipientCount: newsletter.recipientCount,
                successCount: newsletter.successCount,
                failureCount: newsletter.failureCount
            ]
        } catch (Exception e) {
            newsletter.status = 'FAILED'
            newsletter.failureReason = e.message
            newsletter.save(flush: true)
            
            return [success: false, error: e.message]
        }
    }

    private void sendNewsletterToUser(Newsletter newsletter, KpUser user) {
        try {
            def mailService = grails.util.Holders.applicationContext.getBean("mailService")
            
            def unsubscribeUrl = "${grailsApplication.config.app.baseUrl}/newsletter/unsubscribe?token=${generateUnsubscribeToken(user)}"
            
            def htmlContent = newsletter.contentHtml ?: newsletter.content
            htmlContent += """
                <hr>
                <p style="font-size: 12px; color: #999;">
                    You're receiving this email because you're subscribed to KoolPump newsletter.
                    <br>
                    <a href="${unsubscribeUrl}">Unsubscribe</a> | 
                    <a href="${grailsApplication.config.app.baseUrl}/dashboard/profile">Update Preferences</a>
                </p>
            """
            
            mailService.sendMail {
                to user.email
                from 'newsletter@koolpump.com'
                subject newsletter.subject
                html htmlContent
            }
        } catch (Exception e) {
            log.error "Failed to send newsletter to ${user.email}: ${e.message}", e
            throw e // Re-throw so it's caught by the calling method
        }
    }

    def getNewsletterStats() {
        try {
            def total = Newsletter.count()
            def sent = Newsletter.countByStatus('SENT')
            def scheduled = Newsletter.countByStatus('SCHEDULED')
            def draft = Newsletter.countByStatus('DRAFT')
            
            def totalRecipients = Newsletter.findAllByStatus('SENT').sum { it.recipientCount } ?: 0
            def totalSuccess = Newsletter.findAllByStatus('SENT').sum { it.successCount } ?: 0
            
            return [
                totalNewsletters: total,
                sentNewsletters: sent,
                scheduledNewsletters: scheduled,
                draftNewsletters: draft,
                totalRecipients: totalRecipients,
                deliveryRate: totalRecipients > 0 ? (totalSuccess / totalRecipients * 100).round(2) : 0
            ]
        } catch (Exception e) {
            log.error "Failed to get newsletter stats: ${e.message}", e
            return [
                totalNewsletters: 0,
                sentNewsletters: 0,
                scheduledNewsletters: 0,
                draftNewsletters: 0,
                totalRecipients: 0,
                deliveryRate: 0
            ]
        }
    }

    def getSubscribers() {
        try {
            return KpUser.findAllBySubscribeNewsletterAndEnabled(true, true)
        } catch (Exception e) {
            log.error "Failed to get newsletter subscribers: ${e.message}", e
            return []
        }
    }

    def subscribeUser(KpUser user) {
        try {
            user.subscribeNewsletter = true
            
            if (user.save(flush: true)) {
                userService.logActivity(user, 'NEWSLETTER_SUBSCRIBED', 'SYSTEM')
                
                // Send welcome email
                try {
                    def mailService = grails.util.Holders.applicationContext.getBean("mailService")
                    mailService.sendMail {
                        to user.email
                        from 'newsletter@koolpump.com'
                        subject 'Welcome to KoolPump Newsletter'
                        html """
                            <h2>Welcome to KoolPump Newsletter!</h2>
                            <p>Hello ${user.firstName},</p>
                            <p>Thank you for subscribing to our newsletter. You'll receive updates about:</p>
                            <ul>
                                <li>New heat pump models and technologies</li>
                                <li>Energy efficiency tips and guides</li>
                                <li>Regulatory updates and incentives</li>
                                <li>Industry news and trends</li>
                            </ul>
                            <p>You can manage your subscription preferences in your dashboard.</p>
                            <p>Best regards,<br>The KoolPump Team</p>
                        """
                    }
                    log.info "Welcome email sent to new subscriber: ${user.email}"
                } catch (Exception e) {
                    log.error "Failed to send welcome email to ${user.email}: ${e.message}", e
                }
                
                log.info "KpUser subscribed to newsletter: ${user.email}"
                return true
            }
            
            log.warn "Failed to subscribe user to newsletter: ${user.errors}"
            return false
        } catch (Exception e) {
            log.error "Failed to subscribe user ${user.email} to newsletter: ${e.message}", e
            return false
        }
    }

    def unsubscribeUser(KpUser user) {
        try {
            user.subscribeNewsletter = false
            
            if (user.save(flush: true)) {
                userService.logActivity(user, 'NEWSLETTER_UNSUBSCRIBED', 'SYSTEM')
                log.info "KpUser unsubscribed from newsletter: ${user.email}"
                return true
            }
            
            log.warn "Failed to unsubscribe user from newsletter: ${user.errors}"
            return false
        } catch (Exception e) {
            log.error "Failed to unsubscribe user ${user.email} from newsletter: ${e.message}", e
            return false
        }
    }

    def unsubscribeByToken(String token) {
        try {
            def user = findUserByUnsubscribeToken(token)
            
            if (user) {
                return unsubscribeUser(user)
            }
            
            log.warn "Invalid or expired unsubscribe token used"
            return false
        } catch (Exception e) {
            log.error "Failed to unsubscribe by token: ${e.message}", e
            return false
        }
    }

    private String generateUnsubscribeToken(KpUser user) {
        // Simple token generation - in production, use JWT or signed tokens
        def data = "${user.id}:${user.email}:${System.currentTimeMillis()}"
        return data.encodeAsBase64()
    }

    private KpUser findUserByUnsubscribeToken(String token) {
        try {
            def decoded = new String(token.decodeBase64())
            def parts = decoded.split(':')
            
            if (parts.length >= 2) {
                def userId = parts[0] as Long
                return KpUser.get(userId)
            }
        } catch (Exception e) {
            log.error "Invalid unsubscribe token: ${e.message}"
        }
        
        return null
    }

    private String convertToHtml(String plainText) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
                              color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background: #f8f9fa; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>KoolPump Newsletter</h1>
                    </div>
                    <div class="content">
                        ${plainText.replaceAll('\n', '<br>')}
                    </div>
                </div>
            </body>
            </html>
        """
    }

    def processScheduledNewsletters() {
        try {
            def now = new Date()
            def scheduledNewsletters = Newsletter.findAllByStatusAndScheduledDateLessThanEquals('SCHEDULED', now)
            
            log.info "Processing ${scheduledNewsletters.size()} scheduled newsletters"
            
            scheduledNewsletters.each { newsletter ->
                try {
                    sendNewsletter(newsletter)
                    log.info "Sent scheduled newsletter: ${newsletter.subject}"
                } catch (Exception e) {
                    log.error "Failed to send scheduled newsletter ${newsletter.id}: ${e.message}", e
                }
            }
        } catch (Exception e) {
            log.error "Failed to process scheduled newsletters: ${e.message}", e
        }
    }

    def duplicateNewsletter(Newsletter original, KpUser createdBy) {
        try {
            def copy = new Newsletter(
                subject: "${original.subject} (Copy)",
                content: original.content,
                contentHtml: original.contentHtml,
                status: 'DRAFT',
                createdBy: createdBy
            )
            
            if (copy.save(flush: true)) {
                log.info "Newsletter duplicated successfully: ${copy.subject}"
                return copy
            } else {
                log.warn "Failed to save duplicated newsletter: ${copy.errors}"
                return null
            }
        } catch (Exception e) {
            log.error "Failed to duplicate newsletter ${original.subject}: ${e.message}", e
            return null
        }
    }
}