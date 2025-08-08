package koolpump.user

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import grails.compiler.GrailsCompileStatic
import koolpump.user.KpUser

@GrailsCompileStatic
@EqualsAndHashCode(includes='token')
@ToString(includes=['token', 'tokenType'], includeNames=true, includePackage=false)
class UserToken implements Serializable {

    private static final long serialVersionUID = 1

    String token
    String tokenType // RESET_PASSWORD, EMAIL_VERIFICATION
    Date expiryDate
    boolean used = false
    Date dateCreated
    Date lastUpdated

    static belongsTo = [user: KpUser]

    static constraints = {
        token blank: false, unique: true
        tokenType blank: false, inList: ['RESET_PASSWORD', 'EMAIL_VERIFICATION']
        expiryDate nullable: false
    }

    static mapping = {
        token index: 'token_idx'
    }

    boolean isExpired() {
        expiryDate < new Date()
    }

    boolean isValid() {
        !used && !isExpired()
    }
}