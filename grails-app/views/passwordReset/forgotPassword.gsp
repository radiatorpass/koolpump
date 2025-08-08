<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>Forgot Password - KoolPump</title>
    <style>
        .forgot-container {
            max-width: 400px;
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
        .info-text {
            color: #6c757d;
            margin-bottom: 20px;
            line-height: 1.6;
        }
    </style>
</head>
<body>

<div class="forgot-container">
    <h2 class="text-center mb-4">Reset Your Password</h2>
    
    <g:if test="${flash.error}">
        <div class="alert alert-danger">${flash.error}</div>
    </g:if>
    
    <g:if test="${flash.message}">
        <div class="alert alert-success">${flash.message}</div>
    </g:if>
    
    <p class="info-text">
        Enter your email address and we'll send you a link to reset your password.
    </p>
    
    <g:form action="sendResetLink" method="POST">
        <kpSec:csrfToken/>
        <div class="form-group">
            <label for="email">Email Address</label>
            <input type="email" name="email" id="email" class="form-control" 
                   placeholder="Enter your email" required autofocus />
        </div>
        
        <div class="form-group">
            <button type="submit" class="btn-primary">Send Reset Link</button>
        </div>
    </g:form>
    
    <div class="text-center mt-3">
        <g:link controller="auth" action="login">Back to Login</g:link>
    </div>
</div>

</body>
</html>