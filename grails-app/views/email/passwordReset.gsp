<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Password Reset Request</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            text-align: center;
            border-radius: 10px 10px 0 0;
        }
        .content {
            background: #f8f9fa;
            padding: 30px;
            border-radius: 0 0 10px 10px;
        }
        .button {
            display: inline-block;
            padding: 15px 30px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            text-decoration: none;
            border-radius: 5px;
            margin: 20px 0;
        }
        .warning {
            background: #fff3cd;
            border-left: 4px solid #ffc107;
            padding: 15px;
            margin: 20px 0;
        }
        .footer {
            text-align: center;
            margin-top: 30px;
            color: #6c757d;
            font-size: 14px;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>Password Reset Request</h1>
    </div>
    
    <div class="content">
        <h2>Hello ${user.firstName}!</h2>
        
        <p>We received a request to reset the password for your KoolPump account associated with this email address.</p>
        
        <p>To reset your password, click the button below:</p>
        
        <div style="text-align: center;">
            <a href="${resetUrl}" class="button">Reset Password</a>
        </div>
        
        <p>Or copy and paste this link in your browser:</p>
        <p style="word-break: break-all; background: #fff; padding: 10px; border-radius: 5px;">
            ${resetUrl}
        </p>
        
        <div class="warning">
            <strong>⚠️ Important Security Information:</strong>
            <ul style="margin: 10px 0;">
                <li>This link will expire in 12 hours</li>
                <li>If you didn't request this password reset, please ignore this email</li>
                <li>Your password won't be changed until you create a new one</li>
            </ul>
        </div>
        
        <h3>Security Tips:</h3>
        <ul>
            <li>Choose a strong, unique password</li>
            <li>Never share your password with anyone</li>
            <li>Enable two-factor authentication when available</li>
        </ul>
        
        <p>If you continue to have problems accessing your account, please contact our support team.</p>
    </div>
    
    <div class="footer">
        <p>© ${new Date().format('yyyy')} KoolPump - www.koolpump.com</p>
        <p>This is an automated security message, please do not reply to this email.</p>
    </div>
</body>
</html>