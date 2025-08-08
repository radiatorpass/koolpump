# KoolPump Authentication System Documentation

## Overview
Complete user authentication system with registration, login, password reset, OAuth, subscriptions, and payment processing.

## Environment Variables & Secrets

### Required Environment Variables
```bash
# Database Configuration
DB_USER=your_db_user
DB_PASSWORD=your_db_password

# Email Configuration (SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
MAIL_FROM=noreply@koolpump.com

# Stripe Configuration
STRIPE_PUBLIC_KEY=pk_test_... or pk_live_...
STRIPE_SECRET_KEY=sk_test_... or sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...

# OAuth Configuration
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
MICROSOFT_CLIENT_ID=your_microsoft_client_id
MICROSOFT_CLIENT_SECRET=your_microsoft_client_secret

# Application Settings
APP_BASE_URL=http://www.koolpump.com
JWT_SECRET=your_jwt_secret_key
SESSION_SECRET=your_session_secret_key
```

### Setting Up Secrets

#### 1. Stripe Setup
1. Create account at https://stripe.com
2. Get API keys from Dashboard > Developers > API keys
3. Set up webhook endpoint: Dashboard > Developers > Webhooks
   - Endpoint URL: `https://www.koolpump.com/stripe/webhook`
   - Events to listen: `payment_intent.succeeded`, `payment_intent.failed`, `customer.subscription.created`, `customer.subscription.updated`, `customer.subscription.deleted`

#### 2. Google OAuth Setup
1. Go to https://console.cloud.google.com
2. Create new project or select existing
3. Enable Google+ API
4. Create OAuth 2.0 credentials
5. Add redirect URI: `http://www.koolpump.com/auth/google/callback`

#### 3. Microsoft OAuth Setup
1. Go to https://portal.azure.com
2. Register new application
3. Add redirect URI: `http://www.koolpump.com/auth/microsoft/callback`
4. Create client secret

## Authentication Routes

### Public Routes

#### Authentication
- `GET /login` - Login page
- `POST /authenticate` - Process login
- `GET /logout` - Logout user

#### Registration
- `GET /register` - Registration page
- `POST /register/save` - Process registration
- `GET /verify?token={token}` - Verify email

#### Password Reset
- `GET /forgot-password` - Forgot password page
- `POST /password-reset/send` - Send reset email
- `GET /password-reset/reset?token={token}` - Reset password page
- `POST /password-reset/update` - Update password

#### OAuth
- `GET /auth/google` - Initiate Google OAuth
- `GET /auth/google/callback` - Google OAuth callback
- `GET /auth/microsoft` - Initiate Microsoft OAuth
- `GET /auth/microsoft/callback` - Microsoft OAuth callback

### Protected Routes (Login Required)

#### Dashboard
- `GET /dashboard` - User dashboard
- `GET /dashboard/profile` - User profile
- `POST /dashboard/profile/update` - Update profile
- `GET /dashboard/change-password` - Change password page
- `POST /dashboard/update-password` - Update password
- `POST /dashboard/revoke-tokens` - Revoke all tokens

#### Payment & Subscription
- `GET /payment/choose-plan` - Plan selection
- `POST /payment/checkout` - Process payment
- `GET /payment/success` - Payment success page
- `GET /payment/cancel` - Payment cancelled page
- `GET /payment/manage-billing` - Billing management
- `POST /payment/update-method` - Update payment method

### Admin Routes (Admin Only)

- `GET /admin` - Admin dashboard
- `GET /admin/users` - User management
- `GET /admin/payments` - Payment management
- `GET /admin/subscriptions` - Subscription management

### Webhook Routes

- `POST /stripe/webhook` - Stripe webhook endpoint

## User Roles & Permissions

### Regular User
- Access to dashboard
- Manage own profile
- Change password
- Manage subscription
- View payment history

### Admin User
- All regular user permissions
- Access admin dashboard
- Manage all users
- View all payments
- Manage all subscriptions

## Authentication Flow

### Registration Flow
1. User fills registration form with plan selection
2. Account created with `enabled=false` (email verification) or `enabled=true` (OAuth)
3. Verification email sent with token (24h expiry)
4. User clicks verification link
5. Account activated, trial subscription created
6. User redirected to login

### Login Flow
1. User enters email/password or uses OAuth
2. Credentials validated
3. Session created
4. Activity logged
5. Redirect to dashboard or original URL

### Password Reset Flow
1. User requests password reset
2. Token generated (12h expiry)
3. Reset email sent
4. User clicks reset link
5. New password set
6. User redirected to login

### OAuth Flow
1. User clicks OAuth provider button
2. Redirected to provider authorization
3. Provider redirects back with code
4. Code exchanged for user info
5. User created/updated in database
6. Session created
7. Redirect to dashboard

## Subscription Plans

### Trial Plan
- Duration: 14 days
- Price: Free
- Features: Full access
- Auto-expires without payment

### Basic Plan
- Price: €9.99/month
- Features: Heat pump database access
- Billing: Monthly via Stripe

### Professional Plan
- Price: €29.99/month
- Features: Full access + API + Premium support
- Billing: Monthly via Stripe

### Enterprise Plan
- Price: Custom
- Features: Custom solutions
- Billing: Custom agreement

## Payment Processing

### Stripe Integration
1. User selects plan
2. Redirected to Stripe Checkout
3. Payment processed
4. Webhook updates subscription
5. User redirected back
6. Confirmation email sent

### Webhook Events Handled
- `payment_intent.succeeded` - Payment successful
- `payment_intent.failed` - Payment failed
- `customer.subscription.created` - New subscription
- `customer.subscription.updated` - Subscription modified
- `customer.subscription.deleted` - Subscription cancelled

## Security Features

### Password Security
- Minimum 6 characters
- SHA-256 hashing
- Password expiry support
- Password reset tokens

### Token Management
- Secure random token generation
- Token expiry (24h registration, 12h reset)
- One-time use tokens
- Token revocation

### Session Security
- Session timeout
- Activity logging
- IP address tracking
- Concurrent session detection

### Account Protection
- Email verification required
- Account locking support
- Failed login tracking
- Admin approval option

## Activity Logging

All user activities are logged:
- LOGIN
- LOGOUT
- REGISTRATION
- EMAIL_VERIFIED
- PASSWORD_RESET_REQUESTED
- PASSWORD_RESET_COMPLETED
- PASSWORD_CHANGED
- PROFILE_UPDATED
- TOKENS_REVOKED
- SUBSCRIPTION_CREATED
- SUBSCRIPTION_UPDATED
- PAYMENT_COMPLETED

## Email Templates

### Registration Email
- Welcome message
- Verification link
- Feature highlights
- 24h expiry notice

### Password Reset Email
- Reset link
- Security tips
- 12h expiry notice
- Warning if not requested

### Subscription Emails
- Confirmation
- Trial expiry reminder
- Payment success/failure
- Renewal notices

## Testing

### Default Admin User
Created automatically on bootstrap:
- Email: admin@koolpump.com
- Password: admin123
- Role: Admin

### Test Stripe Cards
- Success: 4242 4242 4242 4242
- Decline: 4000 0000 0000 0002
- Authentication: 4000 0025 0000 3155

## Troubleshooting

### Common Issues

1. **Email not sending**
   - Check SMTP configuration
   - Verify credentials
   - Check firewall/ports

2. **OAuth not working**
   - Verify redirect URIs
   - Check client ID/secret
   - Ensure APIs enabled

3. **Stripe webhooks failing**
   - Verify webhook secret
   - Check endpoint URL
   - Review webhook logs

4. **Session issues**
   - Clear browser cookies
   - Check session timeout
   - Verify session secret

## API Endpoints (Future)

Reserved for RESTful API:
- `/api/auth/login`
- `/api/auth/refresh`
- `/api/auth/logout`
- `/api/user/profile`
- `/api/subscription/status`