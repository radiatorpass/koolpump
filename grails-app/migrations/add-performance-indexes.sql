-- Performance indexes for KoolPump database
-- Run this migration to improve query performance

-- User table indexes
CREATE INDEX IF NOT EXISTS idx_user_email ON app_user(email);
CREATE INDEX IF NOT EXISTS idx_user_enabled ON app_user(enabled);
CREATE INDEX IF NOT EXISTS idx_user_date_created ON app_user(date_created);

-- UserToken table indexes
CREATE INDEX IF NOT EXISTS idx_token_token ON user_token(token);
CREATE INDEX IF NOT EXISTS idx_token_user ON user_token(user_id);
CREATE INDEX IF NOT EXISTS idx_token_expiry ON user_token(expiry_date);
CREATE INDEX IF NOT EXISTS idx_token_type ON user_token(token_type);

-- Payment table indexes
CREATE INDEX IF NOT EXISTS idx_payment_user ON user_payment(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_status ON user_payment(status);
CREATE INDEX IF NOT EXISTS idx_payment_date ON user_payment(payment_date);
CREATE INDEX IF NOT EXISTS idx_payment_stripe_intent ON user_payment(stripe_payment_intent_id);

-- Subscription table indexes
CREATE INDEX IF NOT EXISTS idx_subscription_user ON user_subscription(user_id);
CREATE INDEX IF NOT EXISTS idx_subscription_status ON user_subscription(status);
CREATE INDEX IF NOT EXISTS idx_subscription_stripe_customer ON user_subscription(stripe_customer_id);
CREATE INDEX IF NOT EXISTS idx_subscription_next_billing ON user_subscription(next_billing_date);

-- UserActivityLog table indexes
CREATE INDEX IF NOT EXISTS idx_activity_user ON user_activity_log(user_id);
CREATE INDEX IF NOT EXISTS idx_activity_timestamp ON user_activity_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_activity_activity ON user_activity_log(activity);

-- ContactMessage table indexes
CREATE INDEX IF NOT EXISTS idx_contact_user ON contact_messages(user_id);
CREATE INDEX IF NOT EXISTS idx_contact_status ON contact_messages(status);
CREATE INDEX IF NOT EXISTS idx_contact_date ON contact_messages(date_created);

-- Newsletter table indexes
CREATE INDEX IF NOT EXISTS idx_newsletter_status ON newsletters(status);
CREATE INDEX IF NOT EXISTS idx_newsletter_scheduled ON newsletters(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_newsletter_created_by ON newsletters(created_by_id);

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_user_email_enabled ON app_user(email, enabled);
CREATE INDEX IF NOT EXISTS idx_token_user_type ON user_token(user_id, token_type);
CREATE INDEX IF NOT EXISTS idx_payment_user_status ON user_payment(user_id, status);
CREATE INDEX IF NOT EXISTS idx_subscription_user_status ON user_subscription(user_id, status);