<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>Register - KoolPump</title>
    <style>
        .register-container {
            max-width: 450px;
            margin: 50px auto;
            padding: 30px;
            background: white;
            border-radius: 10px;
            box-shadow: 0 5px 20px rgba(0,0,0,0.1);
        }
        .form-group {
            margin-bottom: 20px;
        }
        .form-control {
            width: 100%;
            padding: 12px;
            border: 1px solid #ddd;
            border-radius: 5px;
            font-size: 1rem;
        }
        .btn-primary {
            width: 100%;
            padding: 12px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            border-radius: 5px;
            font-size: 1.1rem;
            cursor: pointer;
            transition: transform 0.2s;
        }
        .btn-primary:hover {
            transform: translateY(-2px);
        }
        .error-message {
            color: #dc3545;
            font-size: 0.875rem;
            margin-top: 5px;
        }
        .subscription-plans {
            margin: 20px 0;
            padding: 15px;
            background: #f8f9fa;
            border-radius: 5px;
        }
        .plan-option {
            display: flex;
            align-items: center;
            margin-bottom: 10px;
            padding: 10px;
            border: 2px solid transparent;
            border-radius: 5px;
            cursor: pointer;
            transition: all 0.2s;
        }
        .plan-option:hover {
            background: white;
            border-color: #667eea;
        }
        .plan-option input[type="radio"] {
            margin-right: 10px;
        }
        .plan-option.selected {
            background: white;
            border-color: #667eea;
        }
        .plan-details {
            flex: 1;
        }
        .plan-name {
            font-weight: bold;
            margin-bottom: 5px;
        }
        .plan-price {
            color: #667eea;
            font-size: 1.2rem;
        }
        .plan-features {
            font-size: 0.875rem;
            color: #6c757d;
        }
        .trial-badge {
            background: #28a745;
            color: white;
            padding: 2px 8px;
            border-radius: 3px;
            font-size: 0.75rem;
            margin-left: 10px;
        }
    </style>
</head>
<body>

<div class="register-container">
    <h2 class="text-center mb-4">Create Your Account</h2>
    
    <g:if test="${flash.error}">
        <div class="alert alert-danger">${flash.error}</div>
    </g:if>
    
    <g:form action="save" method="POST">
        <kpSec:csrfToken/>
        <div class="form-group">
            <label for="firstName">First Name</label>
            <input type="text" name="firstName" id="firstName" class="form-control" 
                   value="${user?.firstName}" required />
            <g:if test="${user?.errors?.hasFieldErrors('firstName')}">
                <div class="error-message">${user.errors.getFieldError('firstName').defaultMessage}</div>
            </g:if>
        </div>
        
        <div class="form-group">
            <label for="lastName">Last Name</label>
            <input type="text" name="lastName" id="lastName" class="form-control" 
                   value="${user?.lastName}" required />
            <g:if test="${user?.errors?.hasFieldErrors('lastName')}">
                <div class="error-message">${user.errors.getFieldError('lastName').defaultMessage}</div>
            </g:if>
        </div>
        
        <div class="form-group">
            <label for="email">Email Address</label>
            <input type="email" name="email" id="email" class="form-control" 
                   value="${user?.email}" required />
            <g:if test="${user?.errors?.hasFieldErrors('email')}">
                <div class="error-message">${user.errors.getFieldError('email').defaultMessage}</div>
            </g:if>
        </div>
        
        <div class="form-group">
            <label for="password">Password</label>
            <input type="password" name="password" id="password" class="form-control" 
                   required minlength="6" />
            <g:if test="${user?.errors?.hasFieldErrors('password')}">
                <div class="error-message">${user.errors.getFieldError('password').defaultMessage}</div>
            </g:if>
        </div>
        
        <div class="form-group">
            <label for="confirmPassword">Confirm Password</label>
            <input type="password" name="confirmPassword" id="confirmPassword" class="form-control" 
                   required minlength="6" />
        </div>

        <div class="subscription-plans">
            <h4>Choose Your Plan</h4>
            <div class="plan-option selected">
                <input type="radio" name="subscriptionPlan" value="trial" id="plan-trial" checked>
                <label for="plan-trial" class="plan-details">
                    <div class="plan-name">
                        Free Trial
                        <span class="trial-badge">14 DAYS FREE</span>
                    </div>
                    <div class="plan-features">Full access to all features for 14 days</div>
                </label>
            </div>
            
            <div class="plan-option">
                <input type="radio" name="subscriptionPlan" value="basic" id="plan-basic">
                <label for="plan-basic" class="plan-details">
                    <div class="plan-name">Basic Plan</div>
                    <div class="plan-price">€9.99/month</div>
                    <div class="plan-features">Access to heat pump database</div>
                </label>
            </div>
            
            <div class="plan-option">
                <input type="radio" name="subscriptionPlan" value="professional" id="plan-professional">
                <label for="plan-professional" class="plan-details">
                    <div class="plan-name">Professional Plan</div>
                    <div class="plan-price">€29.99/month</div>
                    <div class="plan-features">Full access + API + Premium support</div>
                </label>
            </div>
            
            <div class="plan-option">
                <input type="radio" name="subscriptionPlan" value="enterprise" id="plan-enterprise">
                <label for="plan-enterprise" class="plan-details">
                    <div class="plan-name">Enterprise Plan</div>
                    <div class="plan-price">Contact us</div>
                    <div class="plan-features">Custom solutions for businesses</div>
                </label>
            </div>
        </div>
        
        <div class="form-group">
            <label>
                <input type="checkbox" name="agreeTerms" required />
                I agree to the <a href="/terms" target="_blank">Terms of Service</a> 
                and <a href="/privacy" target="_blank">Privacy Policy</a>
            </label>
        </div>
        
        <div class="form-group">
            <button type="submit" class="btn-primary">Create Account</button>
        </div>
    </g:form>
    
    <div class="text-center mt-3">
        Already have an account? <g:link controller="auth" action="login">Sign In</g:link>
    </div>
</div>

<script>
    document.querySelectorAll('.plan-option').forEach(option => {
        option.addEventListener('click', function() {
            document.querySelectorAll('.plan-option').forEach(opt => opt.classList.remove('selected'));
            this.classList.add('selected');
            this.querySelector('input[type="radio"]').checked = true;
        });
    });
    
    document.getElementById('confirmPassword').addEventListener('input', function() {
        const password = document.getElementById('password').value;
        const confirmPassword = this.value;
        
        if (password !== confirmPassword) {
            this.setCustomValidity('Passwords do not match');
        } else {
            this.setCustomValidity('');
        }
    });
</script>

</body>
</html>