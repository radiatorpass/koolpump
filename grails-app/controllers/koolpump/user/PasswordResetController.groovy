package koolpump.user

import koolpump.user.KpUser

class PasswordResetController {

    UserService userService
    EmailService emailService

    def forgotPassword() {
    }

    def sendResetLink() {
        def email = params.email
        
        if (!email) {
            flash.error = "Please enter your email address"
            redirect(action: 'forgotPassword')
            return
        }

        def user = KpUser.findByEmail(email)
        
        if (user) {
            userService.revokeExistingTokens(user, 'RESET_PASSWORD')
            
            def token = userService.generateResetToken(user)
            emailService.sendPasswordResetEmail(user, token)
            userService.logActivity(user, 'PASSWORD_RESET_REQUESTED', request.getRemoteAddr())
        }
        
        flash.message = "If an account exists with this email, a password reset link has been sent."
        redirect(controller: 'auth', action: 'login')
    }

    def resetPassword() {
        def token = params.token
        
        if (!token) {
            flash.error = "Invalid reset link"
            redirect(uri: '/')
            return
        }

        def userToken = UserToken.findByTokenAndTokenType(token, 'RESET_PASSWORD')
        
        if (!userToken || !userToken.isValid()) {
            flash.error = "Invalid or expired reset link"
            redirect(uri: '/')
            return
        }

        [token: token, user: userToken.user]
    }

    def updatePassword() {
        def token = params.token
        def newPassword = params.newPassword
        def confirmPassword = params.confirmPassword
        
        if (!token || !newPassword || !confirmPassword) {
            flash.error = "All fields are required"
            redirect(action: 'resetPassword', params: [token: token])
            return
        }

        if (newPassword != confirmPassword) {
            flash.error = "Passwords do not match"
            redirect(action: 'resetPassword', params: [token: token])
            return
        }

        if (newPassword.length() < 6) {
            flash.error = "Password must be at least 6 characters long"
            redirect(action: 'resetPassword', params: [token: token])
            return
        }

        def userToken = UserToken.findByTokenAndTokenType(token, 'RESET_PASSWORD')
        
        if (!userToken || !userToken.isValid()) {
            flash.error = "Invalid or expired reset link"
            redirect(uri: '/')
            return
        }

        def user = userToken.user
        user.password = userService.hashPassword(newPassword)
        user.passwordExpired = false
        
        if (user.save(flush: true)) {
            userToken.used = true
            userToken.save(flush: true)
            
            userService.logActivity(user, 'PASSWORD_RESET_COMPLETED', request.getRemoteAddr())
            
            flash.message = "Password reset successfully! You can now log in with your new password."
            redirect(controller: 'auth', action: 'login')
        } else {
            flash.error = "An error occurred while resetting your password"
            redirect(action: 'resetPassword', params: [token: token])
        }
    }
}