package koolpump.user

import koolpump.user.KpUser

class AuthInterceptor {

    AuthInterceptor() {
        match(controller: 'dashboard')
        match(controller: 'admin')
        match(controller: 'payment', action: ~/(checkout|manageBilling|updatePaymentMethod)/)
    }

    boolean before() {
        if (!session.user) {
            flash.error = "Please log in to access this page"
            redirect(controller: 'auth', action: 'login', params: [targetUri: request.forwardURI])
            return false
        }
        
        // Admin check
        if (controllerName == 'admin') {
            def user = KpUser.get(session.userId)
            if (!user || !user.isAdmin) {
                flash.error = "Access denied. Admin privileges required."
                redirect(controller: 'dashboard', action: 'index')
                return false
            }
        }
        
        return true
    }

    boolean after() { 
        true 
    }

    void afterView() {
    }
}