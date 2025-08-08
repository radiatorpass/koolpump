package koolpump.user

import koolpump.user.KpUser

class ContactMessage implements Serializable {

    private static final long serialVersionUID = 1

    String name
    String email
    String subject
    String message
    String category // GENERAL, SUPPORT, SALES, PARTNERSHIP, BUG_REPORT
    String status = 'NEW' // NEW, IN_PROGRESS, RESOLVED, CLOSED
    String ipAddress
    Date dateCreated
    Date lastUpdated
    KpUser user // Optional, if logged in
    String adminNotes
    KpUser assignedTo // Admin who handles this

    static constraints = {
        name blank: false
        email blank: false, email: true
        subject blank: false, maxSize: 200
        message blank: false, maxSize: 5000
        category blank: false, inList: ['GENERAL', 'SUPPORT', 'SALES', 'PARTNERSHIP', 'BUG_REPORT']
        status blank: false, inList: ['NEW', 'IN_PROGRESS', 'RESOLVED', 'CLOSED']
        ipAddress nullable: true
        user nullable: true
        adminNotes nullable: true, maxSize: 2000
        assignedTo nullable: true
    }

    static mapping = {
        message type: 'text'
        adminNotes type: 'text'
        table 'contact_messages'
    }
}