package koolpump.user

import grails.gorm.transactions.Transactional
import koolpump.user.Subscription
import koolpump.user.Payment
import koolpump.user.KpUser

@Transactional
class SubscriptionService {

    EmailService emailService
    UserService userService
    
    // Configuration constants
    private static final int DEFAULT_TRIAL_DAYS = 14
    private static final int BILLING_CYCLE_DAYS = 30
    private static final int TRIAL_EXPIRY_WARNING_DAYS = 3
    private static final int TRIAL_FINAL_WARNING_DAYS = 1
    
    // Plan pricing constants
    private static final BigDecimal BASIC_PLAN_PRICE = 9.99
    private static final BigDecimal PROFESSIONAL_PLAN_PRICE = 29.99
    private static final BigDecimal ENTERPRISE_PLAN_PRICE = 99.99

    def createTrialSubscription(KpUser user) {
        def trialDays = grailsApplication?.config?.subscription?.trial?.days ?: DEFAULT_TRIAL_DAYS
        def startDate = new Date()
        def trialEndDate = startDate + trialDays
        
        def subscription = new Subscription(
            user: user,
            planName: 'Free Trial',
            planType: 'TRIAL',
            status: 'TRIAL',
            startDate: startDate,
            trialEndDate: trialEndDate,
            monthlyPrice: 0.0,
            autoRenew: false
        )
        
        if (subscription.save(flush: true)) {
            userService.logActivity(user, 'TRIAL_STARTED', 'SYSTEM')
            return subscription
        }
        
        return null
    }

    def upgradeToPaidPlan(KpUser user, String planType, String stripeCustomerId, String stripeSubscriptionId) {
        def subscription = Subscription.findByUser(user)
        
        if (!subscription) {
            subscription = new Subscription(user: user)
        }
        
        def planDetails = getPlanDetails(planType)
        
        subscription.planName = planDetails.name
        subscription.planType = planType
        subscription.status = 'ACTIVE'
        subscription.monthlyPrice = planDetails.price
        subscription.startDate = new Date()
        subscription.nextBillingDate = new Date() + BILLING_CYCLE_DAYS
        subscription.stripeCustomerId = stripeCustomerId
        subscription.stripeSubscriptionId = stripeSubscriptionId
        subscription.autoRenew = true
        
        if (subscription.save(flush: true)) {
            userService.logActivity(user, 'SUBSCRIPTION_UPGRADED', 'SYSTEM')
            emailService.sendSubscriptionConfirmation(user, subscription)
            return subscription
        }
        
        return null
    }

    def cancelSubscription(KpUser user, boolean immediate = false) {
        def subscription = Subscription.findByUser(user)
        
        if (!subscription || subscription.status == 'CANCELLED') {
            return false
        }
        
        if (immediate) {
            subscription.status = 'CANCELLED'
            subscription.endDate = new Date()
        } else {
            subscription.status = 'CANCELLED'
            subscription.autoRenew = false
            subscription.endDate = subscription.nextBillingDate ?: new Date() + 30
        }
        
        if (subscription.save(flush: true)) {
            userService.logActivity(user, 'SUBSCRIPTION_CANCELLED', 'SYSTEM')
            
            // Send cancellation email
            def mailService = grails.util.Holders.applicationContext.getBean("mailService")
            mailService.sendMail {
                to user.email
                from 'noreply@koolpump.com'
                subject 'KoolPump - Subscription Cancelled'
                html """
                    <p>Hello ${user.firstName},</p>
                    <p>Your subscription has been cancelled.</p>
                    <p>You will continue to have access until ${subscription.endDate.format('dd MMM yyyy')}.</p>
                    <p>We're sorry to see you go. If you have any feedback, please let us know.</p>
                    <p>Best regards,<br>KoolPump Team</p>
                """
            }
            
            return true
        }
        
        return false
    }

    def pauseSubscription(KpUser user, int days) {
        def subscription = Subscription.findByUser(user)
        
        if (!subscription || subscription.status != 'ACTIVE') {
            return false
        }
        
        subscription.status = 'SUSPENDED'
        subscription.nextBillingDate = subscription.nextBillingDate + days
        
        if (subscription.save(flush: true)) {
            userService.logActivity(user, 'SUBSCRIPTION_PAUSED', 'SYSTEM')
            return true
        }
        
        return false
    }

    def resumeSubscription(KpUser user) {
        def subscription = Subscription.findByUser(user)
        
        if (!subscription || subscription.status != 'SUSPENDED') {
            return false
        }
        
        subscription.status = 'ACTIVE'
        
        if (subscription.save(flush: true)) {
            userService.logActivity(user, 'SUBSCRIPTION_RESUMED', 'SYSTEM')
            return true
        }
        
        return false
    }

    def processRenewal(Subscription subscription) {
        if (subscription.status != 'ACTIVE' || !subscription.autoRenew) {
            return false
        }
        
        try {
            // This would integrate with PaymentService to charge via Stripe
            def payment = new Payment(
                user: subscription.user,
                subscription: subscription,
                amount: subscription.monthlyPrice,
                currency: 'EUR',
                status: 'PENDING',
                description: "Subscription renewal - ${subscription.planName}",
                transactionId: UUID.randomUUID().toString()
            )
            
            // Here you would call Stripe to process payment
            // For now, we'll simulate success
            payment.status = 'SUCCESS'
            payment.paymentDate = new Date()
            
            if (payment.save(flush: true)) {
                subscription.nextBillingDate = new Date() + 30
                subscription.save(flush: true)
                
                userService.logActivity(subscription.user, 'SUBSCRIPTION_RENEWED', 'SYSTEM')
                emailService.sendPaymentSuccessNotification(subscription.user, payment)
                
                return true
            }
        } catch (Exception e) {
            log.error "Failed to process renewal for subscription ${subscription.id}: ${e.message}"
        }
        
        return false
    }

    def getSubscriptionStatus(KpUser user) {
        def subscription = Subscription.findByUser(user)
        
        if (!subscription) {
            return [
                hasSubscription: false,
                canAccessPremium: false
            ]
        }
        
        return [
            hasSubscription: true,
            subscription: subscription,
            canAccessPremium: subscription.isActive(),
            daysRemaining: subscription.status == 'TRIAL' ? subscription.getTrialDaysRemaining() : null,
            nextBillingDate: subscription.nextBillingDate,
            autoRenew: subscription.autoRenew
        ]
    }

    def getPlanDetails(String planType) {
        def plans = [
            'BASIC': [name: 'Basic Plan', price: 9.99, features: ['Heat pump database access']],
            'PROFESSIONAL': [name: 'Professional Plan', price: 29.99, features: ['Full access', 'API access', 'Premium support']],
            'ENTERPRISE': [name: 'Enterprise Plan', price: 99.99, features: ['Everything in Professional', 'Custom integrations', 'Dedicated support']]
        ]
        
        return plans[planType] ?: [name: 'Unknown Plan', price: 0.0, features: []]
    }

    def getAllActivePlans() {
        return [
            [
                id: 'BASIC',
                name: 'Basic Plan',
                price: 9.99,
                currency: 'EUR',
                interval: 'month',
                features: [
                    'Access to heat pump database',
                    'Basic search and filters',
                    'Email support'
                ]
            ],
            [
                id: 'PROFESSIONAL',
                name: 'Professional Plan',
                price: 29.99,
                currency: 'EUR',
                interval: 'month',
                features: [
                    'Everything in Basic',
                    'API access',
                    'Advanced analytics',
                    'Priority support',
                    'Export capabilities'
                ]
            ],
            [
                id: 'ENTERPRISE',
                name: 'Enterprise Plan',
                price: 'Custom',
                currency: 'EUR',
                interval: 'custom',
                features: [
                    'Everything in Professional',
                    'Custom integrations',
                    'Dedicated account manager',
                    'SLA guarantee',
                    'Custom reporting'
                ]
            ]
        ]
    }

    def checkSubscriptionExpiry() {
        def expiredSubscriptions = Subscription.findAllByStatusAndEndDateLessThan('ACTIVE', new Date())
        
        expiredSubscriptions.each { subscription ->
            subscription.status = 'EXPIRED'
            subscription.save(flush: true)
            
            userService.logActivity(subscription.user, 'SUBSCRIPTION_EXPIRED', 'SYSTEM')
            
            // Send expiry notification
            def mailService = grails.util.Holders.applicationContext.getBean("mailService")
            mailService.sendMail {
                to subscription.user.email
                from 'noreply@koolpump.com'
                subject 'KoolPump - Subscription Expired'
                html """
                    <p>Hello ${subscription.user.firstName},</p>
                    <p>Your ${subscription.planName} subscription has expired.</p>
                    <p>To continue accessing premium features, please renew your subscription.</p>
                    <p><a href="http://www.koolpump.com/payment/choose-plan">Renew Subscription</a></p>
                    <p>Best regards,<br>KoolPump Team</p>
                """
            }
        }
    }
}