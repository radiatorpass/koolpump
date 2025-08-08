package koolpump.user

import koolpump.user.KpUser

class DashboardController {

    UserService userService

    def index() {
        def user = session.user
        if (!user) {
            redirect(controller: 'auth', action: 'login')
            return
        }

        def recentActivity = userService.getRecentActivity(user, 10)
        def activeSessions = userService.getActiveSessions(user)
        
        [user: user, recentActivity: recentActivity, activeSessions: activeSessions]
    }

    def profile() {
        def user = session.user
        if (!user) {
            redirect(controller: 'auth', action: 'login')
            return
        }

        [user: KpUser.get(user.id)]
    }

    def updateProfile() {
        def user = session.user
        if (!user) {
            redirect(controller: 'auth', action: 'login')
            return
        }

        def currentUser = KpUser.get(user.id)
        currentKpUser.firstName = params.firstName
        currentKpUser.lastName = params.lastName
        
        if (currentKpUser.save(flush: true)) {
            session.user = currentUser
            userService.logActivity(currentUser, 'PROFILE_UPDATED', request.getRemoteAddr())
            flash.message = "Profile updated successfully"
        } else {
            flash.error = "Error updating profile"
        }
        
        redirect(action: 'profile')
    }

    def changePassword() {
        def user = session.user
        if (!user) {
            redirect(controller: 'auth', action: 'login')
            return
        }
    }

    def updatePassword() {
        def user = session.user
        if (!user) {
            redirect(controller: 'auth', action: 'login')
            return
        }

        def currentPassword = params.currentPassword
        def newPassword = params.newPassword
        def confirmPassword = params.confirmPassword
        
        def currentUser = KpUser.get(user.id)
        
        if (!userService.validatePassword(currentPassword, currentKpUser.password)) {
            flash.error = "Current password is incorrect"
            redirect(action: 'changePassword')
            return
        }

        if (newPassword != confirmPassword) {
            flash.error = "New passwords do not match"
            redirect(action: 'changePassword')
            return
        }

        if (newPassword.length() < 6) {
            flash.error = "Password must be at least 6 characters long"
            redirect(action: 'changePassword')
            return
        }

        currentKpUser.password = userService.hashPassword(newPassword)
        
        if (currentKpUser.save(flush: true)) {
            userService.logActivity(currentUser, 'PASSWORD_CHANGED', request.getRemoteAddr())
            flash.message = "Password changed successfully"
            redirect(action: 'index')
        } else {
            flash.error = "Error changing password"
            redirect(action: 'changePassword')
        }
    }

    def revokeTokens() {
        def user = session.user
        if (!user) {
            redirect(controller: 'auth', action: 'login')
            return
        }

        def currentUser = KpUser.get(user.id)
        userService.revokeAllTokens(currentUser)
        userService.logActivity(currentUser, 'TOKENS_REVOKED', request.getRemoteAddr())
        
        flash.message = "All tokens have been revoked"
        redirect(action: 'index')
    }
}