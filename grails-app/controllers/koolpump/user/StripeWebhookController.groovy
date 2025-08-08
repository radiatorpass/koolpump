package koolpump.user

import com.stripe.Stripe
import com.stripe.model.*
import com.stripe.model.checkout.Session
import com.stripe.net.Webhook
import grails.converters.JSON
import grails.core.GrailsApplication
import koolpump.user.Payment
import koolpump.user.Subscription
import koolpump.user.KpUser

class StripeWebhookController {

    PaymentService paymentService
    SubscriptionService subscriptionService
    UserService userService
    EmailService emailService
    GrailsApplication grailsApplication

    def handleWebhook() {
        def payload = request.inputStream.text
        def sigHeader = request.getHeader("Stripe-Signature")
        def endpointSecret = System.getenv('STRIPE_WEBHOOK_SECRET') ?: grailsApplication.config.stripe.webhookSecret
        
        Event event = null
        
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret)
        } catch (Exception e) {
            log.error "Webhook signature verification failed: ${e.message}"
            response.status = 400
            render([error: "Invalid signature"] as JSON)
            return
        }
        
        log.info "Processing webhook event: ${event.type}"
        
        try {
            switch (event.type) {
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event)
                    break
                    
                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(event)
                    break
                    
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event)
                    break
                    
                case "customer.subscription.created":
                    handleSubscriptionCreated(event)
                    break
                    
                case "customer.subscription.updated":
                    handleSubscriptionUpdated(event)
                    break
                    
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(event)
                    break
                    
                case "invoice.payment_succeeded":
                    handleInvoicePaymentSucceeded(event)
                    break
                    
                case "invoice.payment_failed":
                    handleInvoicePaymentFailed(event)
                    break
                    
                default:
                    log.info "Unhandled event type: ${event.type}"
            }
            
            render([received: true] as JSON)
        } catch (Exception e) {
            log.error "Error processing webhook: ${e.message}", e
            response.status = 500
            render([error: "Webhook processing failed"] as JSON)
        }
    }

    private void handlePaymentIntentSucceeded(Event event) {
        def paymentIntent = event.dataObjectDeserializer.getObject().get() as PaymentIntent
        
        def payment = Payment.findByStripePaymentIntentId(paymentIntent.id)
        if (!payment) {
            // Create new payment record
            def userId = paymentIntent.metadata.get("userId")
            if (userId) {
                def user = KpUser.get(Long.parseLong(userId))
                if (user) {
                    payment = new Payment(
                        user: user,
                        transactionId: UUID.randomUUID().toString(),
                        stripePaymentIntentId: paymentIntent.id,
                        amount: paymentIntent.amount / 100,
                        currency: paymentIntent.currency.toUpperCase(),
                        status: 'SUCCESS',
                        description: paymentIntent.description ?: "Payment",
                        paymentDate: new Date()
                    )
                    
                    if (paymentIntent.charges && paymentIntent.charges.data.size() > 0) {
                        def charge = paymentIntent.charges.data[0]
                        payment.stripeChargeId = charge.id
                        payment.receiptUrl = charge.receiptUrl
                        payment.invoiceUrl = charge.invoice?.hostedInvoiceUrl
                    }
                    
                    payment.save(flush: true)
                    
                    userService.logActivity(user, 'PAYMENT_WEBHOOK_SUCCESS', 'STRIPE')
                    emailService.sendPaymentSuccessNotification(user, payment)
                }
            }
        } else {
            // Update existing payment
            payment.status = 'SUCCESS'
            payment.paymentDate = new Date()
            payment.save(flush: true)
        }
    }

    private void handlePaymentIntentFailed(Event event) {
        def paymentIntent = event.dataObjectDeserializer.getObject().get() as PaymentIntent
        
        def payment = Payment.findByStripePaymentIntentId(paymentIntent.id)
        if (payment) {
            payment.status = 'FAILED'
            payment.failureReason = paymentIntent.lastPaymentError?.message
            payment.save(flush: true)
            
            userService.logActivity(payment.user, 'PAYMENT_WEBHOOK_FAILED', 'STRIPE')
            emailService.sendPaymentFailedNotification(payment.user, payment)
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {
        def session = event.dataObjectDeserializer.getObject().get() as Session
        
        def userId = session.metadata.get("userId")
        def planType = session.metadata.get("planType")
        
        if (userId && planType) {
            def user = KpUser.get(Long.parseLong(userId))
            if (user) {
                // Create or update subscription
                subscriptionService.upgradeToPaidPlan(
                    user,
                    planType,
                    session.customer,
                    session.subscription
                )
                
                // Create payment record
                def payment = new Payment(
                    user: user,
                    transactionId: UUID.randomUUID().toString(),
                    amount: session.amountTotal / 100,
                    currency: session.currency.toUpperCase(),
                    status: 'SUCCESS',
                    description: "Subscription - ${planType}",
                    paymentDate: new Date()
                )
                payment.save(flush: true)
                
                userService.logActivity(user, 'CHECKOUT_COMPLETED', 'STRIPE')
            }
        }
    }

    private void handleSubscriptionCreated(Event event) {
        def stripeSubscription = event.dataObjectDeserializer.getObject().get() as com.stripe.model.Subscription
        
        def customerId = stripeSubscription.customer
        def subscription = Subscription.findByStripeCustomerId(customerId)
        
        if (subscription) {
            subscription.stripeSubscriptionId = stripeSubscription.id
            subscription.status = mapStripeStatus(stripeSubscription.status)
            subscription.nextBillingDate = new Date(stripeSubscription.currentPeriodEnd * 1000)
            subscription.save(flush: true)
            
            userService.logActivity(subscription.user, 'SUBSCRIPTION_CREATED', 'STRIPE')
        }
    }

    private void handleSubscriptionUpdated(Event event) {
        def stripeSubscription = event.dataObjectDeserializer.getObject().get() as com.stripe.model.Subscription
        
        def subscription = Subscription.findByStripeSubscriptionId(stripeSubscription.id)
        
        if (subscription) {
            subscription.status = mapStripeStatus(stripeSubscription.status)
            subscription.nextBillingDate = new Date(stripeSubscription.currentPeriodEnd * 1000)
            
            if (stripeSubscription.canceledAt) {
                subscription.endDate = new Date(stripeSubscription.canceledAt * 1000)
            }
            
            subscription.save(flush: true)
            
            userService.logActivity(subscription.user, 'SUBSCRIPTION_UPDATED', 'STRIPE')
        }
    }

    private void handleSubscriptionDeleted(Event event) {
        def stripeSubscription = event.dataObjectDeserializer.getObject().get() as com.stripe.model.Subscription
        
        def subscription = Subscription.findByStripeSubscriptionId(stripeSubscription.id)
        
        if (subscription) {
            subscription.status = 'CANCELLED'
            subscription.endDate = new Date()
            subscription.save(flush: true)
            
            userService.logActivity(subscription.user, 'SUBSCRIPTION_CANCELLED', 'STRIPE')
            
            // Send cancellation email
            def mailService = grails.util.Holders.applicationContext.getBean("mailService")
            mailService.sendMail {
                to subscription.user.email
                from 'noreply@koolpump.com'
                subject 'KoolPump - Subscription Cancelled'
                html """
                    <p>Hello ${subscription.user.firstName},</p>
                    <p>Your subscription has been cancelled.</p>
                    <p>You will continue to have access until the end of your billing period.</p>
                    <p>We're sorry to see you go. If you have any feedback, please let us know.</p>
                    <p>Best regards,<br>KoolPump Team</p>
                """
            }
        }
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        def invoice = event.dataObjectDeserializer.getObject().get() as Invoice
        
        def subscription = Subscription.findByStripeSubscriptionId(invoice.subscription)
        
        if (subscription) {
            // Create payment record
            def payment = new Payment(
                user: subscription.user,
                subscription: subscription,
                transactionId: invoice.id,
                stripeChargeId: invoice.charge,
                amount: invoice.amountPaid / 100,
                currency: invoice.currency.toUpperCase(),
                status: 'SUCCESS',
                description: "Subscription renewal - ${subscription.planName}",
                invoiceUrl: invoice.hostedInvoiceUrl,
                paymentDate: new Date()
            )
            payment.save(flush: true)
            
            // Update subscription
            subscription.nextBillingDate = new Date(invoice.periodEnd * 1000)
            subscription.save(flush: true)
            
            userService.logActivity(subscription.user, 'SUBSCRIPTION_RENEWED', 'STRIPE')
            emailService.sendPaymentSuccessNotification(subscription.user, payment)
        }
    }

    private void handleInvoicePaymentFailed(Event event) {
        def invoice = event.dataObjectDeserializer.getObject().get() as Invoice
        
        def subscription = Subscription.findByStripeSubscriptionId(invoice.subscription)
        
        if (subscription) {
            // Create failed payment record
            def payment = new Payment(
                user: subscription.user,
                subscription: subscription,
                transactionId: invoice.id,
                amount: invoice.amountDue / 100,
                currency: invoice.currency.toUpperCase(),
                status: 'FAILED',
                description: "Failed subscription renewal - ${subscription.planName}",
                failureReason: "Payment failed",
                paymentDate: new Date()
            )
            payment.save(flush: true)
            
            userService.logActivity(subscription.user, 'SUBSCRIPTION_PAYMENT_FAILED', 'STRIPE')
            emailService.sendPaymentFailedNotification(subscription.user, payment)
            
            // Send warning email
            def mailService = grails.util.Holders.applicationContext.getBean("mailService")
            mailService.sendMail {
                to subscription.user.email
                from 'noreply@koolpump.com'
                subject 'KoolPump - Payment Failed - Action Required'
                html """
                    <p>Hello ${subscription.user.firstName},</p>
                    <p>We were unable to process your subscription payment.</p>
                    <p>Please update your payment method to avoid service interruption.</p>
                    <p><a href="http://www.koolpump.com/payment/update-method">Update Payment Method</a></p>
                    <p>Best regards,<br>KoolPump Team</p>
                """
            }
        }
    }

    private String mapStripeStatus(String stripeStatus) {
        switch (stripeStatus) {
            case "active":
                return "ACTIVE"
            case "past_due":
                return "SUSPENDED"
            case "canceled":
                return "CANCELLED"
            case "unpaid":
                return "SUSPENDED"
            case "trialing":
                return "TRIAL"
            default:
                return "EXPIRED"
        }
    }
}