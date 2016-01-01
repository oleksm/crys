package tech.oleks.crys.service.model.domain.quartz

import grails.persistence.support.PersistenceContextInterceptor
import grails.transaction.Transactional
import grails.util.Holders
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.exception.ExceptionUtils
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.JobListener
import org.quartz.Trigger
import org.quartz.TriggerListener
import tech.oleks.crys.model.domain.quartz.JobEvent

@Transactional
class QuartzJobService implements JobListener, TriggerListener {

    @Override
    void jobToBeExecuted(JobExecutionContext jobExecutionContext) {

    }

    @Override
    void jobExecutionVetoed(JobExecutionContext jobExecutionContext) {

    }

    @Override
    void jobWasExecuted(JobExecutionContext jobExecutionContext, JobExecutionException e) {
        def ev = new JobEvent(
                eventId: jobExecutionContext.fireInstanceId,
                jobName: jobExecutionContext.trigger.jobKey.name,
                startDate: jobExecutionContext.fireTime,
                duration: jobExecutionContext.jobRunTime,
        )
        if (e) {
            def msg = ExceptionUtils.getMessage(e)
            ev.errorMessage = StringUtils.length(msg) > 2000 ? StringUtils.substring(msg, 0, 2000 - 1) : msg
            ev.stackTrace = ExceptionUtils.getStackTrace(e)
        }
        ev.save(failOnError: true)
        log.debug "Finished Job ${jobExecutionContext.trigger.jobKey.name} in ${jobExecutionContext.jobRunTime} ms"
    }

    @Override
    String getName() {
        return "Quartz Job Listener"
    }

    @Override
    void triggerFired(Trigger trigger, JobExecutionContext jobExecutionContext) {
        log.debug "Started Job ${trigger.jobKey.name}"
    }

    @Override
    boolean vetoJobExecution(Trigger trigger, JobExecutionContext jobExecutionContext) {
        return false
    }

    @Override
    void triggerMisfired(Trigger trigger) {

    }

    @Override
    void triggerComplete(Trigger trigger, JobExecutionContext jobExecutionContext, Trigger.CompletedExecutionInstruction completedExecutionInstruction) {
//        new JobEvent(
//                eventId: jobExecutionContext.fireInstanceId,
//                jobName: bindTo.jobKey.name,
//                startDate: bindTo.finalFireTime,
//                duration: jobExecutionContext.jobRunTime
//        ).save(failOnError: true)
//        log.debug "Finished Job ${bindTo.jobKey.name} in ${jobExecutionContext.jobRunTime} ms"
    }
}
