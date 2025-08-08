package koolpump.user

import koolpump.user.KpUser

class RegistrationController {

    UserService userService
    EmailService emailService

    def register() {
        [user: new KpUser()]
    }

    def save() {
        def user = new KpUser(params)
        
        if (!user.validate()) {
            render(view: 'register', model: [user: user])
            return
        }

        user.password = userService.hashPassword(params.password)
        
        if (user.save(flush: true)) {
            def token = userService.generateVerificationToken(user)
            
            emailService.sendRegistrationEmail(user, token)
            userService.logActivity(user, 'REGISTRATION', request.getRemoteAddr())
            
            flash.message = "Registration successful! Please check your email to verify your account."
            redirect(controller: 'auth', action: 'login')
        } else {
            render(view: 'register', model: [user: user])
        }
    }

    def verify() {
        def token = params.token
        
        if (!token) {
            flash.error = "Invalid verification link"
            redirect(uri: '/')
            return
        }

        def userToken = UserToken.findByTokenAndTokenType(token, 'EMAIL_VERIFICATION')
        
        if (!userToken || !userToken.isValid()) {
            flash.error = "Invalid or expired verification link"
            redirect(uri: '/')
            return
        }

        userToken.used = true
        userToken.save(flush: true)
        
        def user = userToken.user
        user.enabled = true
        user.save(flush: true)
        
        userService.logActivity(user, 'EMAIL_VERIFIED', request.getRemoteAddr())
        
        flash.message = "Email verified successfully! You can now log in."
        redirect(controller: 'auth', action: 'login')
    }
}