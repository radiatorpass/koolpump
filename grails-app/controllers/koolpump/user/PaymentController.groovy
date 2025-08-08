package koolpump.user

import grails.converters.JSON
import koolpump.user.Subscription
import koolpump.user.Payment
import koolpump.user.KpUser

class PaymentController {

    PaymentService paymentService
    SubscriptionService subscriptionService
    UserService userService

    def choosePlan() {
        if (!session.user) {
            redirect(controller: 'auth', action: 'login')
            return
        }
        
        def plans = subscriptionService.getAllActivePlans()
        def currentSubscription = Subscription.findByUser(session.user)
        
        [plans: plans, currentSubscription: currentSubscription]
    }

    def checkout() {
        if (!session.user) {
            redirect(controller: 'auth', action: 'login')
            return
        }
        
        def planType = params.planType
        if (!planType || !['BASIC', 'PROFESSIONAL', 'ENTERPRISE'].contains(planType)) {
            flash.error = "Invalid plan selected"
            redirect(action: 'choosePlan')
            return
        }
        
        def user = KpUser.get(session.userId)
        def successUrl = "${request.getScheme()}://${request.getServerName()}:${request.getServerPort()}/payment/success?session_id={CHECKOUT_SESSION_ID}"
        def cancelUrl = "${request.getScheme()}://${request.getServerName()}:${request.getServerPort()}/payment/cancel"
        
        def result = paymentService.createCheckoutSession(user, planType, successUrl, cancelUrl)
        
        if (result.success) {
            redirect(url: result.url)
        } else {
            flash.error = "Failed to create checkout session: ${result.error}"
            redirect(action: 'choosePlan')
        }
    }

    def success() {
        if (!session.user) {
            redirect(controller: 'auth', action: 'login')
            return
        }
        
        def sessionId = params.session_id
        
        if (sessionId) {
            // Process the successful payment
            flash.message = "Payment successful! Your subscription is now active."
        }
        
        redirect(controller: 'dashboard', action: 'index')
    }

    def cancel() {
        flash.error = "Payment was cancelled"
        redirect(action: 'choosePlan')
    }

    def manageBilling() {
        if (!session.user) {
            redirect(controller: 'auth', action: 'login')
            return
        }
        
        def user = KpUser.get(session.userId)
        def subscription = Subscription.findByUser(user)
        def payments = paymentService.getPaymentHistory(user, 20)
        def stats = paymentService.getPaymentStatistics(user)
        
        [subscription: subscription, payments: payments, stats: stats]
    }

    def updatePaymentMethod() {
        if (!session.user) {
            redirect(controller: 'auth', action: 'login')
            return
        }
        
        def user = KpUser.get(session.userId)
        def subscription = Subscription.findByUser(user)
        
        [subscription: subscription, stripePublicKey: System.getenv('STRIPE_PUBLIC_KEY')]
    }

    def savePaymentMethod() {
        if (!session.user) {
            render([success: false, error: "Not authenticated"] as JSON)
            return
        }
        
        def user = KpUser.get(session.userId)
        def paymentMethodId = params.paymentMethodId
        
        if (!paymentMethodId) {
            render([success: false, error: "Payment method ID required"] as JSON)
            return
        }
        
        def result = paymentService.updatePaymentMethod(user, paymentMethodId)
        
        render(result as JSON)
    }

    def cancelSubscription() {
        if (!session.user) {
            redirect(controller: 'auth', action: 'login')
            return
        }
        
        def user = KpUser.get(session.userId)
        def immediate = params.boolean('immediate', false)
        
        if (subscriptionService.cancelSubscription(user, immediate)) {
            flash.message = "Your subscription has been cancelled"
        } else {
            flash.error = "Failed to cancel subscription"
        }
        
        redirect(action: 'manageBilling')
    }

    def pauseSubscription() {
        if (!session.user) {
            redirect(controller: 'auth', action: 'login')
            return
        }
        
        def user = KpUser.get(session.userId)
        def days = params.int('days', 30)
        
        if (subscriptionService.pauseSubscription(user, days)) {
            flash.message = "Your subscription has been paused for ${days} days"
        } else {
            flash.error = "Failed to pause subscription"
        }
        
        redirect(action: 'manageBilling')
    }

    def resumeSubscription() {
        if (!session.user) {
            redirect(controller: 'auth', action: 'login')
            return
        }
        
        def user = KpUser.get(session.userId)
        
        if (subscriptionService.resumeSubscription(user)) {
            flash.message = "Your subscription has been resumed"
        } else {
            flash.error = "Failed to resume subscription"
        }
        
        redirect(action: 'manageBilling')
    }

    def downloadInvoice() {
        if (!session.user) {
            redirect(controller: 'auth', action: 'login')
            return
        }
        
        def paymentId = params.id
        def payment = Payment.get(paymentId)
        
        if (!payment || payment.user.id != session.userId) {
            flash.error = "Invoice not found"
            redirect(action: 'manageBilling')
            return
        }
        
        if (payment.invoiceUrl) {
            redirect(url: payment.invoiceUrl)
        } else {
            flash.error = "Invoice not available"
            redirect(action: 'manageBilling')
        }
    }
}