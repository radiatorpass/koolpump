package koolpump.user

import grails.gorm.transactions.Transactional
import com.stripe.Stripe
import com.stripe.model.*
import com.stripe.param.*
import com.stripe.param.checkout.*
import com.stripe.exception.*
import koolpump.user.Payment
import koolpump.user.Subscription
import java.util.concurrent.TimeUnit
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import koolpump.user.KpUser

@Transactional
class PaymentService {

    def grailsApplication
    EmailService emailService
    UserService userService
    SubscriptionService subscriptionService
    
    // Circuit breaker for Stripe API
    private static final Cache<String, Integer> stripeFailureCache = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(100)
        .build()
    
    private static final int MAX_STRIPE_FAILURES = 3
    private static final String STRIPE_CIRCUIT_KEY = "stripe_api"

    def init() {
        def apiKey = System.getenv('STRIPE_SECRET_KEY') ?: grailsApplication.config.stripe.secretKey
        if (!apiKey || apiKey == 'sk_test_placeholder') {
            throw new IllegalStateException("Stripe API key not configured")
        }
        Stripe.apiKey = apiKey
    }
    
    private boolean isStripeAvailable() {
        Integer failures = stripeFailureCache.getIfPresent(STRIPE_CIRCUIT_KEY) ?: 0
        return failures < MAX_STRIPE_FAILURES
    }
    
    private void recordStripeFailure() {
        Integer failures = stripeFailureCache.getIfPresent(STRIPE_CIRCUIT_KEY) ?: 0
        stripeFailureCache.put(STRIPE_CIRCUIT_KEY, failures + 1)
        
        if (failures + 1 >= MAX_STRIPE_FAILURES) {
            log.error "Stripe API circuit breaker opened after ${MAX_STRIPE_FAILURES} failures"
        }
    }
    
    private void resetStripeFailures() {
        stripeFailureCache.invalidate(STRIPE_CIRCUIT_KEY)
    }

    def createCheckoutSession(KpUser user, String planType, String successUrl, String cancelUrl) {
        // Check circuit breaker
        if (!isStripeAvailable()) {
            log.error "Stripe API unavailable - circuit breaker open"
            return [success: false, error: "Payment service temporarily unavailable. Please try again later."]
        }
        
        try {
            init()
            def planDetails = subscriptionService.getPlanDetails(planType)
            
            def params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setCustomerEmail(user.email)
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(planDetails.name)
                                        .build()
                                )
                                .setUnitAmount((planDetails.price * 100).longValue())
                                .setRecurring(
                                    SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                        .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                                        .build()
                                )
                                .build()
                        )
                        .setQuantity(1L)
                        .build()
                )
                .putMetadata("userId", user.id.toString())
                .putMetadata("planType", planType)
                .build()
            
            def session = Session.create(params)
            
            userService.logActivity(user, 'CHECKOUT_SESSION_CREATED', 'SYSTEM')
            
            resetStripeFailures() // Reset on success
            return [
                success: true,
                sessionId: session.id,
                url: session.url
            ]
        } catch (CardException e) {
            // Card was declined
            log.warn "Card declined: ${e.message}"
            return [success: false, error: "Your card was declined. Please check your card details and try again."]
        } catch (RateLimitException e) {
            // Too many requests made to the API too quickly
            recordStripeFailure()
            log.error "Stripe rate limit exceeded: ${e.message}"
            return [success: false, error: "Service temporarily unavailable. Please try again in a few minutes."]
        } catch (InvalidRequestException e) {
            // Invalid parameters were supplied to Stripe's API
            log.error "Invalid Stripe request: ${e.message}"
            return [success: false, error: "Invalid payment request. Please contact support."]
        } catch (AuthenticationException e) {
            // Authentication with Stripe's API failed
            recordStripeFailure()
            log.error "Stripe authentication failed: ${e.message}"
            return [success: false, error: "Payment configuration error. Please contact support."]
        } catch (ApiConnectionException e) {
            // Network communication with Stripe failed
            recordStripeFailure()
            log.error "Stripe API connection failed: ${e.message}"
            return [success: false, error: "Connection error. Please check your internet connection and try again."]
        } catch (StripeException e) {
            // Generic Stripe error
            recordStripeFailure()
            log.error "Stripe error: ${e.message}", e
            return [success: false, error: "Payment processing error. Please try again later."]
        } catch (Exception e) {
            // Non-Stripe error
            log.error "Unexpected error creating checkout session: ${e.message}", e
            return [success: false, error: "An unexpected error occurred. Please try again later."]
        }
    }

    def createOneTimePayment(KpUser user, BigDecimal amount, String description) {
        init()
        
        try {
            def params = PaymentIntentCreateParams.builder()
                .setAmount((amount * 100).longValue())
                .setCurrency("eur")
                .setDescription(description)
                .putMetadata("userId", user.id.toString())
                .build()
            
            def paymentIntent = PaymentIntent.create(params)
            
            def payment = new Payment(
                user: user,
                transactionId: UUID.randomUUID().toString(),
                stripePaymentIntentId: paymentIntent.id,
                amount: amount,
                currency: 'EUR',
                status: 'PENDING',
                description: description,
                paymentDate: new Date()
            )
            
            payment.save(flush: true)
            
            return [
                success: true,
                clientSecret: paymentIntent.clientSecret,
                paymentId: payment.id
            ]
        } catch (Exception e) {
            log.error "Failed to create payment intent: ${e.message}", e
            return [
                success: false,
                error: e.message
            ]
        }
    }

    def confirmPayment(String paymentIntentId, KpUser user) {
        init()
        
        try {
            def paymentIntent = PaymentIntent.retrieve(paymentIntentId)
            
            def payment = Payment.findByStripePaymentIntentId(paymentIntentId)
            if (!payment) {
                payment = new Payment(
                    user: user,
                    transactionId: UUID.randomUUID().toString(),
                    stripePaymentIntentId: paymentIntentId,
                    amount: paymentIntent.amount / 100,
                    currency: paymentIntent.currency.toUpperCase()
                )
            }
            
            payment.status = paymentIntent.status == 'succeeded' ? 'SUCCESS' : 'FAILED'
            payment.paymentDate = new Date()
            
            if (paymentIntent.charges && paymentIntent.charges.data.size() > 0) {
                def charge = paymentIntent.charges.data[0]
                payment.stripeChargeId = charge.id
                payment.receiptUrl = charge.receiptUrl
                
                if (charge.paymentMethodDetails?.card) {
                    payment.cardLast4 = charge.paymentMethodDetails.card.last4
                    payment.cardBrand = charge.paymentMethodDetails.card.brand
                }
            }
            
            payment.save(flush: true)
            
            if (payment.status == 'SUCCESS') {
                userService.logActivity(user, 'PAYMENT_COMPLETED', 'SYSTEM')
                emailService.sendPaymentSuccessNotification(user, payment)
            } else {
                userService.logActivity(user, 'PAYMENT_FAILED', 'SYSTEM')
                emailService.sendPaymentFailedNotification(user, payment)
            }
            
            return payment
        } catch (Exception e) {
            log.error "Failed to confirm payment: ${e.message}", e
            return null
        }
    }

    def createCustomer(KpUser user) {
        init()
        
        try {
            def params = CustomerCreateParams.builder()
                .setEmail(user.email)
                .setName("${user.firstName} ${user.lastName}")
                .putMetadata("userId", user.id.toString())
                .build()
            
            def customer = Customer.create(params)
            
            return customer.id
        } catch (Exception e) {
            log.error "Failed to create Stripe customer: ${e.message}", e
            return null
        }
    }

    def updatePaymentMethod(KpUser user, String paymentMethodId) {
        init()
        
        try {
            def subscription = Subscription.findByUser(user)
            if (!subscription || !subscription.stripeCustomerId) {
                return [success: false, error: "No active subscription found"]
            }
            
            def customer = Customer.retrieve(subscription.stripeCustomerId)
            
            def params = CustomerUpdateParams.builder()
                .setInvoiceSettings(
                    CustomerUpdateParams.InvoiceSettings.builder()
                        .setDefaultPaymentMethod(paymentMethodId)
                        .build()
                )
                .build()
            
            customer.update(params)
            
            userService.logActivity(user, 'PAYMENT_METHOD_UPDATED', 'SYSTEM')
            
            return [success: true]
        } catch (Exception e) {
            log.error "Failed to update payment method: ${e.message}", e
            return [success: false, error: e.message]
        }
    }

    def cancelStripeSubscription(String subscriptionId) {
        init()
        
        try {
            def stripeSubscription = com.stripe.model.Subscription.retrieve(subscriptionId)
            stripeSubscription.cancel()
            
            return true
        } catch (Exception e) {
            log.error "Failed to cancel Stripe subscription: ${e.message}", e
            return false
        }
    }

    def getPaymentHistory(KpUser user, int limit = 10) {
        return Payment.findAllByUser(user, [max: limit, sort: 'dateCreated', order: 'desc'])
    }

    def getPaymentStatistics(KpUser user) {
        def payments = Payment.findAllByUser(user)
        
        return [
            totalPayments: payments.size(),
            successfulPayments: payments.count { it.status == 'SUCCESS' },
            totalSpent: payments.findAll { it.status == 'SUCCESS' }.sum { it.amount } ?: 0,
            averagePayment: payments.size() > 0 ? 
                (payments.findAll { it.status == 'SUCCESS' }.sum { it.amount } ?: 0) / 
                payments.count { it.status == 'SUCCESS' } : 0,
            lastPaymentDate: payments.max { it.dateCreated }?.dateCreated
        ]
    }

    def processRefund(Payment payment, BigDecimal amount = null) {
        init()
        
        if (payment.status != 'SUCCESS') {
            return [success: false, error: "Can only refund successful payments"]
        }
        
        try {
            def refundAmount = amount ?: payment.amount
            
            def params = RefundCreateParams.builder()
                .setCharge(payment.stripeChargeId)
                .setAmount((refundAmount * 100).longValue())
                .build()
            
            def refund = Refund.create(params)
            
            payment.status = 'REFUNDED'
            payment.save(flush: true)
            
            userService.logActivity(payment.user, 'PAYMENT_REFUNDED', 'SYSTEM')
            
            // Send refund notification
            def mailService = grails.util.Holders.applicationContext.getBean("mailService")
            mailService.sendMail {
                to payment.user.email
                from 'noreply@koolpump.com'
                subject 'KoolPump - Payment Refunded'
                html """
                    <p>Hello ${payment.user.firstName},</p>
                    <p>Your payment of €${refundAmount} has been refunded.</p>
                    <p>The refund should appear in your account within 5-10 business days.</p>
                    <p>Reference: ${payment.transactionId}</p>
                    <p>Best regards,<br>KoolPump Team</p>
                """
            }
            
            return [success: true, refundId: refund.id]
        } catch (Exception e) {
            log.error "Failed to process refund: ${e.message}", e
            return [success: false, error: e.message]
        }
    }

    def createStripePrice(String productName, BigDecimal amount, String currency = 'EUR') {
        init()
        
        try {
            def productParams = ProductCreateParams.builder()
                .setName(productName)
                .build()
            
            def product = Product.create(productParams)
            
            def priceParams = PriceCreateParams.builder()
                .setProduct(product.id)
                .setUnitAmount((amount * 100).longValue())
                .setCurrency(currency.toLowerCase())
                .setRecurring(
                    PriceCreateParams.Recurring.builder()
                        .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
                        .build()
                )
                .build()
            
            def price = Price.create(priceParams)
            
            return price.id
        } catch (Exception e) {
            log.error "Failed to create Stripe price: ${e.message}", e
            return null
        }
    }
}