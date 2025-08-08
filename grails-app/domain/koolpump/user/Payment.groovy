package koolpump.user

import koolpump.user.KpUser

class Payment implements Serializable {

    private static final long serialVersionUID = 1

    String transactionId
    String stripePaymentIntentId
    String stripeChargeId
    BigDecimal amount
    String currency = 'EUR'
    String status // SUCCESS, PENDING, FAILED, REFUNDED
    String description
    String paymentMethod // CARD, BANK_TRANSFER, PAYPAL
    String cardLast4
    String cardBrand
    String invoiceUrl
    String receiptUrl
    Date paymentDate
    String failureReason
    Date dateCreated
    Date lastUpdated
    
    static belongsTo = [user: KpUser, subscription: Subscription]

    static constraints = {
        transactionId blank: false, unique: true
        stripePaymentIntentId nullable: true
        stripeChargeId nullable: true
        amount nullable: false, min: 0.0
        currency blank: false
        status blank: false, inList: ['SUCCESS', 'PENDING', 'FAILED', 'REFUNDED']
        description nullable: true
        paymentMethod nullable: true
        cardLast4 nullable: true
        cardBrand nullable: true
        invoiceUrl nullable: true
        receiptUrl nullable: true
        paymentDate nullable: true
        failureReason nullable: true
        subscription nullable: true
    }

    static mapping = {
        table 'user_payment'
        user index: 'user_payment_idx'
        transactionId index: 'transaction_id_idx'
        stripePaymentIntentId index: 'stripe_payment_intent_idx'
    }
}