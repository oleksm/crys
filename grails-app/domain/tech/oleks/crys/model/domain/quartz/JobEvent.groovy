package tech.oleks.crys.model.domain.quartz

class JobEvent {
    String eventId
    String eventName
    String jobName
    Date startDate
    Long duration
    String errorMessage
    String stackTrace

    static mapping = {
        table "CRYS_JOB_EVENT"
        stackTrace type: 'text'
    }

    static constraints = {
        eventName (nullable: true)
        errorMessage (nullable: true, type: 'text')
        stackTrace (nullable: true, maxSize: 8000)
        duration (min: 0L)
        eventId (unique: true)
    }
}
