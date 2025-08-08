package koolpump

import koolpump.user.KpUser
import koolpump.user.Subscription
import koolpump.user.UserActivityLog
import koolpump.user.UserService
import java.security.MessageDigest

class BootStrap {

    UserService userService

    def init = { servletContext ->
        createAdminUser()
    }
    
    def destroy = {
    }
    
    private void createAdminUser() {
        def adminEmail = 'admin@koolpump.com'
        
        if (!KpUser.findByEmail(adminEmail)) {
            def adminKpUser = new KpUser(
                email: adminEmail,
                password: userService.hashPassword('admin123'),
                firstName: 'Admin',
                lastName: 'User',
                enabled: true,
                isAdmin: true
            )
            
            if (adminKpUser.save(flush: true)) {
                // Create trial subscription for admin
                def subscription = new Subscription(
                    user: adminUser,
                    planName: 'Enterprise',
                    planType: 'ENTERPRISE',
                    status: 'ACTIVE',
                    startDate: new Date(),
                    monthlyPrice: 0.0
                )
                subscription.save(flush: true)
                
                log.info "Admin user created: ${adminEmail}"
            } else {
                log.error "Failed to create admin user: ${adminKpUser.errors}"
            }
        }
    }
}