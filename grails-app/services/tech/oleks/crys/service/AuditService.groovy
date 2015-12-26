package tech.oleks.crys.service

import grails.transaction.Transactional
import tech.oleks.crys.model.domain.Audit

@Transactional
class AuditService {

    def addAudit(Audit audit) {
        audit.created = new Date()
        audit.save(failOnError: true)
    }

    def syncEvent(Audit audit) {
        audit.eventType = Audit.EventType.Sync
        addAudit(audit)
    }
}
