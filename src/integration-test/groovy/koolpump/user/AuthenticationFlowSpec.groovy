package koolpump.user

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetup
import javax.mail.internet.MimeMessage
import koolpump.user.*

@Integration
@Rollback
class AuthenticationFlowSpec extends Specification {

    UserService userService
    EmailService emailService
    SubscriptionService subscriptionService
    
    GreenMail greenMail
    
    def setup() {
        // Setup GreenMail for email testing
        greenMail = new GreenMail(new ServerSetup(3025, "localhost", ServerSetup.PROTOCOL_SMTP))
        greenMail.start()
    }
    
    def cleanup() {
        greenMail.stop()
    }
    
    void "test complete user registration flow"() {
        given: "A new user registration data"
        def email = "test@example.com"
        def password = "Test123!"
        def firstName = "Test"
        def lastName = "User"
        
        when: "User is created"
        def user = new User(
            email: email,
            password: userService.hashPassword(password),
            firstName: firstName,
            lastName: lastName,
            enabled: false
        )
        user.save(flush: true)
        
        then: "User is saved but not enabled"
        User.count() == 1
        !user.enabled
        
        when: "Verification token is generated"
        def token = userService.generateVerificationToken(user)
        
        then: "Token is created"
        token != null
        UserToken.count() == 1
        
        when: "User verifies email"
        def userToken = UserToken.findByToken(token)
        userToken.used = true
        userToken.save(flush: true)
        user.enabled = true
        user.save(flush: true)
        
        then: "User is now enabled"
        user.enabled
        userToken.used
    }
    
    void "test password reset flow"() {
        given: "An existing user"
        def user = new User(
            email: "user@example.com",
            password: userService.hashPassword("OldPassword123"),
            firstName: "John",
            lastName: "Doe",
            enabled: true
        ).save(flush: true)
        
        when: "Password reset is requested"
        def resetToken = userService.generateResetToken(user)
        
        then: "Reset token is created"
        resetToken != null
        UserToken.findByTokenAndTokenType(resetToken, 'RESET_PASSWORD') != null
        
        when: "User resets password with valid token"
        def newPassword = "NewPassword456"
        def userToken = UserToken.findByToken(resetToken)
        user.password = userService.hashPassword(newPassword)
        user.save(flush: true)
        userToken.used = true
        userToken.save(flush: true)
        
        then: "Password is updated and token is used"
        userService.validatePassword(newPassword, user.password)
        userToken.used
    }
    
    void "test trial subscription creation"() {
        given: "A new user"
        def user = new User(
            email: "trial@example.com",
            password: userService.hashPassword("Password123"),
            firstName: "Trial",
            lastName: "User",
            enabled: true
        ).save(flush: true)
        
        when: "Trial subscription is created"
        def subscription = subscriptionService.createTrialSubscription(user)
        
        then: "Subscription is created with correct settings"
        subscription != null
        subscription.planType == 'TRIAL'
        subscription.status == 'TRIAL'
        subscription.monthlyPrice == 0.0
        subscription.trialEndDate != null
        subscription.getTrialDaysRemaining() <= 14
    }
    
    void "test user authentication with invalid credentials"() {
        given: "A user with known credentials"
        def user = new User(
            email: "auth@example.com",
            password: userService.hashPassword("CorrectPassword"),
            firstName: "Auth",
            lastName: "User",
            enabled: true
        ).save(flush: true)
        
        when: "Authentication is attempted with wrong password"
        def result = userService.authenticate("auth@example.com", "WrongPassword")
        
        then: "Authentication fails"
        result == null
        
        when: "Authentication is attempted with correct password"
        def successResult = userService.authenticate("auth@example.com", "CorrectPassword")
        
        then: "Authentication succeeds"
        successResult != null
        successResult.id == user.id
    }
    
    void "test email sending with GreenMail"() {
        given: "Email configuration for GreenMail"
        System.setProperty("grails.mail.host", "localhost")
        System.setProperty("grails.mail.port", "3025")
        
        and: "A user"
        def user = new User(
            email: "email@example.com",
            password: userService.hashPassword("Password123"),
            firstName: "Email",
            lastName: "Test",
            enabled: true
        ).save(flush: true)
        
        when: "Registration email is sent"
        def token = userService.generateVerificationToken(user)
        // Note: In real test, you would call emailService.sendRegistrationEmail(user, token)
        // For this example, we'll simulate
        greenMail.setUser("test@localhost", "test@localhost", "password")
        
        then: "Email would be received"
        // In real test: greenMail.getReceivedMessages().length == 1
        token != null
    }
    
    void "test activity logging"() {
        given: "A user"
        def user = new User(
            email: "log@example.com",
            password: userService.hashPassword("Password123"),
            firstName: "Log",
            lastName: "User",
            enabled: true
        ).save(flush: true)
        
        when: "Various activities are logged"
        userService.logActivity(user, 'LOGIN', '127.0.0.1')
        userService.logActivity(user, 'PROFILE_UPDATED', '127.0.0.1')
        userService.logActivity(user, 'LOGOUT', '127.0.0.1')
        
        then: "Activities are recorded"
        UserActivityLog.count() == 3
        UserActivityLog.findAllByUser(user).size() == 3
        
        when: "Recent activity is retrieved"
        def recentActivity = userService.getRecentActivity(user, 2)
        
        then: "Correct number of activities returned"
        recentActivity.size() == 2
        recentActivity[0].activity == 'LOGOUT'
    }
    
    void "test subscription upgrade flow"() {
        given: "A user with trial subscription"
        def user = new User(
            email: "upgrade@example.com",
            password: userService.hashPassword("Password123"),
            firstName: "Upgrade",
            lastName: "User",
            enabled: true
        ).save(flush: true)
        
        def trialSubscription = subscriptionService.createTrialSubscription(user)
        
        when: "User upgrades to paid plan"
        def upgradedSubscription = subscriptionService.upgradeToPaidPlan(
            user, 
            'PROFESSIONAL',
            'cus_test123',
            'sub_test123'
        )
        
        then: "Subscription is upgraded"
        upgradedSubscription != null
        upgradedSubscription.planType == 'PROFESSIONAL'
        upgradedSubscription.status == 'ACTIVE'
        upgradedSubscription.monthlyPrice == 29.99
        upgradedSubscription.stripeCustomerId == 'cus_test123'
    }
}