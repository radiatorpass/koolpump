<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>Reset Password - KoolPump</title>
    <style>
        .reset-container {
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
        .password-requirements {
            background: #f8f9fa;
            padding: 10px;
            border-radius: 5px;
            margin-bottom: 20px;
            font-size: 0.9rem;
        }
        .password-requirements ul {
            margin: 5px 0;
            padding-left: 20px;
        }
    </style>
</head>
<body>

<div class="reset-container">
    <h2 class="text-center mb-4">Create New Password</h2>
    
    <g:if test="${flash.error}">
        <div class="alert alert-danger">${flash.error}</div>
    </g:if>
    
    <div class="password-requirements">
        <strong>Password Requirements:</strong>
        <ul>
            <li>At least 6 characters long</li>
            <li>Must match confirmation password</li>
        </ul>
    </div>
    
    <g:form action="updatePassword" method="POST">
        <kpSec:csrfToken/>
        <input type="hidden" name="token" value="${token}" />
        
        <div class="form-group">
            <label for="newPassword">New Password</label>
            <input type="password" name="newPassword" id="newPassword" class="form-control" 
                   required minlength="6" autofocus />
        </div>
        
        <div class="form-group">
            <label for="confirmPassword">Confirm New Password</label>
            <input type="password" name="confirmPassword" id="confirmPassword" class="form-control" 
                   required minlength="6" />
        </div>
        
        <div class="form-group">
            <button type="submit" class="btn-primary">Reset Password</button>
        </div>
    </g:form>
    
    <div class="text-center mt-3">
        <g:link controller="auth" action="login">Back to Login</g:link>
    </div>
</div>

<script>
    document.getElementById('confirmPassword').addEventListener('input', function() {
        const password = document.getElementById('newPassword').value;
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