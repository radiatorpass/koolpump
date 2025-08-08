<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Welcome to KoolPump</title>
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
        <h1>Welcome to KoolPump!</h1>
    </div>
    
    <div class="content">
        <h2>Hello ${user.firstName}!</h2>
        
        <p>Thank you for registering with KoolPump - Your comprehensive European Heat Pump Database.</p>
        
        <p>To complete your registration and activate your account, please verify your email address by clicking the button below:</p>
        
        <div style="text-align: center;">
            <a href="${verificationUrl}" class="button">Verify Email Address</a>
        </div>
        
        <p>Or copy and paste this link in your browser:</p>
        <p style="word-break: break-all; background: #fff; padding: 10px; border-radius: 5px;">
            ${verificationUrl}
        </p>
        
        <p><strong>This verification link will expire in 24 hours.</strong></p>
        
        <h3>What's Next?</h3>
        <ul>
            <li>Explore our comprehensive heat pump database</li>
            <li>Access performance data and efficiency ratings</li>
            <li>Connect with certified installers in your area</li>
            <li>Calculate available subsidies for your region</li>
        </ul>
        
        <p>If you didn't create an account with KoolPump, please ignore this email.</p>
    </div>
    
    <div class="footer">
        <p>© ${new Date().format('yyyy')} KoolPump - www.koolpump.com</p>
        <p>This is an automated message, please do not reply to this email.</p>
    </div>
</body>
</html>