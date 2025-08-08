package koolpump.user

import grails.gorm.transactions.Transactional
import groovy.sql.Sql
import javax.sql.DataSource

@Transactional
class DatabaseMigrationService {

    DataSource dataSource

    def applyPerformanceIndexes() {
        def sql = new Sql(dataSource)
        def results = []
        
        try {
            // User table indexes
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_user_email ON app_user(email)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_user_enabled ON app_user(enabled)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_user_date_created ON app_user(date_created)")
            
            // UserToken table indexes
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_token_token ON user_token(token)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_token_user ON user_token(user_id)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_token_expiry ON user_token(expiry_date)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_token_type ON user_token(token_type)")
            
            // Payment table indexes
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_payment_user ON user_payment(user_id)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_payment_status ON user_payment(status)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_payment_date ON user_payment(payment_date)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_payment_stripe_intent ON user_payment(stripe_payment_intent_id)")
            
            // Subscription table indexes
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_subscription_user ON user_subscription(user_id)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_subscription_status ON user_subscription(status)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_subscription_stripe_customer ON user_subscription(stripe_customer_id)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_subscription_next_billing ON user_subscription(next_billing_date)")
            
            // UserActivityLog table indexes
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_activity_user ON user_activity_log(user_id)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_activity_timestamp ON user_activity_log(timestamp)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_activity_activity ON user_activity_log(activity)")
            
            // ContactMessage table indexes
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_contact_user ON contact_messages(user_id)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_contact_status ON contact_messages(status)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_contact_date ON contact_messages(date_created)")
            
            // Newsletter table indexes
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_newsletter_status ON newsletters(status)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_newsletter_scheduled ON newsletters(scheduled_date)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_newsletter_created_by ON newsletters(created_by_id)")
            
            // Composite indexes for common queries
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_user_email_enabled ON app_user(email, enabled)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_token_user_type ON user_token(user_id, token_type)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_payment_user_status ON user_payment(user_id, status)")
            results << executeIndex(sql, "CREATE INDEX IF NOT EXISTS idx_subscription_user_status ON user_subscription(user_id, status)")
            
            log.info "Database indexes applied successfully: ${results.size()} indexes processed"
            return [success: true, message: "Applied ${results.size()} database indexes", details: results]
            
        } catch (Exception e) {
            log.error "Failed to apply database indexes: ${e.message}", e
            return [success: false, message: "Failed to apply indexes: ${e.message}"]
        } finally {
            sql?.close()
        }
    }
    
    private def executeIndex(Sql sql, String indexStatement) {
        try {
            sql.execute(indexStatement)
            def indexName = indexStatement.find(/idx_\w+/)
            log.debug "Created/verified index: ${indexName}"
            return [index: indexName, status: 'success']
        } catch (Exception e) {
            log.warn "Index creation failed or already exists: ${e.message}"
            return [index: indexStatement, status: 'skipped', reason: e.message]
        }
    }
    
    def checkExistingIndexes() {
        def sql = new Sql(dataSource)
        def indexes = []
        
        try {
            def query = """
                SELECT 
                    schemaname,
                    tablename,
                    indexname,
                    indexdef
                FROM pg_indexes
                WHERE schemaname = 'public'
                AND indexname LIKE 'idx_%'
                ORDER BY tablename, indexname
            """
            
            sql.eachRow(query) { row ->
                indexes << [
                    table: row.tablename,
                    index: row.indexname,
                    definition: row.indexdef
                ]
            }
            
            return indexes
        } catch (Exception e) {
            log.error "Failed to check existing indexes: ${e.message}", e
            return []
        } finally {
            sql?.close()
        }
    }
}