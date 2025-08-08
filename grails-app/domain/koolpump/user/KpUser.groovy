package koolpump.user

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import grails.compiler.GrailsCompileStatic

@GrailsCompileStatic
@EqualsAndHashCode(includes='email')
@ToString(includes='email', includeNames=true, includePackage=false)
class KpUser implements Serializable {

    private static final long serialVersionUID = 1

    String email
    String password
    String firstName
    String lastName
    boolean enabled = true
    boolean accountExpired = false
    boolean accountLocked = false
    boolean passwordExpired = false
    boolean isAdmin = false
    boolean subscribeNewsletter = false
    
    // Two-Factor Authentication
    boolean twoFactorEnabled = false
    String twoFactorSecret
    String twoFactorBackupCodes
    
    // OAuth fields
    String oauthProvider
    String oauthProviderId
    
    Date dateCreated
    Date lastUpdated

    static constraints = {
        email blank: false, unique: true, email: true
        password blank: false, minSize: 6
        firstName blank: false
        lastName blank: false
        twoFactorSecret nullable: true
        twoFactorBackupCodes nullable: true, maxSize: 1000
        oauthProvider nullable: true, inList: ['GOOGLE', 'MICROSOFT', null]
        oauthProviderId nullable: true
    }

    static mapping = {
        password column: '`password`'
        table 'app_user'
    }

    Set<Role> getAuthorities() {
        (UserRole.findAllByUser(this) as List<UserRole>)*.role as Set<Role>
    }
}