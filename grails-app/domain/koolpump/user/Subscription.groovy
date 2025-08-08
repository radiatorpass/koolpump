package koolpump.user

import koolpump.user.KpUser

class Subscription implements Serializable {

    private static final long serialVersionUID = 1

    String planName
    String planType // TRIAL, BASIC, PROFESSIONAL, ENTERPRISE
    String status // TRIAL, ACTIVE, CANCELLED, EXPIRED, SUSPENDED
    BigDecimal monthlyPrice
    Date startDate
    Date endDate
    Date trialEndDate
    Date nextBillingDate
    String stripeCustomerId
    String stripeSubscriptionId
    boolean autoRenew = true
    Date dateCreated
    Date lastUpdated
    
    static belongsTo = [user: KpUser]

    static constraints = {
        planName blank: false
        planType blank: false, inList: ['TRIAL', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE']
        status blank: false, inList: ['TRIAL', 'ACTIVE', 'CANCELLED', 'EXPIRED', 'SUSPENDED']
        monthlyPrice nullable: true, min: 0.0
        startDate nullable: false
        endDate nullable: true
        trialEndDate nullable: true
        nextBillingDate nullable: true
        stripeCustomerId nullable: true
        stripeSubscriptionId nullable: true
    }

    static mapping = {
        table 'user_subscription'
        user index: 'user_subscription_idx'
    }

    int getTrialDaysRemaining() {
        if (status != 'TRIAL' || !trialEndDate) {
            return 0
        }
        def days = (trialEndDate.time - new Date().time) / (1000 * 60 * 60 * 24)
        return Math.max(0, days.intValue())
    }

    boolean isActive() {
        status in ['TRIAL', 'ACTIVE']
    }
}