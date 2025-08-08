package koolpump.user

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import java.nio.ByteBuffer
import koolpump.user.KpUser

@Transactional
@Slf4j
class TwoFactorService {

    def grailsApplication
    UserService userService

    private static final String TOTP_ALGORITHM = "HmacSHA1"
    private static final int CODE_DIGITS = 6
    private static final int TIME_STEP = 30 // seconds
    private static final int BACKUP_CODES_COUNT = 10

    def generateSecret() {
        try {
            SecureRandom random = new SecureRandom()
            byte[] bytes = new byte[20]
            random.nextBytes(bytes)
            return encodeBase32(bytes)
        } catch (Exception e) {
            log.error "Failed to generate 2FA secret: ${e.message}", e
            throw new RuntimeException("Failed to generate 2FA secret", e)
        }
    }

    def generateBackupCodes() {
        try {
            def codes = []
            SecureRandom random = new SecureRandom()
            
            BACKUP_CODES_COUNT.times {
                def code = String.format("%08d", random.nextInt(100000000))
                codes << code
            }
            
            return codes
        } catch (Exception e) {
            log.error "Failed to generate backup codes: ${e.message}", e
            throw new RuntimeException("Failed to generate backup codes", e)
        }
    }

    def enableTwoFactor(KpUser user) {
        try {
            def secret = generateSecret()
            def backupCodes = generateBackupCodes()
            
            user.twoFactorSecret = secret
            user.twoFactorBackupCodes = backupCodes.join(',')
            user.twoFactorEnabled = false // Will be enabled after verification
            
            if (user.save(flush: true)) {
                userService.logActivity(user, 'TWO_FACTOR_SETUP_INITIATED', 'SYSTEM')
                
                log.info "2FA setup initiated for user ${user.email}"
                
                return [
                    success: true,
                    secret: secret,
                    backupCodes: backupCodes,
                    qrCodeUrl: generateQRCodeUrl(user, secret)
                ]
            }
            
            log.warn "Failed to save 2FA settings for user ${user.email}"
            return [success: false, error: "Failed to save 2FA settings"]
        } catch (Exception e) {
            log.error "Failed to enable 2FA for user ${user.email}: ${e.message}", e
            return [success: false, error: "Failed to enable 2FA: ${e.message}"]
        }
    }

    def verifyAndEnableTwoFactor(KpUser user, String code) {
        try {
            if (!user.twoFactorSecret) {
                return [success: false, error: "2FA not initialized"]
            }
            
            if (verifyCode(user.twoFactorSecret, code)) {
                user.twoFactorEnabled = true
                if (user.save(flush: true)) {
                    userService.logActivity(user, 'TWO_FACTOR_ENABLED', 'SYSTEM')
                    log.info "2FA enabled successfully for user ${user.email}"
                    return [success: true]
                } else {
                    log.warn "Failed to save 2FA enabled status for user ${user.email}"
                    return [success: false, error: "Failed to save 2FA settings"]
                }
            }
            
            return [success: false, error: "Invalid verification code"]
        } catch (Exception e) {
            log.error "Failed to verify and enable 2FA for user ${user.email}: ${e.message}", e
            return [success: false, error: "Failed to enable 2FA: ${e.message}"]
        }
    }

    def disableTwoFactor(KpUser user, String password) {
        try {
            if (!userService.validatePassword(password, user.password)) {
                return [success: false, error: "Invalid password"]
            }
            
            user.twoFactorEnabled = false
            user.twoFactorSecret = null
            user.twoFactorBackupCodes = null
            
            if (user.save(flush: true)) {
                userService.logActivity(user, 'TWO_FACTOR_DISABLED', 'SYSTEM')
                log.info "2FA disabled for user ${user.email}"
                return [success: true]
            }
            
            log.warn "Failed to save 2FA disabled status for user ${user.email}"
            return [success: false, error: "Failed to disable 2FA"]
        } catch (Exception e) {
            log.error "Failed to disable 2FA for user ${user.email}: ${e.message}", e
            return [success: false, error: "Failed to disable 2FA: ${e.message}"]
        }
    }

    def verifyCode(String secret, String code) {
        try {
            if (!secret || !code) {
                return false
            }
            
            def currentCode = generateCurrentCode(secret)
            def previousCode = generateCodeForTime(secret, getCurrentTimeStep() - 1)
            def nextCode = generateCodeForTime(secret, getCurrentTimeStep() + 1)
            
            // Allow for time drift by checking previous and next codes
            return code == currentCode || code == previousCode || code == nextCode
        } catch (Exception e) {
            log.error "Failed to verify 2FA code: ${e.message}", e
            return false
        }
    }

    def verifyBackupCode(KpUser user, String code) {
        try {
            if (!user.twoFactorBackupCodes) {
                return false
            }
            
            def backupCodes = user.twoFactorBackupCodes.split(',').toList()
            
            if (backupCodes.contains(code)) {
                // Remove used backup code
                backupCodes.remove(code)
                user.twoFactorBackupCodes = backupCodes.join(',')
                
                if (user.save(flush: true)) {
                    userService.logActivity(user, 'TWO_FACTOR_BACKUP_CODE_USED', 'SYSTEM')
                    log.info "Backup code used successfully for user ${user.email}"
                    return true
                } else {
                    log.warn "Failed to save backup code removal for user ${user.email}"
                }
            }
            
            return false
        } catch (Exception e) {
            log.error "Failed to verify backup code for user ${user.email}: ${e.message}", e
            return false
        }
    }

    def regenerateBackupCodes(KpUser user) {
        try {
            def newCodes = generateBackupCodes()
            
            user.twoFactorBackupCodes = newCodes.join(',')
            
            if (user.save(flush: true)) {
                userService.logActivity(user, 'TWO_FACTOR_BACKUP_CODES_REGENERATED', 'SYSTEM')
                log.info "Backup codes regenerated for user ${user.email}"
                return [success: true, backupCodes: newCodes]
            }
            
            log.warn "Failed to save regenerated backup codes for user ${user.email}"
            return [success: false, error: "Failed to regenerate backup codes"]
        } catch (Exception e) {
            log.error "Failed to regenerate backup codes for user ${user.email}: ${e.message}", e
            return [success: false, error: "Failed to regenerate backup codes: ${e.message}"]
        }
    }

    private String generateCurrentCode(String secret) {
        return generateCodeForTime(secret, getCurrentTimeStep())
    }

    private String generateCodeForTime(String secret, long timeStep) {
        try {
            byte[] key = decodeBase32(secret)
            byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array()
            
            Mac mac = Mac.getInstance(TOTP_ALGORITHM)
            mac.init(new SecretKeySpec(key, TOTP_ALGORITHM))
            byte[] hash = mac.doFinal(data)
            
            int offset = hash[hash.length - 1] & 0xf
            int binary = ((hash[offset] & 0x7f) << 24) |
                        ((hash[offset + 1] & 0xff) << 16) |
                        ((hash[offset + 2] & 0xff) << 8) |
                        (hash[offset + 3] & 0xff)
            
            int otp = binary % (int) Math.pow(10, CODE_DIGITS)
            
            return String.format("%0${CODE_DIGITS}d", otp)
        } catch (Exception e) {
            log.error "Failed to generate code for time step ${timeStep}: ${e.message}", e
            throw new RuntimeException("Failed to generate TOTP code", e)
        }
    }

    private long getCurrentTimeStep() {
        return System.currentTimeMillis() / 1000L / TIME_STEP
    }

    private String generateQRCodeUrl(KpUser user, String secret) {
        try {
            def issuer = grailsApplication.config.twoFactor.issuer ?: "KoolPump"
            def accountName = user.email
            
            def otpAuthUrl = "otpauth://totp/${issuer}:${accountName}?secret=${secret}&issuer=${issuer}"
            
            // Google Chart API for QR Code generation
            def qrCodeWidth = grailsApplication.config.twoFactor.qrCodeWidth ?: 200
            def qrCodeHeight = grailsApplication.config.twoFactor.qrCodeHeight ?: 200
            
            return "https://chart.googleapis.com/chart?chs=${qrCodeWidth}x${qrCodeHeight}&chld=M|0&cht=qr&chl=" + 
                   URLEncoder.encode(otpAuthUrl, "UTF-8")
        } catch (Exception e) {
            log.error "Failed to generate QR code URL for user ${user.email}: ${e.message}", e
            throw new RuntimeException("Failed to generate QR code URL", e)
        }
    }

    private String encodeBase32(byte[] bytes) {
        def base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        def encoded = new StringBuilder()
        
        int i = 0
        int index = 0
        int digit = 0
        int currByte
        int nextByte
        
        while (i < bytes.length) {
            currByte = bytes[i] >= 0 ? bytes[i] : bytes[i] + 256
            
            if (index > 3) {
                if (i + 1 < bytes.length) {
                    nextByte = bytes[i + 1] >= 0 ? bytes[i + 1] : bytes[i + 1] + 256
                } else {
                    nextByte = 0
                }
                
                digit = currByte & (0xFF >> index)
                index = (index + 5) % 8
                digit <<= index
                digit |= nextByte >> (8 - index)
                i++
            } else {
                digit = (currByte >> (8 - (index + 5))) & 0x1F
                index = (index + 5) % 8
                if (index == 0) {
                    i++
                }
            }
            
            encoded.append(base32Chars.charAt(digit))
        }
        
        return encoded.toString()
    }

    private byte[] decodeBase32(String base32) {
        try {
            def base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
            base32 = base32.toUpperCase().replaceAll("[=]", "")
            
            def bytes = []
            int buffer = 0
            int bitsLeft = 0
            
            for (char c : base32.toCharArray()) {
                int val = base32Chars.indexOf(c)
                if (val < 0) {
                    throw new IllegalArgumentException("Invalid base32 character: ${c}")
                }
                
                buffer <<= 5
                buffer |= val
                bitsLeft += 5
                
                if (bitsLeft >= 8) {
                    bytes << ((buffer >> (bitsLeft - 8)) & 0xFF) as byte
                    bitsLeft -= 8
                }
            }
            
            return bytes as byte[]
        } catch (Exception e) {
            log.error "Failed to decode base32 string: ${e.message}", e
            throw new IllegalArgumentException("Invalid base32 encoding", e)
        }
    }

    def requiresTwoFactor(KpUser user) {
        return user.twoFactorEnabled && user.twoFactorSecret
    }

    def validateTwoFactorLogin(KpUser user, String code) {
        try {
            if (!requiresTwoFactor(user)) {
                return true
            }
            
            // First try regular TOTP code
            if (verifyCode(user.twoFactorSecret, code)) {
                userService.logActivity(user, 'TWO_FACTOR_LOGIN_SUCCESS', 'SYSTEM')
                log.info "2FA login successful for user ${user.email}"
                return true
            }
            
            // Then try backup code
            if (verifyBackupCode(user, code)) {
                log.info "2FA login successful using backup code for user ${user.email}"
                return true
            }
            
            userService.logActivity(user, 'TWO_FACTOR_LOGIN_FAILED', 'SYSTEM')
            log.warn "2FA login failed for user ${user.email}"
            return false
        } catch (Exception e) {
            log.error "Error during 2FA login validation for user ${user.email}: ${e.message}", e
            userService.logActivity(user, 'TWO_FACTOR_LOGIN_FAILED', 'SYSTEM')
            return false
        }
    }
}