package koolpump.user

import koolpump.user.KpUser

class UserActivityLog implements Serializable {

    private static final long serialVersionUID = 1

    String activity
    String ipAddress
    Date timestamp
    String userAgent
    String sessionId
    
    static belongsTo = [user: KpUser]

    static constraints = {
        activity blank: false
        ipAddress nullable: true
        timestamp nullable: false
        userAgent nullable: true, maxSize: 500
        sessionId nullable: true
    }

    static mapping = {
        table 'user_activity_log'
        timestamp index: 'timestamp_idx'
        user index: 'user_activity_idx'
    }
}