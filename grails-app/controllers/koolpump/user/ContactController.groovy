package koolpump.user

import org.owasp.encoder.Encode
import koolpump.user.KpUser

class ContactController {

    EmailService emailService
    UserService userService

    def index() {
        def contactMessage = new ContactMessage()
        
        if (session.user) {
            def user = KpUser.get(session.userId)
            contactMessage.name = "${user.firstName} ${user.lastName}"
            contactMessage.email = user.email
            contactMessage.user = user
        }
        
        [contactMessage: contactMessage]
    }

    def send() {
        // Sanitize input to prevent XSS
        def sanitizedParams = [:]
        sanitizedParams.name = Encode.forHtml(params.name ?: '')
        sanitizedParams.email = params.email // Email validation handled by domain
        sanitizedParams.subject = Encode.forHtml(params.subject ?: '')
        sanitizedParams.message = Encode.forHtml(params.message ?: '')
        sanitizedParams.category = params.category // Validated by domain inList constraint
        
        def contactMessage = new ContactMessage(sanitizedParams)
        contactMessage.ipAddress = request.getRemoteAddr()
        
        if (session.user) {
            contactMessage.user = KpUser.get(session.userId)
        }
        
        if (!contactMessage.validate()) {
            render(view: 'index', model: [contactMessage: contactMessage])
            return
        }
        
        if (contactMessage.save(flush: true)) {
            // Send notification to admin
            sendAdminNotification(contactMessage)
            
            // Send confirmation to user
            sendUserConfirmation(contactMessage)
            
            if (contactMessage.user) {
                userService.logActivity(contactMessage.user, 'CONTACT_MESSAGE_SENT', request.getRemoteAddr())
            }
            
            flash.message = "Thank you for contacting us! We'll get back to you within 24-48 hours."
            redirect(action: 'success')
        } else {
            render(view: 'index', model: [contactMessage: contactMessage])
        }
    }

    def success() {
        // Success page after sending message
    }

    private void sendAdminNotification(ContactMessage message) {
        try {
            def mailService = grails.util.Holders.applicationContext.getBean("mailService")
            
            mailService.sendMail {
                to 'admin@koolpump.com'
                from 'noreply@koolpump.com'
                subject "[KoolPump Contact] ${message.category}: ${message.subject}"
                html """
                    <h3>New Contact Message</h3>
                    <p><strong>From:</strong> ${message.name} (${message.email})</p>
                    <p><strong>Category:</strong> ${message.category}</p>
                    <p><strong>Subject:</strong> ${message.subject}</p>
                    <p><strong>Message:</strong></p>
                    <p>${message.message.encodeAsHTML().replaceAll('\n', '<br>')}</p>
                    <p><strong>User:</strong> ${message.user ? 'Registered User - ' + message.user.email : 'Anonymous'}</p>
                    <p><strong>IP Address:</strong> ${message.ipAddress}</p>
                    <p><strong>Date:</strong> ${message.dateCreated}</p>
                    <hr>
                    <p><a href="http://www.koolpump.com/admin/contact/${message.id}">View in Admin Panel</a></p>
                """
            }
        } catch (Exception e) {
            log.error "Failed to send admin notification for contact message: ${e.message}"
        }
    }

    private void sendUserConfirmation(ContactMessage message) {
        try {
            def mailService = grails.util.Holders.applicationContext.getBean("mailService")
            
            mailService.sendMail {
                to message.email
                from 'noreply@koolpump.com'
                subject "KoolPump - We've received your message"
                html """
                    <h3>Thank you for contacting KoolPump!</h3>
                    <p>Dear ${message.name},</p>
                    <p>We have received your message and will get back to you within 24-48 hours.</p>
                    <p><strong>Your message details:</strong></p>
                    <p><strong>Subject:</strong> ${message.subject}</p>
                    <p><strong>Category:</strong> ${message.category}</p>
                    <p><strong>Message:</strong></p>
                    <p>${message.message.encodeAsHTML().replaceAll('\n', '<br>')}</p>
                    <p><strong>Reference Number:</strong> #${String.format('%06d', message.id)}</p>
                    <p>Please quote this reference number in any future correspondence about this inquiry.</p>
                    <p>Best regards,<br>The KoolPump Team</p>
                """
            }
        } catch (Exception e) {
            log.error "Failed to send user confirmation for contact message: ${e.message}"
        }
    }
}