<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>Choose Your Plan - KoolPump</title>
    <style>
        .plans-container {
            max-width: 1200px;
            margin: 50px auto;
            padding: 30px;
        }
        .plans-header {
            text-align: center;
            margin-bottom: 50px;
        }
        .plans-header h1 {
            color: #667eea;
            font-size: 2.5rem;
        }
        .plans-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 30px;
            margin-bottom: 30px;
        }
        .plan-card {
            background: white;
            border-radius: 10px;
            padding: 30px;
            box-shadow: 0 5px 20px rgba(0,0,0,0.1);
            position: relative;
            transition: transform 0.3s;
        }
        .plan-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 10px 30px rgba(0,0,0,0.15);
        }
        .plan-card.recommended {
            border: 2px solid #667eea;
        }
        .recommended-badge {
            position: absolute;
            top: -15px;
            left: 50%;
            transform: translateX(-50%);
            background: #667eea;
            color: white;
            padding: 5px 20px;
            border-radius: 20px;
            font-size: 0.85rem;
            font-weight: bold;
        }
        .plan-name {
            font-size: 1.5rem;
            font-weight: bold;
            margin-bottom: 10px;
        }
        .plan-price {
            font-size: 2.5rem;
            color: #667eea;
            margin-bottom: 5px;
        }
        .plan-price .currency {
            font-size: 1.5rem;
        }
        .plan-price .period {
            font-size: 1rem;
            color: #6c757d;
        }
        .plan-features {
            list-style: none;
            padding: 0;
            margin: 30px 0;
        }
        .plan-features li {
            padding: 10px 0;
            border-bottom: 1px solid #f0f0f0;
        }
        .plan-features li:last-child {
            border-bottom: none;
        }
        .plan-features li:before {
            content: "✓";
            color: #28a745;
            font-weight: bold;
            margin-right: 10px;
        }
        .plan-button {
            width: 100%;
            padding: 15px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            border-radius: 5px;
            font-size: 1.1rem;
            cursor: pointer;
            transition: transform 0.2s;
        }
        .plan-button:hover {
            transform: translateY(-2px);
        }
        .plan-button.current {
            background: #6c757d;
            cursor: not-allowed;
        }
        .current-plan-notice {
            background: #d4edda;
            color: #155724;
            padding: 10px;
            border-radius: 5px;
            text-align: center;
            margin-bottom: 10px;
        }
        .faq-section {
            margin-top: 60px;
            padding: 40px;
            background: #f8f9fa;
            border-radius: 10px;
        }
        .faq-item {
            margin-bottom: 20px;
        }
        .faq-question {
            font-weight: bold;
            margin-bottom: 10px;
            color: #333;
        }
        .faq-answer {
            color: #6c757d;
            line-height: 1.6;
        }
    </style>
</head>
<body>

<div class="plans-container">
    <div class="plans-header">
        <h1>Choose Your Plan</h1>
        <p>Select the perfect plan for your needs. Upgrade or downgrade anytime.</p>
    </div>

    <div class="plans-grid">
        <g:each in="${plans}" var="plan">
            <div class="plan-card ${plan.id == 'PROFESSIONAL' ? 'recommended' : ''}">
                <g:if test="${plan.id == 'PROFESSIONAL'}">
                    <div class="recommended-badge">MOST POPULAR</div>
                </g:if>
                
                <div class="plan-name">${plan.name}</div>
                
                <div class="plan-price">
                    <g:if test="${plan.price == 'Custom'}">
                        Custom
                    </g:if>
                    <g:else>
                        <span class="currency">€</span>${plan.price}
                        <span class="period">/${plan.interval}</span>
                    </g:else>
                </div>
                
                <g:if test="${currentSubscription?.planType == plan.id && currentSubscription?.status == 'ACTIVE'}">
                    <div class="current-plan-notice">
                        Your Current Plan
                    </div>
                </g:if>
                
                <ul class="plan-features">
                    <g:each in="${plan.features}" var="feature">
                        <li>${feature}</li>
                    </g:each>
                </ul>
                
                <g:if test="${currentSubscription?.planType == plan.id && currentSubscription?.status == 'ACTIVE'}">
                    <button class="plan-button current" disabled>Current Plan</button>
                </g:if>
                <g:elseif test="${plan.id == 'ENTERPRISE'}">
                    <g:link controller="contact" action="index" params="[category: 'SALES']" class="plan-button">
                        Contact Sales
                    </g:link>
                </g:elseif>
                <g:else>
                    <g:form controller="payment" action="checkout" method="POST">
                        <kpSec:csrfToken/>
                        <input type="hidden" name="planType" value="${plan.id}"/>
                        <button type="submit" class="plan-button">
                            <g:if test="${currentSubscription}">
                                <g:if test="${['BASIC', 'PROFESSIONAL'].indexOf(currentSubscription.planType) < ['BASIC', 'PROFESSIONAL'].indexOf(plan.id)}">
                                    Upgrade Now
                                </g:if>
                                <g:else>
                                    Switch Plan
                                </g:else>
                            </g:if>
                            <g:else>
                                Start Free Trial
                            </g:else>
                        </button>
                    </g:form>
                </g:else>
            </div>
        </g:each>
    </div>

    <div class="faq-section">
        <h2>Frequently Asked Questions</h2>
        
        <div class="faq-item">
            <div class="faq-question">Can I change my plan later?</div>
            <div class="faq-answer">
                Yes! You can upgrade or downgrade your plan at any time. Changes take effect immediately, 
                and we'll prorate any charges or credits.
            </div>
        </div>
        
        <div class="faq-item">
            <div class="faq-question">What payment methods do you accept?</div>
            <div class="faq-answer">
                We accept all major credit cards (Visa, MasterCard, American Express) and SEPA direct debit 
                for European customers. All payments are processed securely through Stripe.
            </div>
        </div>
        
        <div class="faq-item">
            <div class="faq-question">Is there a free trial?</div>
            <div class="faq-answer">
                Yes! All new users get a 14-day free trial with full access to all features. 
                No credit card required to start your trial.
            </div>
        </div>
        
        <div class="faq-item">
            <div class="faq-question">Can I cancel anytime?</div>
            <div class="faq-answer">
                Absolutely. You can cancel your subscription at any time. You'll continue to have access 
                until the end of your current billing period.
            </div>
        </div>
        
        <div class="faq-item">
            <div class="faq-question">Do you offer refunds?</div>
            <div class="faq-answer">
                We offer a 30-day money-back guarantee. If you're not satisfied within the first 30 days, 
                contact us for a full refund.
            </div>
        </div>
    </div>
</div>

</body>
</html>