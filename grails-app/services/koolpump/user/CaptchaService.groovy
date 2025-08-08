package koolpump.user

import grails.gorm.transactions.Transactional
import grails.converters.JSON

@Transactional
class CaptchaService {

    def grailsApplication

    def verifyCaptcha(String captchaResponse, String remoteIp = null) {
        if (!captchaResponse) {
            return [success: false, error: "CAPTCHA response is required"]
        }
        
        def secretKey = System.getenv('RECAPTCHA_SECRET_KEY') ?: grailsApplication.config.recaptcha.secretKey
        
        if (!secretKey || secretKey == '6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe') {
            // Test key - always validate in development
            log.warn "Using test reCAPTCHA key - all validations will pass"
            return [success: true, score: 1.0]
        }
        
        try {
            def url = new URL("https://www.google.com/recaptcha/api/siteverify")
            def connection = url.openConnection() as HttpURLConnection
            connection.setRequestMethod("POST")
            connection.setDoOutput(true)
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            
            def params = "secret=${secretKey}&response=${captchaResponse}"
            if (remoteIp) {
                params += "&remoteip=${remoteIp}"
            }
            
            connection.outputStream.withWriter { it.write(params) }
            
            def response = connection.inputStream.text
            def result = JSON.parse(response)
            
            if (result.success) {
                return [
                    success: true,
                    score: result.score ?: 1.0,
                    action: result.action,
                    challengeTimestamp: result.challenge_ts,
                    hostname: result.hostname
                ]
            } else {
                def errors = result['error-codes'] ?: []
                log.warn "CAPTCHA verification failed: ${errors.join(', ')}"
                
                return [
                    success: false,
                    error: mapErrorCode(errors[0]),
                    errorCodes: errors
                ]
            }
        } catch (Exception e) {
            log.error "CAPTCHA verification error: ${e.message}", e
            return [
                success: false,
                error: "CAPTCHA verification failed"
            ]
        }
    }

    def getSiteKey() {
        return System.getenv('RECAPTCHA_SITE_KEY') ?: grailsApplication.config.recaptcha.siteKey
    }

    def isEnabled() {
        def siteKey = getSiteKey()
        def secretKey = System.getenv('RECAPTCHA_SECRET_KEY') ?: grailsApplication.config.recaptcha.secretKey
        
        return siteKey && secretKey && 
               siteKey != '6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI' &&
               secretKey != '6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe'
    }

    def renderCaptchaScript() {
        if (!isEnabled()) {
            return "<!-- reCAPTCHA not configured -->"
        }
        
        def siteKey = getSiteKey()
        return """
            <script src="https://www.google.com/recaptcha/api.js?render=${siteKey}"></script>
            <script>
                function executeCaptcha(action, callback) {
                    grecaptcha.ready(function() {
                        grecaptcha.execute('${siteKey}', {action: action}).then(function(token) {
                            callback(token);
                        });
                    });
                }
            </script>
        """
    }

    def renderCaptchaV2() {
        if (!isEnabled()) {
            return "<!-- reCAPTCHA not configured -->"
        }
        
        def siteKey = getSiteKey()
        return """
            <div class="g-recaptcha" data-sitekey="${siteKey}"></div>
            <script src="https://www.google.com/recaptcha/api.js" async defer></script>
        """
    }

    def renderInvisibleCaptcha(String formId, String callbackFunction = "onCaptchaSubmit") {
        if (!isEnabled()) {
            return "<!-- reCAPTCHA not configured -->"
        }
        
        def siteKey = getSiteKey()
        return """
            <button class="g-recaptcha" 
                    data-sitekey="${siteKey}"
                    data-callback="${callbackFunction}"
                    data-size="invisible">
            </button>
            <script src="https://www.google.com/recaptcha/api.js" async defer></script>
            <script>
                function ${callbackFunction}(token) {
                    document.getElementById('${formId}').submit();
                }
            </script>
        """
    }

    private String mapErrorCode(String errorCode) {
        switch (errorCode) {
            case "missing-input-secret":
                return "The secret parameter is missing"
            case "invalid-input-secret":
                return "The secret parameter is invalid or malformed"
            case "missing-input-response":
                return "The response parameter is missing"
            case "invalid-input-response":
                return "The response parameter is invalid or malformed"
            case "bad-request":
                return "The request is invalid or malformed"
            case "timeout-or-duplicate":
                return "The response is no longer valid"
            default:
                return "CAPTCHA verification failed"
        }
    }

    def validateFormWithCaptcha(String captchaResponse, String remoteIp, double minimumScore = 0.5) {
        if (!isEnabled()) {
            // CAPTCHA not configured, allow form submission
            return [success: true, bypassed: true]
        }
        
        def result = verifyCaptcha(captchaResponse, remoteIp)
        
        if (!result.success) {
            return result
        }
        
        // For reCAPTCHA v3, check the score
        if (result.score != null && result.score < minimumScore) {
            return [
                success: false,
                error: "Suspicious activity detected",
                score: result.score
            ]
        }
        
        return result
    }
}