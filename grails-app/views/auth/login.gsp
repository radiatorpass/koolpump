<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>Login - KoolPump</title>
    <style>
        .login-container {
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
        .oauth-button {
            width: 100%;
            padding: 12px;
            margin-bottom: 10px;
            border: 1px solid #ddd;
            border-radius: 5px;
            background: white;
            color: #333;
            font-size: 1rem;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: background 0.2s;
        }
        .oauth-button:hover {
            background: #f8f9fa;
        }
        .oauth-button img {
            width: 20px;
            height: 20px;
            margin-right: 10px;
        }
        .divider {
            text-align: center;
            margin: 20px 0;
            position: relative;
        }
        .divider:before {
            content: '';
            position: absolute;
            top: 50%;
            left: 0;
            right: 0;
            height: 1px;
            background: #ddd;
        }
        .divider span {
            background: white;
            padding: 0 10px;
            position: relative;
        }
    </style>
</head>
<body>

<div class="login-container">
    <h2 class="text-center mb-4">Sign In</h2>
    
    <g:if test="${flash.error}">
        <div class="alert alert-danger">${flash.error}</div>
    </g:if>
    
    <g:if test="${flash.message}">
        <div class="alert alert-success">${flash.message}</div>
    </g:if>

    <div class="oauth-buttons">
        <g:link controller="oauth" action="googleLogin" class="oauth-button">
            <img src="https://www.google.com/favicon.ico" alt="Google">
            Continue with Google
        </g:link>
        
        <g:link controller="oauth" action="microsoftLogin" class="oauth-button">
            <img src="https://www.microsoft.com/favicon.ico" alt="Microsoft">
            Continue with Microsoft
        </g:link>
    </div>

    <div class="divider">
        <span>OR</span>
    </div>
    
    <g:form action="authenticate" method="POST">
        <kpSec:csrfToken/>
        <div class="form-group">
            <label for="email">Email Address</label>
            <input type="email" name="email" id="email" class="form-control" required autofocus />
        </div>
        
        <div class="form-group">
            <label for="password">Password</label>
            <input type="password" name="password" id="password" class="form-control" required />
        </div>
        
        <div class="form-group">
            <button type="submit" class="btn-primary">Sign In</button>
        </div>
    </g:form>
    
    <div class="text-center mt-3">
        <g:link controller="passwordReset" action="forgotPassword">Forgot Password?</g:link>
    </div>
    
    <div class="text-center mt-2">
        Don't have an account? <g:link controller="registration" action="register">Sign Up</g:link>
    </div>
</div>

</body>
</html>