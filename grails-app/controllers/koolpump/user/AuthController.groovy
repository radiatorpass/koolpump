package koolpump.user

import grails.converters.JSON
import java.security.MessageDigest

class AuthController {

    UserService userService

    def login() {
        if (session.user) {
            redirect(controller: 'dashboard', action: 'index')
            return
        }
    }

    def authenticate() {
        def email = params.email
        def password = params.password
        
        if (!email || !password) {
            flash.error = "Email and password are required"
            redirect(action: 'login')
            return
        }

        def result = userService.authenticate(email, password)
        
        if (result.success && result.user) {
            session.user = result.user
            session.userId = result.user.id
            
            userService.logActivity(result.user, 'LOGIN', request.getRemoteAddr())
            
            def redirectUrl = params.targetUri ?: '/dashboard'
            redirect(uri: redirectUrl)
        } else {
            flash.error = result.error ?: "Invalid email or password"
            if (result.blocked) {
                flash.error = "Account temporarily locked due to too many failed attempts. Please try again later."
            }
            redirect(action: 'login')
        }
    }

    def logout() {
        if (session.user) {
            userService.logActivity(session.user, 'LOGOUT', request.getRemoteAddr())
        }
        
        session.invalidate()
        flash.message = "You have been logged out successfully"
        redirect(uri: '/')
    }
}