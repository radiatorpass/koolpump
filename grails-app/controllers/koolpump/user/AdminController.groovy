package koolpump.user

import koolpump.user.*

class AdminController {

    UserService userService

    def index() {
        def totalUsers = KpUser.count()
        def activeUsers = KpUser.countByEnabled(true)
        def totalPayments = Payment.count()
        def totalRevenue = Payment.findAllByStatus('SUCCESS').sum { it.amount } ?: 0
        def activeSubscriptions = Subscription.countByStatusInList(['ACTIVE', 'TRIAL'])
        
        [
            totalUsers: totalUsers,
            activeUsers: activeUsers,
            totalPayments: totalPayments,
            totalRevenue: totalRevenue,
            activeSubscriptions: activeSubscriptions,
            recentUsers: KpUser.list(max: 5, sort: 'dateCreated', order: 'desc'),
            recentPayments: Payment.list(max: 5, sort: 'dateCreated', order: 'desc')
        ]
    }

    def users() {
        // Sanitize and validate parameters to prevent SQL injection
        def max = Math.min(params.int('max') ?: 20, 100) // Limit max to 100
        def offset = params.int('offset') ?: 0
        
        // Whitelist allowed sort fields
        def allowedSortFields = ['dateCreated', 'email', 'firstName', 'lastName', 'enabled', 'lastUpdated']
        def sort = params.sort in allowedSortFields ? params.sort : 'dateCreated'
        
        // Validate order direction
        def order = params.order?.toLowerCase() in ['asc', 'desc'] ? params.order.toLowerCase() : 'desc'
        
        // Use criteria with eager fetching to avoid N+1 queries
        def users = KpUser.createCriteria().list(max: max, offset: offset) {
            // Eager fetch associations to prevent N+1
            createAlias('subscription', 'sub', CriteriaSpecification.LEFT_JOIN)
            createAlias('payments', 'pay', CriteriaSpecification.LEFT_JOIN)
            
            order(sort, order)
        }
        
        def userCount = KpUser.count()
        
        [users: users, userCount: userCount]
    }

    def editUser() {
        def user = KpUser.get(params.id)
        if (!user) {
            flash.error = "User not found"
            redirect(action: 'users')
            return
        }
        
        def subscription = Subscription.findByUser(user)
        def payments = Payment.findAllByUser(user, [max: 10, sort: 'dateCreated', order: 'desc'])
        def activities = UserActivityLog.findAllByUser(user, [max: 20, sort: 'timestamp', order: 'desc'])
        
        [user: user, subscription: subscription, payments: payments, activities: activities]
    }

    def updateUser() {
        def user = KpUser.get(params.id)
        if (!user) {
            flash.error = "User not found"
            redirect(action: 'users')
            return
        }
        
        user.firstName = params.firstName
        user.lastName = params.lastName
        user.enabled = params.enabled ? true : false
        user.accountLocked = params.accountLocked ? true : false
        user.accountExpired = params.accountExpired ? true : false
        user.passwordExpired = params.passwordExpired ? true : false
        user.isAdmin = params.isAdmin ? true : false
        
        if (user.save(flush: true)) {
            userService.logActivity(user, 'ADMIN_USER_UPDATE', request.getRemoteAddr())
            flash.message = "User updated successfully"
        } else {
            flash.error = "Error updating user"
        }
        
        redirect(action: 'editUser', id: user.id)
    }

    def resetUserPassword() {
        def user = KpUser.get(params.id)
        if (!user) {
            flash.error = "User not found"
            redirect(action: 'users')
            return
        }
        
        def tempPassword = UUID.randomUUID().toString().substring(0, 8)
        user.password = userService.hashPassword(tempPassword)
        user.passwordExpired = true
        
        if (user.save(flush: true)) {
            userService.logActivity(user, 'ADMIN_PASSWORD_RESET', request.getRemoteAddr())
            flash.message = "Password reset to: ${tempPassword} (user must change on next login)"
        } else {
            flash.error = "Error resetting password"
        }
        
        redirect(action: 'editUser', id: user.id)
    }

    def deleteUser() {
        def user = KpUser.get(params.id)
        if (!user) {
            flash.error = "User not found"
            redirect(action: 'users')
            return
        }
        
        if (user.isAdmin) {
            flash.error = "Cannot delete admin users"
            redirect(action: 'editUser', id: user.id)
            return
        }
        
        try {
            UserActivityLog.deleteAll(UserActivityLog.findAllByUser(user))
            UserToken.deleteAll(UserToken.findAllByUser(user))
            Payment.deleteAll(Payment.findAllByUser(user))
            Subscription.deleteAll(Subscription.findAllByUser(user))
            user.delete(flush: true)
            flash.message = "User deleted successfully"
            redirect(action: 'users')
        } catch (Exception e) {
            flash.error = "Error deleting user: ${e.message}"
            redirect(action: 'editUser', id: user.id)
        }
    }

    def payments() {
        def max = params.max ?: 20
        def offset = params.offset ?: 0
        def sort = params.sort ?: 'dateCreated'
        def order = params.order ?: 'desc'
        
        def payments = Payment.list(max: max, offset: offset, sort: sort, order: order)
        def paymentCount = Payment.count()
        
        def stats = [
            totalRevenue: Payment.findAllByStatus('SUCCESS').sum { it.amount } ?: 0,
            pendingPayments: Payment.countByStatus('PENDING'),
            failedPayments: Payment.countByStatus('FAILED'),
            refundedAmount: Payment.findAllByStatus('REFUNDED').sum { it.amount } ?: 0
        ]
        
        [payments: payments, paymentCount: paymentCount, stats: stats]
    }

    def subscriptions() {
        def max = params.max ?: 20
        def offset = params.offset ?: 0
        def sort = params.sort ?: 'dateCreated'
        def order = params.order ?: 'desc'
        
        def subscriptions = Subscription.list(max: max, offset: offset, sort: sort, order: order)
        def subscriptionCount = Subscription.count()
        
        def stats = [
            activeSubscriptions: Subscription.countByStatus('ACTIVE'),
            trialSubscriptions: Subscription.countByStatus('TRIAL'),
            cancelledSubscriptions: Subscription.countByStatus('CANCELLED'),
            expiredSubscriptions: Subscription.countByStatus('EXPIRED'),
            basicPlan: Subscription.countByPlanType('BASIC'),
            professionalPlan: Subscription.countByPlanType('PROFESSIONAL'),
            enterprisePlan: Subscription.countByPlanType('ENTERPRISE')
        ]
        
        [subscriptions: subscriptions, subscriptionCount: subscriptionCount, stats: stats]
    }

    def updateSubscription() {
        def subscription = Subscription.get(params.id)
        if (!subscription) {
            flash.error = "Subscription not found"
            redirect(action: 'subscriptions')
            return
        }
        
        subscription.status = params.status
        subscription.autoRenew = params.autoRenew ? true : false
        
        if (params.extendTrial && subscription.status == 'TRIAL') {
            subscription.trialEndDate = new Date() + params.int('trialDays')
        }
        
        if (subscription.save(flush: true)) {
            userService.logActivity(subscription.user, 'ADMIN_SUBSCRIPTION_UPDATE', request.getRemoteAddr())
            flash.message = "Subscription updated successfully"
        } else {
            flash.error = "Error updating subscription"
        }
        
        redirect(action: 'editUser', id: subscription.user.id)
    }

    def refundPayment() {
        def payment = Payment.get(params.id)
        if (!payment) {
            flash.error = "Payment not found"
            redirect(action: 'payments')
            return
        }
        
        if (payment.status != 'SUCCESS') {
            flash.error = "Can only refund successful payments"
            redirect(action: 'payments')
            return
        }
        
        payment.status = 'REFUNDED'
        
        if (payment.save(flush: true)) {
            userService.logActivity(payment.user, 'ADMIN_PAYMENT_REFUND', request.getRemoteAddr())
            flash.message = "Payment marked as refunded (manual Stripe refund required)"
        } else {
            flash.error = "Error refunding payment"
        }
        
        redirect(action: 'payments')
    }

    def systemSettings() {
        def settings = [
            totalUsers: KpUser.count(),
            adminUsers: KpUser.countByIsAdmin(true),
            emailVerificationRequired: true,
            trialPeriodDays: 14,
            passwordMinLength: 6,
            sessionTimeout: 30
        ]
        
        [settings: settings]
    }

    def activityLog() {
        def max = params.max ?: 50
        def offset = params.offset ?: 0
        
        def activities = UserActivityLog.list(max: max, offset: offset, sort: 'timestamp', order: 'desc')
        def activityCount = UserActivityLog.count()
        
        [activities: activities, activityCount: activityCount]
    }
}