package koolpump.user

import koolpump.user.KpUser

class Newsletter implements Serializable {

    private static final long serialVersionUID = 1

    String subject
    String content
    String contentHtml
    String status = 'DRAFT' // DRAFT, SCHEDULED, SENDING, SENT, FAILED
    Date scheduledDate
    Date sentDate
    int recipientCount = 0
    int successCount = 0
    int failureCount = 0
    String failureReason
    KpUser createdBy
    Date dateCreated
    Date lastUpdated

    static constraints = {
        subject blank: false, maxSize: 200
        content blank: false, maxSize: 10000
        contentHtml nullable: true, maxSize: 50000
        status blank: false, inList: ['DRAFT', 'SCHEDULED', 'SENDING', 'SENT', 'FAILED']
        scheduledDate nullable: true
        sentDate nullable: true
        failureReason nullable: true, maxSize: 1000
        createdBy nullable: false
    }

    static mapping = {
        content type: 'text'
        contentHtml type: 'text'
        table 'newsletters'
    }
}